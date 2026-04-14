package com.studysphere.ui.screens.attendance

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studysphere.data.models.*
import com.studysphere.ui.components.*
import com.studysphere.ui.theme.*
import com.studysphere.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailScreen(
    subjectId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val subjects  by viewModel.subjects.collectAsState()
    val subject   = subjects.find { it.id == subjectId }

    // Collect records for this subject
    val records   by viewModel.getRecordsBySubject(subjectId).collectAsState(initial = emptyList())
    val lectures  by viewModel.getLecturesBySubject(subjectId).collectAsState(initial = emptyList())
    val summaries by viewModel.attendanceSummariesRefreshed.collectAsState()
    val summary   = summaries.find { it.subject.id == subjectId }

    var showMarkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(subjectId) { viewModel.refreshSummaries() }

    if (subject == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Subject not found")
        }
        return
    }

    val subjectColor = remember(subject.colorHex) {
        try { Color(android.graphics.Color.parseColor(subject.colorHex)) }
        catch (e: Exception) { Indigo500 }
    }

    // Group records by date descending
    val groupedRecords = remember(records) {
        records.sortedByDescending { it.date }.groupBy { it.date }
    }

    Scaffold(
        floatingActionButton = {
            if (lectures.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showMarkDialog = true },
                    icon    = { Icon(Icons.Rounded.EditCalendar, null) },
                    text    = { Text("Mark Attendance") },
                    containerColor = subjectColor,
                    contentColor   = Color.White
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary card
            item {
                if (summary != null) {
                    DetailSummaryCard(summary = summary, subjectColor = subjectColor)
                }
            }

            // Lecture schedule card
            if (lectures.isNotEmpty()) {
                item {
                    LectureScheduleCard(lectures = lectures, subject = subject)
                }
            }

            // History header
            item {
                SectionHeader(
                    title = "Attendance History",
                    modifier = Modifier
                )
            }

            if (groupedRecords.isEmpty()) {
                item {
                    SphereCard(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            icon     = Icons.Rounded.History,
                            title    = "No Records Yet",
                            subtitle = "Mark attendance to see history here",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                groupedRecords.forEach { (date, dayRecords) ->
                    item(key = date) {
                        AttendanceDayGroup(
                            date       = date,
                            records    = dayRecords,
                            lectures   = lectures,
                            subject    = subject,
                            onStatusChange = { record, newStatus ->
                                viewModel.markAttendance(
                                    lectureId = record.lectureId,
                                    subjectId = subjectId,
                                    date      = record.date,
                                    status    = newStatus
                                )
                                viewModel.refreshSummaries()
                            }
                        )
                    }
                }
            }
        }
    }

    // Mark attendance dialog
    if (showMarkDialog) {
        MarkAttendanceDialog(
            subject   = subject,
            lectures  = lectures,
            subjectColor = subjectColor,
            onDismiss = { showMarkDialog = false },
            onMark    = { lectureId, date, status ->
                viewModel.markAttendance(lectureId, subjectId, date, status)
                viewModel.refreshSummaries()
                showMarkDialog = false
            }
        )
    }
}

@Composable
private fun DetailSummaryCard(summary: SubjectAttendanceSummary, subjectColor: Color) {
    SphereCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Big percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${summary.percentage.toInt()}%",
                         style = MaterialTheme.typography.displayMedium,
                         fontWeight = FontWeight.Bold,
                         color = subjectColor)
                    Text("of ${summary.subject.minAttendancePercent.toInt()}% required",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RiskIndicator(summary.riskLevel)
                    Text("${summary.attended} / ${summary.totalClasses} classes",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AttendanceProgressBar(
                percentage   = summary.percentage,
                minThreshold = summary.subject.minAttendancePercent,
                colorHex     = summary.subject.colorHex
            )

            // Insight cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightMini(
                    label = "Can Skip",
                    value = if (summary.canSkip > 0) summary.canSkip.toString() else "0",
                    color = if (summary.canSkip > 0) Green500 else Slate500,
                    icon  = Icons.Rounded.EventBusy,
                    modifier = Modifier.weight(1f)
                )
                InsightMini(
                    label = "Must Attend",
                    value = if (summary.mustAttend > 0) summary.mustAttend.toString() else "On Track",
                    color = if (summary.mustAttend > 0) Red500 else Green500,
                    icon  = if (summary.mustAttend > 0) Icons.Rounded.Warning else Icons.Rounded.Verified,
                    modifier = Modifier.weight(1f)
                )
                InsightMini(
                    label = "Cancelled",
                    value = summary.cancelled.toString(),
                    color = Amber500,
                    icon  = Icons.Rounded.RemoveCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InsightMini(
    label: String, value: String, color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = color)
        Text(value, style = MaterialTheme.typography.titleSmall,
             fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LectureScheduleCard(lectures: List<Lecture>, subject: Subject) {
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    SphereCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Weekly Schedule", style = MaterialTheme.typography.titleSmall,
                 fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            lectures.sortedWith(compareBy({ it.dayOfWeek }, { it.startTimeHour }, { it.startTimeMinute }))
                .forEach { lecture ->
                val timeStr = "%02d:%02d – %02d:%02d".format(
                    lecture.startTimeHour, lecture.startTimeMinute,
                    lecture.endTimeHour, lecture.endTimeMinute
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dayNames.getOrElse(lecture.dayOfWeek - 1) { "?" },
                                 style = MaterialTheme.typography.labelSmall,
                                 fontWeight = FontWeight.SemiBold,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(timeStr, style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onBackground)
                    }
                    if (lecture.room.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Rounded.LocationOn, null,
                                 Modifier.size(12.dp),
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lecture.room, style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceDayGroup(
    date: String,
    records: List<AttendanceRecord>,
    lectures: List<Lecture>,
    subject: Subject,
    onStatusChange: (AttendanceRecord, AttendanceStatus) -> Unit
) {
    val parsedDate = remember(date) {
        try {
            val ld = LocalDate.parse(date)
            ld.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
        } catch (e: Exception) { date }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(parsedDate, style = MaterialTheme.typography.labelMedium,
             fontWeight = FontWeight.SemiBold,
             color = MaterialTheme.colorScheme.onSurfaceVariant,
             modifier = Modifier.padding(vertical = 2.dp))
        records.forEach { record ->
            val lecture = lectures.find { it.id == record.lectureId }
            AttendanceRecordRow(
                record  = record,
                lecture = lecture,
                onStatusChange = { newStatus -> onStatusChange(record, newStatus) }
            )
        }
    }
}

@Composable
private fun AttendanceRecordRow(
    record: AttendanceRecord,
    lecture: Lecture?,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    SphereCard(modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lecture != null) {
                        val timeStr = "%02d:%02d".format(lecture.startTimeHour, lecture.startTimeMinute)
                        Text(timeStr, style = MaterialTheme.typography.bodySmall,
                             fontWeight = FontWeight.Medium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AttendanceStatusChip(record.status)
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "Toggle",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Change Status", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AttendanceStatus.values().forEach { status ->
                            val isSelected = record.status == status
                            val (chipColor, label) = when (status) {
                                AttendanceStatus.PRESENT   -> Green500 to "Present"
                                AttendanceStatus.ABSENT    -> Red500 to "Absent"
                                AttendanceStatus.CANCELLED -> Amber500 to "Cancelled"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick  = { if (!isSelected) onStatusChange(status) },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(0.18f),
                                    selectedLabelColor     = chipColor
                                ),
                                shape    = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkAttendanceDialog(
    subject: Subject,
    lectures: List<Lecture>,
    subjectColor: Color,
    onDismiss: () -> Unit,
    onMark: (Long, String, AttendanceStatus) -> Unit
) {
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    var selectedLectureId by remember { mutableStateOf(lectures.firstOrNull()?.id ?: 0L) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectColorDot(subject.colorHex, size = 10.dp)
                Text("Mark Attendance", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Lecture selector
                Text("Select Lecture", style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lectures.forEach { lecture ->
                        val timeStr = "%02d:%02d – %02d:%02d".format(
                            lecture.startTimeHour, lecture.startTimeMinute,
                            lecture.endTimeHour, lecture.endTimeMinute
                        )
                        val dayName = dayNames.getOrElse(lecture.dayOfWeek - 1) { "?" }
                        val isSelected = selectedLectureId == lecture.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) subjectColor.copy(0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedLectureId = lecture.id }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$dayName · $timeStr",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = if (isSelected) subjectColor
                                 else MaterialTheme.colorScheme.onSurfaceVariant)
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, null,
                                     Modifier.size(16.dp), tint = subjectColor)
                            }
                        }
                    }
                }

                // Date input
                Text("Date", style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                            Column {
                                Text(
                                    text = selectedDate.toAttendanceDateLabel(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = selectedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Rounded.EditCalendar,
                            contentDescription = "Select attendance date"
                        )
                    }
                }

                // Status selector
                Text("Status", style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttendanceStatus.values().forEach { status ->
                        val isSelected = selectedStatus == status
                        val (chipColor, label) = when (status) {
                            AttendanceStatus.PRESENT   -> Green500 to "Present"
                            AttendanceStatus.ABSENT    -> Red500 to "Absent"
                            AttendanceStatus.CANCELLED -> Amber500 to "Cancelled"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick  = { selectedStatus = status },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(0.18f),
                                selectedLabelColor     = chipColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedLectureId != 0L) {
                        onMark(selectedLectureId, selectedDate, selectedStatus)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = subjectColor),
                shape  = RoundedCornerShape(12.dp)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val initialDate = selectedDate.toLocalDateOrNull() ?: LocalDate.now()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toEpochMillis())

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis ?: initialDate.toEpochMillis()
                        selectedDate = millis.toIsoDateString()
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                showModeToggle = false,
                title = null,
                headline = null
            )
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toAttendanceDateLabel(): String {
    val date = toLocalDateOrNull() ?: return this
    return date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toIsoDateString(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}
