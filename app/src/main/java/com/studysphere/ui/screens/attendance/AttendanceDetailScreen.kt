package com.studysphere.ui.screens.attendance

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    var recordToDelete by remember { mutableStateOf<AttendanceRecord?>(null) }

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
            ExtendedFloatingActionButton(
                onClick = { showMarkDialog = true },
                icon    = { Icon(Icons.Rounded.EditCalendar, null) },
                text    = { Text("Mark Attendance") },
                containerColor = subjectColor,
                contentColor   = Color.White
            )
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
                                viewModel.updateAttendanceStatus(record, newStatus)
                            },
                            onDelete = { recordToDelete = it }
                        )
                    }
                }
            }
        }
    }

    // Mark attendance dialog
    if (showMarkDialog) {
        MarkAttendanceDialog(
            subject      = subject,
            lectures     = lectures,
            subjectColor = subjectColor,
            onDismiss    = { showMarkDialog = false },
            onMark       = { lectureId, date, status ->
                viewModel.markAttendance(lectureId, subjectId, date, status)
                showMarkDialog = false
            },
            onMarkExtra = { date, status, startH, startM, endH, endM, room ->
                viewModel.markExtraAttendance(subjectId, date, status, startH, startM, endH, endM, room)
                showMarkDialog = false
            }
        )
    }

    // Delete confirmation
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Record?") },
            text = { Text("This will permanently remove this attendance record. The subject and timetable will not be affected.") },
            confirmButton = {
                Button(
                    onClick = {
                        recordToDelete?.let { viewModel.deleteAttendanceRecord(it) }
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Cancel") }
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
    onStatusChange: (AttendanceRecord, AttendanceStatus) -> Unit,
    onDelete: (AttendanceRecord) -> Unit
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
                onStatusChange = { newStatus -> onStatusChange(record, newStatus) },
                onDelete = { onDelete(record) }
            )
        }
    }
}

@Composable
private fun AttendanceRecordRow(
    record: AttendanceRecord,
    lecture: Lecture?,
    onStatusChange: (AttendanceStatus) -> Unit,
    onDelete: () -> Unit
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
                    if (record.isExtra) {
                        val timeStr = "%02d:%02d".format(record.startTimeHour, record.startTimeMinute)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = Amber500)
                            Text(timeStr, style = MaterialTheme.typography.bodySmall,
                                 fontWeight = FontWeight.Medium,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (lecture != null) {
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
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (record.isExtra && record.room.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.LocationOn, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(record.room, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text("Change Status", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error)
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
    onMark: (Long, String, AttendanceStatus) -> Unit,
    onMarkExtra: (String, AttendanceStatus, Int, Int, Int, Int, String) -> Unit
) {
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    var isExtraMode by remember { mutableStateOf(lectures.isEmpty()) }

    var selectedLectureId by remember { mutableStateOf(lectures.firstOrNull()?.id ?: 0L) }
    val selectedLecture = remember(selectedLectureId, lectures) {
        lectures.find { it.id == selectedLectureId }
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }

    // When mode or lecture changes, snap selectedDate to the nearest valid date in the past
    LaunchedEffect(isExtraMode, selectedLectureId) {
        if (!isExtraMode && selectedLecture != null) {
            val current = LocalDate.parse(selectedDate)
            if (current.dayOfWeek.value != selectedLecture.dayOfWeek || current.isAfter(LocalDate.now())) {
                // Find most recent Monday/Tuesday/etc. that is not in future
                var date = LocalDate.now()
                while (date.dayOfWeek.value != selectedLecture.dayOfWeek) {
                    date = date.minusDays(1)
                }
                selectedDate = date.toString()
            }
        } else if (isExtraMode) {
            val current = LocalDate.parse(selectedDate)
            if (current.isAfter(LocalDate.now())) {
                selectedDate = LocalDate.now().toString()
            }
        }
    }

    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Extra mode state
    var startH by remember { mutableStateOf(9) }
    var startM by remember { mutableStateOf(0) }
    var endH by remember { mutableStateOf(10) }
    var endM by remember { mutableStateOf(0) }
    var room by remember { mutableStateOf("") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val isTimeValid = remember(startH, startM, endH, endM) {
        (startH * 60 + startM) < (endH * 60 + endM)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectColorDot(subject.colorHex, size = 10.dp)
                Text(if (isExtraMode) "Extra Lecture" else "Mark Attendance",
                     style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Mode Switcher
                TabRow(
                    selectedTabIndex = if (isExtraMode) 1 else 0,
                    containerColor = Color.Transparent,
                    contentColor = subjectColor,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (isExtraMode) 1 else 0]),
                            color = subjectColor
                        )
                    }
                ) {
                    Tab(selected = !isExtraMode, onClick = { isExtraMode = false }) {
                        Text("Timetable", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    Tab(selected = isExtraMode, onClick = { isExtraMode = true }) {
                        Text("Extra/Surprise", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (!isExtraMode) {
                    // Lecture selector
                    Text("Select Scheduled Lecture", style = MaterialTheme.typography.labelMedium,
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
                } else {
                    // Extra lecture inputs
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Time & Room", style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedCard(
                                onClick = { showStartTimePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text("Start Time", style = MaterialTheme.typography.labelSmall)
                                    Text("%02d:%02d".format(startH, startM), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedCard(
                                onClick = { showEndTimePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text("End Time", style = MaterialTheme.typography.labelSmall)
                                    Text("%02d:%02d".format(endH, endM), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it },
                            label = { Text("Room / Location") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.LocationOn, null) }
                        )

                        if (!isTimeValid) {
                            Text(
                                "End time must be after start time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 4.dp)
                            )
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
                    if (isExtraMode) {
                        if (isTimeValid) {
                            onMarkExtra(selectedDate, selectedStatus, startH, startM, endH, endM, room)
                        }
                    } else if (selectedLectureId != 0L) {
                        onMark(selectedLectureId, selectedDate, selectedStatus)
                    }
                },
                enabled = if (isExtraMode) isTimeValid else selectedLectureId != 0L,
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
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toEpochMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate()
                    val today = LocalDate.now()

                    // Rule 1: No future dates
                    if (date.isAfter(today)) return false

                    // Rule 2: If in timetable mode, only match the lecture's day of week
                    if (!isExtraMode && selectedLecture != null) {
                        return date.dayOfWeek.value == selectedLecture.dayOfWeek
                    }

                    return true
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year <= LocalDate.now().year
                }
            }
        )

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

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { h, m ->
                startH = h
                startM = m
                showStartTimePicker = false
            },
            initialHour = startH,
            initialMinute = startM,
            title = "Start Time"
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { h, m ->
                endH = h
                endM = m
                showEndTimePicker = false
            },
            initialHour = endH,
            initialMinute = endM,
            title = "End Time"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    initialHour: Int,
    initialMinute: Int,
    title: String
) {
    val state = rememberTimePickerState(initialHour, initialMinute, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onTimeSelected(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        }
    )
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
