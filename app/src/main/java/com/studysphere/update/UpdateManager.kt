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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

//  Data Models 

data class GithubRelease(
    val tagName: String,           // e.g. "v1.3.0"
    val versionName: String,       // e.g. "1.3.0"
    val versionCode: Int,          // parsed from tag, e.g. 130
    val releaseNotes: String,
    val apkDownloadUrl: String,
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

//  UpdateManager 

class UpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_OWNER = "Chirag8405"
        private const val GITHUB_REPO = "StudySphere"
        private const val APK_ASSET_NAME = "StudySphere.apk"   // must match your release asset name
        private const val RELEASES_API =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    //  Version check 

    /**
     * Fetches the latest GitHub release and compares it against the installed versionCode.
     * Call from a ViewModel / coroutine scope (runs on IO dispatcher internally).
     */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val release = fetchLatestRelease()
                ?: return@withContext UpdateCheckResult.Error("No release found")

            val installedCode = installedVersionCode()
            return@withContext if (release.versionCode > installedCode) {
                UpdateCheckResult.UpdateAvailable(release)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "Unknown error")
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

    private fun fetchLatestRelease(): GithubRelease? {
        val conn = URL(RELEASES_API).openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        if (conn.responseCode != 200) return null

        val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val json = JSONObject(body)

        val tag = json.getString("tag_name")          // "v1.3.0"
        val versionName = tag.trimStart('v')           // "1.3.0"
        val versionCode = parseVersionCode(versionName)
        val releaseNotes = json.optString("body", "")
        val htmlUrl = json.optString("html_url", "")

        val assets = json.getJSONArray("assets")
        var apkUrl = ""
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name") == APK_ASSET_NAME) {
                apkUrl = asset.getString("browser_download_url")
                break
            }
        }

        if (apkUrl.isEmpty()) return null

        return GithubRelease(tag, versionName, versionCode, releaseNotes, apkUrl, htmlUrl)
    }

    /**
     * Converts "1.3.0" → 130, "1.10.2" → 11002, etc.
     * Must match the versionCode logic you use in build.gradle.
     * Default: major*10000 + minor*100 + patch
     */
    private fun parseVersionCode(versionName: String): Int {
        val parts = versionName.split(".").map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return major * 10_000 + minor * 100 + patch
    }

    //  Download 

    /**
     * Downloads the APK using DownloadManager and emits [DownloadState] updates.
     * The returned Flow completes (or emits Failed) when the download finishes.
     */
    fun downloadUpdate(release: GithubRelease): Flow<DownloadState> = callbackFlow {
        val apkFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "StudySphere-update.apk"
        )
        if (apkFile.exists()) apkFile.delete()

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
        }

        val downloadId = dm.enqueue(request)
        trySend(DownloadState.Downloading(0))

        // Poll progress
        var polling = true
        val pollingThread = Thread {
            while (polling) {
                Thread.sleep(500)
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesDownloaded =
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal =
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (bytesTotal > 0) {
                        val percent = ((bytesDownloaded * 100) / bytesTotal).toInt()
                        trySend(DownloadState.Downloading(percent))
                    }
                    cursor.close()
                }
            }
        }
        pollingThread.start()

        // Completion receiver
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != downloadId) return

                polling = false

                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val status =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    cursor.close()
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        trySend(DownloadState.Downloaded(apkFile))
                    } else {
                        trySend(DownloadState.Failed("Download failed (status $status)"))
                    }
                } else {
                    trySend(DownloadState.Failed("Download record not found"))
                }
                channel.close()
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

        awaitClose {
            polling = false
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
            "${context.packageName}.update.provider",   // authority declared in manifest
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