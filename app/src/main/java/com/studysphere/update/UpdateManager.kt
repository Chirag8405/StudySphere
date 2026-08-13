package com.studysphere.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

//  Data Models

data class GithubRelease(
    val tagName: String,           // e.g. "v1.3.0"
    val versionName: String,       // e.g. "1.3.0"
    val versionCode: Int,          // derived from tag via same formula as build.gradle
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkExpectedBytes: Long,    // size GitHub reports for the asset, -1 if unknown
    val htmlUrl: String
)

sealed class UpdateCheckResult {
    object UpToDate : UpdateCheckResult()
    data class UpdateAvailable(val release: GithubRelease) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progressPercent: Int) : DownloadState()
    data class Downloaded(val apkFile: File) : DownloadState()
    data class Failed(val reason: String) : DownloadState()
}

/** Internal result of the raw GitHub API fetch — carries a specific reason on failure
 *  instead of collapsing everything into null, so the caller can surface a real message. */
private sealed class FetchReleaseResult {
    data class Success(val release: GithubRelease) : FetchReleaseResult()
    data class Failure(val reason: String) : FetchReleaseResult()
}

//  UpdateManager

class UpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_OWNER = "Chirag8405"
        private const val GITHUB_REPO = "StudySphere"
        private const val RELEASES_API =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        // Matches any asset ending in ".apk" (case-insensitive). Filenames like
        // "StudySphere-v1.2.4.apk" change every release, so we don't hardcode a name.
        private val APK_NAME_REGEX = Regex(""".+\.apk$""", RegexOption.IGNORE_CASE)

        // How often to poll DownloadManager for progress/status.
        private const val POLL_INTERVAL_MS = 500L
    }

    //  Version check

    /**
     * Fetches the latest GitHub release and compares it against the installed versionCode.
     * Call from a ViewModel / coroutine scope (runs on IO dispatcher internally).
     */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        when (val result = fetchLatestRelease()) {
            is FetchReleaseResult.Failure -> UpdateCheckResult.Error(result.reason)
            is FetchReleaseResult.Success -> {
                val release = result.release
                val installedCode = try {
                    installedVersionCode()
                } catch (e: Exception) {
                    return@withContext UpdateCheckResult.Error(
                        "Couldn't read installed app version: ${e.localizedMessage ?: e.javaClass.simpleName}"
                    )
                }
                if (release.versionCode > installedCode) {
                    UpdateCheckResult.UpdateAvailable(release)
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        }
    }

    private fun installedVersionCode(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionCode
        }
    }

    private fun fetchLatestRelease(): FetchReleaseResult {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                // Avoid any intermediate cache serving a stale release payload.
                setRequestProperty("Cache-Control", "no-cache")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = conn.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return FetchReleaseResult.Failure(describeHttpError(conn, responseCode))
            }

            val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)

            val json = try {
                JSONObject(body)
            } catch (e: JSONException) {
                return FetchReleaseResult.Failure(
                    "GitHub returned a response that couldn't be parsed as JSON: ${e.localizedMessage}"
                )
            }

            val tag = json.optString("tag_name", "")
            if (tag.isEmpty()) {
                return FetchReleaseResult.Failure("GitHub response is missing a 'tag_name' field.")
            }

            val versionName = tag.trimStart('v')
            val versionCode = parseVersionCode(versionName)
            if (versionCode == null) {
                return FetchReleaseResult.Failure(
                    "Couldn't parse a version number out of tag '$tag'. Expected a format like 'v1.2.3'."
                )
            }

            val releaseNotes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "")

            val assetsJson = json.optJSONArray("assets")
            if (assetsJson == null || assetsJson.length() == 0) {
                return FetchReleaseResult.Failure(
                    "Release $tag has no files attached to it — nothing to download."
                )
            }

            val assetNames = mutableListOf<String>()
            val apkAssets = mutableListOf<Triple<String, String, Long>>()
            for (i in 0 until assetsJson.length()) {
                val asset = assetsJson.getJSONObject(i)
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                val size = asset.optLong("size", -1L)
                if (name.isEmpty() || url.isEmpty()) continue
                assetNames.add(name)
                if (APK_NAME_REGEX.matches(name)) {
                    apkAssets.add(Triple(name, url, size))
                }
            }

            if (apkAssets.isEmpty()) {
                return FetchReleaseResult.Failure(
                    "Release $tag has no .apk file attached. Found: ${assetNames.joinToString(", ")}."
                )
            }

            if (apkAssets.size > 1) {
                return FetchReleaseResult.Failure(
                    "Release $tag has ${apkAssets.size} .apk files attached, expected exactly one: " +
                        apkAssets.joinToString(", ") { it.first }
                )
            }

            val (_, apkUrl, apkSize) = apkAssets.first()

            return FetchReleaseResult.Success(
                GithubRelease(
                    tagName = tag,
                    versionName = versionName,
                    versionCode = versionCode,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl,
                    apkExpectedBytes = apkSize,
                    htmlUrl = htmlUrl
                )
            )
        } catch (e: SocketTimeoutException) {
            return FetchReleaseResult.Failure("Timed out contacting GitHub. Check your connection and try again.")
        } catch (e: IOException) {
            return FetchReleaseResult.Failure("Network error while contacting GitHub: ${e.localizedMessage ?: e.javaClass.simpleName}")
        } catch (e: Exception) {
            return FetchReleaseResult.Failure("Unexpected error checking for updates: ${e.localizedMessage ?: e.javaClass.simpleName}")
        } finally {
            conn?.disconnect()
        }
    }

    private fun describeHttpError(conn: HttpURLConnection, responseCode: Int): String {
        return when (responseCode) {
            HttpURLConnection.HTTP_NOT_FOUND ->
                "No published release found for $GITHUB_OWNER/$GITHUB_REPO. " +
                    "(The latest release may still be a draft or marked as pre-release — " +
                    "GitHub's \"latest\" endpoint skips those.)"

            HttpURLConnection.HTTP_FORBIDDEN, 429 -> {
                val remaining = conn.getHeaderField("X-RateLimit-Remaining")
                val resetEpoch = conn.getHeaderField("X-RateLimit-Reset")?.toLongOrNull()
                val resetInfo = if (resetEpoch != null) {
                    val waitSeconds = (resetEpoch * 1000 - System.currentTimeMillis()) / 1000
                    if (waitSeconds > 0) " Try again in ~${waitSeconds}s." else ""
                } else ""
                "GitHub API rate limit hit (remaining: ${remaining ?: "unknown"}).$resetInfo"
            }

            else -> {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                } catch (e: IOException) {
                    null
                }
                "GitHub API returned HTTP $responseCode." +
                    if (!errorBody.isNullOrBlank()) " Response: ${errorBody.take(200)}" else ""
            }
        }
    }

    /**
     * Converts "1.3.0" → 1_003_000, "1.10.2" → 1_010_002, etc.
     * Formula: major * 1_000_000 + minor * 1_000 + patch.
     * Must stay in sync with computeVersionCode() in build.gradle.
     * Returns null if versionName isn't in a recognizable numeric dotted format.
     */
    private fun parseVersionCode(versionName: String): Int? {
        val parts = versionName.split(".")
        if (parts.isEmpty() || parts.size > 3 || parts.any { it.toIntOrNull() == null }) return null
        val nums = parts.map { it.toInt() }
        val major = nums.getOrElse(0) { 0 }
        val minor = nums.getOrElse(1) { 0 }
        val patch = nums.getOrElse(2) { 0 }
        return major * 1_000_000 + minor * 1_000 + patch
    }

    //  Download

    /**
     * Downloads the APK using DownloadManager and emits [DownloadState] updates.
     * Always performs a fresh download for the requested release — any existing
     * file for this or an older release is deleted first, so a stale or corrupt
     * on-disk APK can never be silently reused. This is the key fix versus the
     * previous version: no "already exists, skip straight to Downloaded" path.
     */
    fun downloadUpdate(release: GithubRelease): Flow<DownloadState> = callbackFlow {
        val apkFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "StudySphere-${release.tagName}-update.apk"
        )

        // Wipe any previous update APKs (this version's or older) before starting.
        // We never trust a pre-existing file, so this always results in a real download.
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.listFiles { f ->
                (f.name.startsWith("StudySphere-") && f.name.endsWith("-update.apk"))
                    || f.name == "StudySphere-update.apk"
            }
            ?.forEach { it.delete() }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(release.apkDownloadUrl)).apply {
            setTitle("StudySphere ${release.versionName}")
            setDescription("Downloading update…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationUri(Uri.fromFile(apkFile))
            setMimeType("application/vnd.android.package-archive")
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
            // Cache-busting: force a real network fetch rather than letting
            // DownloadManager or an intermediate proxy serve a cached/stale asset.
            addRequestHeader("Cache-Control", "no-cache")
        }

        val downloadId = dm.enqueue(request)
        trySend(DownloadState.Downloading(0))

        // Guards against the coroutine poll loop and the broadcast receiver both
        // trying to finish the flow.
        val completed = AtomicBoolean(false)

        fun finishSuccess() {
            if (!completed.compareAndSet(false, true)) return
            // Verify size against what GitHub reported for the asset, if known,
            // so a truncated download never gets treated as a valid APK.
            val expected = release.apkExpectedBytes
            if (expected > 0 && apkFile.length() < expected) {
                trySend(
                    DownloadState.Failed(
                        "Downloaded file is smaller than expected (${apkFile.length()} of $expected bytes) — likely a truncated download."
                    )
                )
            } else if (!apkFile.exists() || apkFile.length() == 0L) {
                trySend(DownloadState.Failed("Download reported success but the file is missing or empty."))
            } else {
                trySend(DownloadState.Downloaded(apkFile))
            }
            channel.close()
        }

        fun finishFailure(reason: String) {
            if (!completed.compareAndSet(false, true)) return
            trySend(DownloadState.Failed(reason))
            channel.close()
        }

        // Broadcast receiver — fast path, fires as soon as the system reports completion.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != downloadId || completed.get()) return

                val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    cursor.close()
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        finishSuccess()
                    } else {
                        finishFailure("Download failed (status $status, reason $reason)")
                    }
                } else {
                    cursor?.close()
                    finishFailure("Download record not found")
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }

        // Coroutine-based poll — reliable fallback in case the broadcast is never
        // delivered (happens on some OEM ROMs / Android 13+ receiver quirks).
        // Using delay() instead of a raw Thread means cancellation is immediate:
        // when the flow collector goes away, this coroutine is cancelled right
        // away rather than finishing its current sleep first.
        val pollJob = launch(Dispatchers.IO) {
            while (isActive && !completed.get()) {
                delay(POLL_INTERVAL_MS)
                val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor == null || !cursor.moveToFirst()) {
                    cursor?.close()
                    continue
                }
                val status  = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val dlBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total   = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val reason  = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                cursor.close()

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        finishSuccess()
                        return@launch
                    }
                    DownloadManager.STATUS_FAILED -> {
                        finishFailure("Download failed (reason $reason)")
                        return@launch
                    }
                    else -> {
                        if (total > 0) {
                            trySend(DownloadState.Downloading(((dlBytes * 100) / total).toInt()))
                        }
                    }
                }
            }
        }

        awaitClose {
            completed.set(true)
            pollJob.cancel()
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    //  Install

    /**
     * Launches Android's package installer for the downloaded APK.
     * Requires the FileProvider authority declared in AndroidManifest.
     */
    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.update.provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Deletes the temporary APK from cache. Call after install prompt is shown. */
    fun cleanUp(apkFile: File) {
        if (apkFile.exists()) apkFile.delete()
    }
}