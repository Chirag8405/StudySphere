package com.studysphere.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysphere.R
import com.studysphere.ui.components.SphereCard
import com.studysphere.ui.theme.*
import com.studysphere.update.DownloadState
import com.studysphere.update.UpdateDialog
import com.studysphere.update.UpdateViewModel
import com.studysphere.viewmodel.ImportResult
import com.studysphere.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate

// ─── SettingsScreen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    updateViewModel: UpdateViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Collect state ─────────────────────────────────────────────────────────

    val themeMode          by viewModel.themeMode.collectAsStateWithLifecycle()
    val workingDays        by viewModel.workingDaysPerWeek.collectAsStateWithLifecycle()
    val deadlineWindow     by viewModel.deadlineWindowDays.collectAsStateWithLifecycle()
    val lastBackupAt       by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val subjects           by viewModel.subjects.collectAsStateWithLifecycle()
    val allLectures        by viewModel.allLectures.collectAsStateWithLifecycle()
    val allAssignments     by viewModel.allAssignments.collectAsStateWithLifecycle()
    val updateState        by updateViewModel.state.collectAsStateWithLifecycle()
    val appVersionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    }

    // ── Dialog state booleans ─────────────────────────────────────────────────

    var showImportTimetableConfirm   by remember { mutableStateOf(false) }
    var showImportBackupConfirm      by remember { mutableStateOf(false) }
    var pendingImportTimetableUri    by remember { mutableStateOf<Uri?>(null) }
    var pendingImportBackupUri       by remember { mutableStateOf<Uri?>(null) }

    // Feature 5: Two-step semester reset
    var showSemesterStep1  by remember { mutableStateOf(false) }
    var showSemesterStep2  by remember { mutableStateOf(false) }

    // Feature 6: Single-step clear assignments
    var showClearAssignments by remember { mutableStateOf(false) }

    // Feature 13: Two-step reset all data
    var showResetStep1  by remember { mutableStateOf(false) }
    var showResetStep2  by remember { mutableStateOf(false) }

    // ── SAF Launchers ─────────────────────────────────────────────────────────

    val today = LocalDate.now().toString().replace("-", "")

    // Export timetable
    val exportTimetableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.exportTimetable(uri, context)
                .onSuccess {
                    snackbarHostState.showSnackbar("Timetable exported successfully")
                }
                .onFailure { e ->
                    snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}")
                }
        }
    }

    // Export full backup
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            viewModel.exportFullBackup(uri, context)
                .onSuccess {
                    snackbarHostState.showSnackbar("Full backup exported successfully")
                }
                .onFailure { e ->
                    snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}")
                }
        }
    }

    // Import timetable — pick file, then show confirm dialog
    val importTimetableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingImportTimetableUri  = uri
            showImportTimetableConfirm = true
        }
    }

    // Import full backup — pick file, then show confirm dialog
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingImportBackupUri  = uri
            showImportBackupConfirm = true
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── 1. APPEARANCE ─────────────────────────────────────────────────

            SettingsSectionHeader("Appearance", Icons.Rounded.Palette)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsLabel("Theme")
                    ThemeSegmentedButtons(
                        selected = themeMode,
                        onSelect = { viewModel.setThemeMode(it) }
                    )
                }
            }

            // ── 2. ATTENDANCE DEFAULTS ────────────────────────────────────────

            SettingsSectionHeader("Attendance", Icons.Rounded.HowToReg)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsLabel("Working days per week")
                        Text(
                            "Used for attendance health calculations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(5, 6).forEach { days ->
                                val selected = workingDays == days
                                FilterChip(
                                    selected = selected,
                                    onClick  = { viewModel.setWorkingDays(days) },
                                    label    = {
                                        Text(
                                            "$days Days",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor     = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. ASSIGNMENTS ────────────────────────────────────────────────

            SettingsSectionHeader("Assignments", Icons.Rounded.Assignment)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsLabel("Deadline window")
                    Text(
                        "Days before deadline to show in 'Due Soon'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5, 7).forEach { days ->
                            val selected = deadlineWindow == days
                            FilterChip(
                                selected = selected,
                                onClick  = { viewModel.setDeadlineWindow(days) },
                                label    = {
                                    Text(
                                        "${days}d",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor     = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                border = null
                            )
                        }
                    }
                    // Notification hint — informs the user that this setting
                    // also controls when daily push notifications fire.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            "You'll be notified daily at 8:00 AM for assignments due within this window",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }


            // ── 4. TIMETABLE ──────────────────────────────────────────────────

            SettingsSectionHeader("Timetable", Icons.Rounded.CalendarMonth)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsActionRow(
                        icon        = Icons.Rounded.FileDownload,
                        iconTint    = MaterialTheme.colorScheme.onSurfaceVariant,
                        title       = "Export Timetable",
                        subtitle    = "JSON file with subjects & lectures",
                        onClick     = {
                            exportTimetableLauncher.launch("studysphere_timetable_$today.json")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        icon        = Icons.Rounded.FileUpload,
                        iconTint    = MaterialTheme.colorScheme.onSurfaceVariant,
                        title       = "Import Timetable",
                        subtitle    = "Restore subjects & lectures",
                        onClick     = {
                            importTimetableLauncher.launch("application/json")
                        }
                    )
                }
            }

            // ── 5. BACKUP & RESTORE ───────────────────────────────────────────

            SettingsSectionHeader("Backup & Restore", Icons.Rounded.Backup)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsActionRow(
                        icon     = Icons.Rounded.FileDownload,
                        iconTint = Green500,
                        title    = "Full Export",
                        subtitle = "Includes attendance & assignments",
                        onClick  = {
                            exportBackupLauncher.launch("studysphere_backup_$today.json")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        icon     = Icons.Rounded.FileUpload,
                        iconTint = Green500,
                        title    = "Restore Backup",
                        subtitle = "Full data restoration from file",
                        onClick  = {
                            importBackupLauncher.launch("application/json")
                        }
                    )
                    if (lastBackupAt.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Green500
                            )
                            Text(
                                "Last: ${formatBackupTimestamp(lastBackupAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── 6. STATISTICS ─────────────────────────────────────────────────

            SettingsSectionHeader("Statistics", Icons.Rounded.BarChart)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DataStatRow("Subjects",      subjects.size.toString(),       Icons.Rounded.School)
                    DataStatRow("Schedule Slots", allLectures.size.toString(),   Icons.Rounded.CalendarToday)
                    DataStatRow("Assignments",   allAssignments.size.toString(), Icons.Rounded.Assignment)
                }
            }

            // ── 7. DANGER ZONE ────────────────────────────────────────────────

            SettingsSectionHeader("Danger Zone", Icons.Rounded.Warning, tint = Red500)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsActionRow(
                        icon     = Icons.Rounded.RestartAlt,
                        iconTint = Amber500,
                        title    = "New Semester",
                        subtitle = "Clear schedule & history",
                        onClick  = { showSemesterStep1 = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        icon     = Icons.Rounded.DeleteSweep,
                        iconTint = Red500,
                        title    = "Clear Assignments",
                        subtitle = "Delete all assignment records",
                        onClick  = { showClearAssignments = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        icon     = Icons.Rounded.DeleteForever,
                        iconTint = Red600,
                        title    = "Factory Reset",
                        subtitle = "Wipe all app data permanently",
                        onClick  = { showResetStep1 = true }
                    )
                }
            }

            // ── 8. ABOUT ──────────────────────────────────────────────────────

            SettingsSectionHeader("About", Icons.Rounded.Info)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (LocalDarkTheme.current) PureBlack else Gray100)
                                .border(1.dp, if (LocalDarkTheme.current) Gray800 else Gray300, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Column {
                            Text(
                                "StudySphere",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            // Live version string — replaces hardcoded "Version 1.2.0"
                            Text(
                                "Version $appVersionName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Show download progress inline under the version if active
                            val dlState = updateState.downloadState
                            if (dlState is DownloadState.Downloading) {
                                Text(
                                    "Downloading update… ${dlState.progressPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryPurple
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        "Modern Attendance & Assignment Tracker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // ── Check for updates row ─────────────────────────────────
                    CheckForUpdatesRow(
                        updateState = updateState,
                        onCheck     = { updateViewModel.checkForUpdate() },
                        onReopen    = { updateViewModel.reopenDialog() }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // View on GitHub
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Chirag8405/StudySphere")
                                )
                                context.startActivity(intent)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null,
                             Modifier.size(20.dp),
                             tint = MaterialTheme.colorScheme.onBackground)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Open Source",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "github.com/Chirag8405/StudySphere",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, null,
                             Modifier.size(20.dp),
                             tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ── Update dialog — rendered outside Scaffold so it overlays everything ──

    UpdateDialog(
        state           = updateState,
        onStartDownload = { updateViewModel.startDownload() },
        onInstall       = { updateViewModel.installAndCleanup(it) },
        onDismiss       = { updateViewModel.dismissDialog() }
    )

    // ── All other dialogs (unchanged) ─────────────────────────────────────────

    if (showImportTimetableConfirm) {
        AlertDialog(
            onDismissRequest = { showImportTimetableConfirm = false; pendingImportTimetableUri = null },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.FileUpload, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Import Timetable?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "This will add subjects and lectures to your existing data. Continue?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportTimetableUri
                        showImportTimetableConfirm = false
                        pendingImportTimetableUri  = null
                        if (uri != null) {
                            scope.launch {
                                viewModel.importTimetable(uri, context)
                                    .onSuccess { result ->
                                        snackbarHostState.showSnackbar("Imported ${result.subjectsAdded} subjects")
                                    }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showImportTimetableConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportBackupConfirm) {
        AlertDialog(
            onDismissRequest = { showImportBackupConfirm = false; pendingImportBackupUri = null },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.Backup, null, tint = Green500) },
            title = { Text("Restore Backup?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text  = {
                Text("This will restore all records from the selected file. Continue?", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportBackupUri
                        showImportBackupConfirm = false
                        pendingImportBackupUri  = null
                        if (uri != null) {
                            scope.launch {
                                viewModel.importFullBackup(uri, context)
                                    .onSuccess { snackbarHostState.showSnackbar("Backup restored successfully") }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showImportBackupConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showSemesterStep1) {
        AlertDialog(
            onDismissRequest = { showSemesterStep1 = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.RestartAlt, null, tint = Amber500) },
            title = { Text("Semester Reset", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text  = {
                Text("This will clear all lectures and attendance history. Continue?", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(onClick = { showSemesterStep1 = false; showSemesterStep2 = true }, shape = RoundedCornerShape(12.dp)) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSemesterStep1 = false }) { Text("Cancel") }
            }
        )
    }

    if (showSemesterStep2) {
        AlertDialog(
            onDismissRequest = { showSemesterStep2 = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.Warning, null, tint = Red500) },
            title = { Text("Final Warning", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text  = {
                Text("All schedule slots and attendance will be erased. This cannot be undone.", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSemesterStep2 = false
                        viewModel.clearSemesterData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Yes, Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showSemesterStep2 = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearAssignments) {
        AlertDialog(
            onDismissRequest = { showClearAssignments = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.DeleteSweep, null, tint = Red500) },
            title = { Text("Clear Assignments?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text  = {
                Text("Every assignment record will be permanently deleted. Continue?", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAssignments = false
                        viewModel.clearAllAssignments()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAssignments = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetStep1) {
        AlertDialog(
            onDismissRequest = { showResetStep1 = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.DeleteForever, null, tint = Red600) },
            title = { Text("Factory Reset?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text  = {
                Text("Wipe ALL data? This is permanent and irreversible.", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(onClick = { showResetStep1 = false; showResetStep2 = true }, shape = RoundedCornerShape(12.dp)) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStep1 = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetStep2) {
        AlertDialog(
            onDismissRequest = { showResetStep2 = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.Warning, null, tint = Red600) },
            title = { Text("Absolute Final Warning", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text  = {
                Text("Everything will be erased. Are you sure?", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetStep2 = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Yes, Wipe Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showResetStep2 = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Check for updates row ────────────────────────────────────────────────────

@Composable
private fun CheckForUpdatesRow(
    updateState: com.studysphere.update.UpdateUiState,
    onCheck: () -> Unit,
    onReopen: () -> Unit,
) {
    val dlState   = updateState.downloadState
    val hasUpdate = updateState.availableRelease != null

    val subtitle = when {
        updateState.isChecking                      -> "Connecting to GitHub…"
        updateState.checkError != null              -> "Tap to retry"
        dlState is DownloadState.Downloading        -> "Downloading ${dlState.progressPercent}% — tap to view"
        dlState is DownloadState.Downloaded         -> "Ready to install — tap to open"
        hasUpdate                                   -> "v${updateState.availableRelease!!.versionName} is available"
        updateState.hasChecked                      -> "You're on the latest version"
        else                                        -> "Tap to check"
    }

    val subtitleColor = when {
        hasUpdate || dlState is DownloadState.Downloaded -> PrimaryPurple
        updateState.checkError != null                   -> MaterialTheme.colorScheme.error
        else                                             -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val iconTint = when {
        hasUpdate || dlState is DownloadState.Downloaded -> PrimaryPurple
        else                                             -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val onClick: () -> Unit = when {
        updateState.isChecking                                   -> ({})
        dlState is DownloadState.Downloading
            || dlState is DownloadState.Downloaded              -> onReopen
        else                                                     -> onCheck
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !updateState.isChecking, onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.SystemUpdateAlt,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = iconTint
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "Check for updates",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
        // Trailing — spinner while checking, badge when update waiting, chevron otherwise
        when {
            updateState.isChecking -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            dlState is DownloadState.Downloading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = PrimaryPurple
                )
            }
            hasUpdate || dlState is DownloadState.Downloaded -> {
                Badge(containerColor = PrimaryPurple) {
                    Text(
                        if (dlState is DownloadState.Downloaded) "!" else "New",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            else -> {
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ─── Sub-composables (all unchanged) ─────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint.copy(alpha = 0.7f))
        Text(
            text  = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconTint)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                 fontWeight = FontWeight.Bold,
                 color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null,
             Modifier.size(20.dp),
             tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun DataStatRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null,
             modifier = Modifier.size(18.dp),
             tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
             modifier = Modifier.weight(1f),
             fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodySmall,
             fontWeight = FontWeight.Bold,
             color = MaterialTheme.colorScheme.onBackground)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSegmentedButtons(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        Triple("system", "System", Icons.Rounded.Brightness4),
        Triple("light",  "Light",  Icons.Rounded.LightMode),
        Triple("dark",   "Dark",   Icons.Rounded.DarkMode)
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label, icon) ->
            SegmentedButton(
                selected = selected == mode,
                onClick  = { onSelect(mode) },
                shape    = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon     = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Helpers (unchanged) ──────────────────────────────────────────────────────

private fun formatBackupTimestamp(isoString: String): String {
    return try {
        val instant   = Instant.parse(isoString)
        val zonedTime = instant.atZone(ZoneId.systemDefault())
        zonedTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
    } catch (e: Exception) {
        isoString
    }
}