package com.studysphere.update

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studysphere.ui.theme.Green500
import com.studysphere.ui.theme.PrimaryPurple
import com.studysphere.ui.theme.Red500
import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
//  UpdateDialog
//
//  Matches StudySphere's existing AlertDialog style (no bottom sheet).
//  Four visual states, single composable:
//
//    CHECKING        → spinner  (brief; shown while network call is in flight)
//    UPDATE_AVAILABLE → version + release notes + Cancel / Update buttons
//                       ↳ downloading sub-state: progress bar + "Background" / disabled
//                       ↳ downloaded sub-state:  progress gone + "Install now"
//    UP_TO_DATE      → green check + "You're on the latest version" + Close
//    ERROR           → red error icon + message + Close
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onStartDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.showDialog && !state.isChecking) return

    when {
        state.isChecking -> CheckingDialog(onDismiss = onDismiss)

        state.checkError != null -> StatusDialog(
            icon     = Icons.Rounded.ErrorOutline,
            iconTint = Red500,
            title    = "Couldn't check for updates",
            body     = state.checkError,
            onDismiss = onDismiss
        )

        state.availableRelease != null -> UpdateAvailableDialog(
            state           = state,
            onStartDownload = onStartDownload,
            onInstall       = onInstall,
            onDismiss       = onDismiss
        )

        state.hasChecked -> StatusDialog(
            icon      = Icons.Rounded.CheckCircle,
            iconTint  = Green500,
            title     = "You're up to date",
            body      = "StudySphere is running the latest version.",
            onDismiss = onDismiss
        )
    }
}

//  State: checking 

@Composable
private fun CheckingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                "Checking for updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = PrimaryPurple
                )
                Text(
                    "Connecting to GitHub…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    )
}

//  State: up to date / error (shared layout) 

@Composable
private fun StatusDialog(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    )
}

//  State: update available (idle / downloading / downloaded) 

@Composable
private fun UpdateAvailableDialog(
    state: UpdateUiState,
    onStartDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    val release = state.availableRelease ?: return
    val dlState = state.downloadState

    AlertDialog(
        onDismissRequest = {
            // Allow dismissal always — download keeps going in background
            onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.SystemUpdateAlt,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Update available",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // Version chip — same FilterChip style the screen uses
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "v${release.versionName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Release notes (capped at 280 chars so dialog doesn't overflow)
                if (release.releaseNotes.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = release.releaseNotes.take(280).let {
                                if (release.releaseNotes.length > 280) "$it…" else it
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Data-safe note
                Text(
                    "Your subjects, attendance, and assignments are safe during updates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Download progress — only shown while/after downloading
                if (dlState is DownloadState.Downloading) {
                    val pct = dlState.progressPercent
                    val progress by animateFloatAsState(
                        targetValue = pct / 100f,
                        animationSpec = tween(300),
                        label = "dl_progress"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryPurple,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "Downloading… $pct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Download failure
                if (dlState is DownloadState.Failed) {
                    Text(
                        "Download failed: ${dlState.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    when (dlState) {
                        is DownloadState.Downloading -> "Continue in background"
                        else -> "Cancel"
                    }
                )
            }
        },
        confirmButton = {
            when (dlState) {
                is DownloadState.Idle, is DownloadState.Failed -> {
                    Button(
                        onClick = onStartDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple
                        )
                    ) {
                        Text(if (dlState is DownloadState.Failed) "Retry" else "Update")
                    }
                }
                is DownloadState.Downloading -> {
                    // Disabled while in-flight; LinearProgressIndicator above shows status
                    Button(
                        onClick = {},
                        enabled = false
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
                is DownloadState.Downloaded -> {
                    Button(
                        onClick = { onInstall(dlState.apkFile) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green500,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Install now")
                    }
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    )
}