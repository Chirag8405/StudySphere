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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studysphere.ui.components.SphereCard
import com.studysphere.ui.theme.*
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
    onBack: () -> Unit
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── 1. APPEARANCE ─────────────────────────────────────────────────

            SettingsSectionHeader("Appearance", Icons.Rounded.Palette)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsLabel("Theme")
                    ThemeSegmentedButtons(
                        selected = themeMode,
                        onSelect = { viewModel.setThemeMode(it) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 2. ATTENDANCE DEFAULTS ────────────────────────────────────────

            SettingsSectionHeader("Attendance Defaults", Icons.Rounded.HowToReg)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Working days
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsLabel("Working days per week")
                        Text(
                            "Used for attendance calculations on the dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 6).forEach { days ->
                                val selected = workingDays == days
                                FilterChip(
                                    selected = selected,
                                    onClick  = { viewModel.setWorkingDays(days) },
                                    label    = {
                                        Text(
                                            "$days days",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 3. ASSIGNMENTS ────────────────────────────────────────────────

            SettingsSectionHeader("Assignments", Icons.Rounded.Assignment)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsLabel("Upcoming deadline window")
                    Text(
                        "Assignments due within this many days appear in 'Due Soon' on the dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
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
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 4. TIMETABLE ──────────────────────────────────────────────────

            SettingsSectionHeader("Timetable", Icons.Rounded.CalendarMonth)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsActionRow(
                        icon        = Icons.Rounded.FileDownload,
                        iconTint    = Indigo500,
                        title       = "Export Timetable",
                        subtitle    = "Save subjects & lectures as JSON",
                        onClick     = {
                            exportTimetableLauncher.launch("studysphere_timetable_$today.json")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    SettingsActionRow(
                        icon        = Icons.Rounded.FileUpload,
                        iconTint    = Indigo500,
                        title       = "Import Timetable",
                        subtitle    = "Add subjects & lectures from JSON",
                        onClick     = {
                            importTimetableLauncher.launch("application/json")
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 5. BACKUP & RESTORE ───────────────────────────────────────────

            SettingsSectionHeader("Backup & Restore", Icons.Rounded.Backup)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsActionRow(
                        icon     = Icons.Rounded.FileDownload,
                        iconTint = Green600,
                        title    = "Export Full Backup",
                        subtitle = "Save all data including attendance & assignments",
                        onClick  = {
                            exportBackupLauncher.launch("studysphere_backup_$today.json")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    SettingsActionRow(
                        icon     = Icons.Rounded.FileUpload,
                        iconTint = Green600,
                        title    = "Import / Restore Backup",
                        subtitle = "Restore from a previously exported backup",
                        onClick  = {
                            importBackupLauncher.launch("application/json")
                        }
                    )
                    if (lastBackupAt.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Last backup: ${formatBackupTimestamp(lastBackupAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 6. DATA STATISTICS ────────────────────────────────────────────

            SettingsSectionHeader("Data Statistics", Icons.Rounded.BarChart)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DataStatRow("Subjects",         subjects.size.toString(),    Icons.Rounded.School)
                    DataStatRow("Lecture Slots",    allLectures.size.toString(), Icons.Rounded.CalendarToday)
                    DataStatRow("Assignments",      allAssignments.size.toString(), Icons.Rounded.Assignment)
                    // Note: attendance records are not exposed as a count StateFlow, so show from summaries
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.CloudDone, null,
                             Modifier.size(16.dp),
                             tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (lastBackupAt.isBlank()) "Never backed up"
                            else "Last backup: ${formatBackupTimestamp(lastBackupAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 7. DANGER ZONE ────────────────────────────────────────────────

            SettingsSectionHeader("Danger Zone", Icons.Rounded.Warning, tint = Red500)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Feature 5: New Semester Reset
                    SettingsActionRow(
                        icon     = Icons.Rounded.RestartAlt,
                        iconTint = Amber500,
                        title    = "New Semester Reset",
                        subtitle = "Delete all lectures & attendance, keep subjects & assignments",
                        onClick  = { showSemesterStep1 = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    // Feature 6: Clear All Assignments
                    SettingsActionRow(
                        icon     = Icons.Rounded.DeleteSweep,
                        iconTint = Red500,
                        title    = "Clear All Assignments",
                        subtitle = "Permanently delete every assignment",
                        onClick  = { showClearAssignments = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    // Feature 13: Reset All Data
                    SettingsActionRow(
                        icon     = Icons.Rounded.DeleteForever,
                        iconTint = Red600,
                        title    = "Reset All Data",
                        subtitle = "Wipe everything — subjects, attendance, assignments",
                        onClick  = { showResetStep1 = true }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 8. ABOUT ──────────────────────────────────────────────────────

            SettingsSectionHeader("About", Icons.Rounded.Info)
            SphereCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoStories, null,
                                 Modifier.size(24.dp),
                                 tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column {
                            Text(
                                "StudySphere",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Version 1.1.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Text(
                        "Built with Jetpack Compose · Room · DataStore",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Developed by Chirag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    // View on GitHub
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Chirag8405/StudySphere")
                                )
                                context.startActivity(intent)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null,
                             Modifier.size(18.dp),
                             tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "View on GitHub",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "github.com/Chirag8405/StudySphere",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, null,
                             Modifier.size(18.dp),
                             tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                    }
                }
            }

            // Bottom padding
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    // Import timetable confirmation
    if (showImportTimetableConfirm) {
        AlertDialog(
            onDismissRequest = { showImportTimetableConfirm = false; pendingImportTimetableUri = null },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.FileUpload, null, tint = Indigo500) },
            title = { Text("Import Timetable?", style = MaterialTheme.typography.headlineSmall) },
            text  = {
                Text(
                    "This will ADD the imported subjects and lectures to your existing data. " +
                    "Duplicate subjects (same name) will be skipped. Continue?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        snackbarHostState.showSnackbar(
                                            "Imported: ${result.subjectsAdded} subjects, " +
                                            "${result.lecturesAdded} lectures added " +
                                            "(duplicates skipped)"
                                        )
                                    }
                                    .onFailure { e ->
                                        val msg = if (e.message == "version_mismatch") "Invalid file format"
                                                  else "Invalid file format"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportTimetableConfirm = false
                    pendingImportTimetableUri  = null
                }) { Text("Cancel") }
            }
        )
    }

    // Import full backup confirmation
    if (showImportBackupConfirm) {
        AlertDialog(
            onDismissRequest = { showImportBackupConfirm = false; pendingImportBackupUri = null },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.Backup, null, tint = Green600) },
            title = { Text("Restore Backup?", style = MaterialTheme.typography.headlineSmall) },
            text  = {
                Text(
                    "This will ADD all data from the backup to your existing data. " +
                    "Existing records are not deleted. Duplicate subjects (by name) will be " +
                    "reused, not duplicated. Continue?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                                    .onSuccess { result ->
                                        snackbarHostState.showSnackbar(
                                            "Restored: ${result.subjectsAdded} subjects, " +
                                            "${result.lecturesAdded} lectures, " +
                                            "${result.recordsAdded} records, " +
                                            "${result.assignmentsAdded} assignments added " +
                                            "(duplicates skipped)"
                                        )
                                    }
                                    .onFailure {
                                        snackbarHostState.showSnackbar("Invalid file format")
                                    }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportBackupConfirm = false
                    pendingImportBackupUri  = null
                }) { Text("Cancel") }
            }
        )
    }

    // Feature 5 — Semester Reset: Step 1
    if (showSemesterStep1) {
        AlertDialog(
            onDismissRequest = { showSemesterStep1 = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.RestartAlt, null, tint = Amber500) },
            title = { Text("Clear Semester Data?", style = MaterialTheme.typography.headlineSmall) },
            text  = {
                Text(
                    "This will delete all lecture slots and attendance history. " +
                    "Your subjects and assignments will be kept.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSemesterStep1 = false; showSemesterStep2 = true },
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showSemesterStep1 = false }) { Text("Cancel") }
            }
        )
    }

    // Feature 5 — Semester Reset: Step 2
    if (showSemesterStep2) {
        AlertDialog(
            onDismissRequest = { showSemesterStep2 = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.Warning, null, tint = Red500) },
            title = { Text("Are you absolutely sure?", style = MaterialTheme.typography.headlineSmall) },
            text  = {
                Text(
                    "This cannot be undone. All lecture slots and attendance records will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSemesterStep2 = false
                        viewModel.clearSemesterData()
                        scope.launch {
                            snackbarHostState.showSnackbar("Semester data cleared")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor   = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Yes, Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showSemesterStep2 = false }) { Text("Cancel") }
            }
        )
    }

    // Feature 6 — Clear All Assignments
    if (showClearAssignments) {
        AlertDialog(
            onDismissRequest = { showClearAssignments = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.DeleteSweep, null, tint = Red500) },
            title = { Text("Delete All Assignments?", style = MaterialTheme.typography.headlineSmall) },
            text  = {
                Text(
                    "This cannot be undone. Every assignment regardless of status will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAssignments = false
                        viewModel.clearAllAssignments()
                        scope.launch {
                            snackbarHostState.showSnackbar("All assignments deleted")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor   = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Delete All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAssignments = false }) { Text("Cancel") }
            }
        )
    }

    // Feature 13 — Reset All Data: Step 1
    if (showResetStep1) {
        AlertDialog(
            onDismissRequest = { showResetStep1 = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.DeleteForever, null, tint = Red500) },
            title = { Text("Reset All Data?", style = MaterialTheme.typography.headlineSmall) },
            text  = {
                Text(
                    "This will permanently wipe all subjects, lectures, attendance records, and assignments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showResetStep1 = false; showResetStep2 = true },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor   = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showResetStep1 = false }) { Text("Cancel") }
            }
        )
    }

    // Feature 13 — Reset All Data: Step 2
    if (showResetStep2) {
        AlertDialog(
            onDismissRequest = { showResetStep2 = false },
            shape = RoundedCornerShape(20.dp),
            icon  = { Icon(Icons.Rounded.Warning, null, tint = Red600) },
            title = { Text("Are you absolutely sure?", style = MaterialTheme.typography.headlineSmall) },
            text  = {
                Text(
                    "This action CANNOT be undone. All your data will be permanently erased.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetStep2 = false
                        viewModel.clearAllData()
                        scope.launch {
                            snackbarHostState.showSnackbar("All data has been reset")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor   = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Yes, Reset Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showResetStep2 = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
        Text(
            text  = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconTint)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                 fontWeight = FontWeight.SemiBold,
                 color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null,
             Modifier.size(18.dp),
             tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun DataStatRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null,
             modifier = Modifier.size(16.dp),
             tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
             modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall,
             fontWeight = FontWeight.SemiBold,
             color = MaterialTheme.colorScheme.onBackground)
    }
}

// Feature 7: Theme segmented buttons — System / Light / Dark
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
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatBackupTimestamp(isoString: String): String {
    return try {
        val instant   = Instant.parse(isoString)
        val zonedTime = instant.atZone(ZoneId.systemDefault())
        zonedTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
    } catch (e: Exception) {
        isoString
    }
}
