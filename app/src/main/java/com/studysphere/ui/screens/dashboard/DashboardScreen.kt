package com.studysphere.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studysphere.data.models.*
import com.studysphere.ui.components.*
import com.studysphere.ui.theme.*
import com.studysphere.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToSubjectDetail: (Long) -> Unit,
    onNavigateToSubjects: () -> Unit
) {
    val subjects           by viewModel.subjects.collectAsState()
    val todayLectures      by viewModel.todayLectures.collectAsState()
    val upcomingAssign     by viewModel.upcomingAssignments.collectAsState()
    val summaries          by viewModel.attendanceSummariesRefreshed.collectAsState()
    val refreshing         by viewModel.isRefreshing.collectAsState()
    val deadlineWindowDays by viewModel.deadlineWindowDays.collectAsState()
    val selectedDate       by viewModel.selectedDate.collectAsState()

    val today = LocalDate.now()
    val isToday = selectedDate == today
    val isFutureDate = selectedDate.isAfter(today)
    
    val dateDisplayStr = if (isToday) "Today's Schedule" else selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    val headerDateStr = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { viewModel.refreshAll() }
    )

    LaunchedEffect(Unit) { viewModel.refreshSummaries() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                DashboardHeader(dateStr = headerDateStr)
            }

            item {
                StatsRow(
                    subjects      = subjects,
                    summaries     = summaries,
                    upcomingCount = upcomingAssign.count { it.daysUntilDue in 0..deadlineWindowDays },
                    modifier      = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                SectionHeader(
                    title = dateDisplayStr,
                    action = "View All",
                    onAction = onNavigateToAttendance,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
                
                DateSlider(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.setSelectedDate(it) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (todayLectures.isEmpty()) {
                item {
                    SphereCard(modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    ) {
                        EmptyState(
                            icon     = Icons.Rounded.EventAvailable,
                            title    = if (isToday) "No Classes Today" else "No Classes Scheduled",
                            subtitle = if (isToday) "Enjoy your free day!" else "Nothing for ${selectedDate.format(DateTimeFormatter.ofPattern("d MMM"))}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            } else {
                items(todayLectures, key = { it.lecture.id }) { todayLecture ->
                    TodayLectureCard(
                        todayLecture = todayLecture,
                        onMark       = { status ->
                            viewModel.quickMarkToday(todayLecture.lecture.id, todayLecture.subject.id, status)
                        },
                        canMark      = !isFutureDate,
                        modifier     = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }

            item {
                SectionHeader(
                    title    = "Attendance Health",
                    action   = "Details",
                    onAction = onNavigateToAttendance,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            if (summaries.isEmpty()) {
                item {
                    SphereCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        onClick  = onNavigateToSubjects
                    ) {
                        EmptyState(
                            icon        = Icons.Rounded.School,
                            title       = "No Subjects Yet",
                            subtitle    = "Add subjects to track attendance",
                            actionLabel = "Add Subject",
                            onAction    = onNavigateToSubjects,
                            modifier    = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            } else {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summaries.take(4).forEach { summary ->
                            AttendanceHealthCard(
                                summary = summary,
                                onClick = { onNavigateToSubjectDetail(summary.subject.id) }
                            )
                        }
                        if (summaries.size > 4) {
                            TextButton(
                                onClick = onNavigateToAttendance,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("+${summaries.size - 4} more subjects",
                                     style = MaterialTheme.typography.labelMedium,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                     fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            item {
                SectionHeader(
                    title    = "Upcoming Deadlines",
                    action   = "View All",
                    onAction = onNavigateToAssignments,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            if (upcomingAssign.isEmpty()) {
                item {
                    SphereCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    ) {
                        EmptyState(
                            icon     = Icons.Rounded.AssignmentTurnedIn,
                            title    = "All Clear!",
                            subtitle = "No pending assignments",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                items(upcomingAssign.take(4), key = { it.assignment.id }) { ua ->
                    DashboardAssignmentCard(
                        ua       = ua,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                if (upcomingAssign.size > 4) {
                    item {
                        TextButton(
                            onClick  = onNavigateToAssignments,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Text("+${upcomingAssign.size - 4} more assignments",
                                 style = MaterialTheme.typography.labelMedium,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                                 fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DashboardHeader(dateStr: String) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..6 -> "Up Early?"
        in 7..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..23 -> "Good Evening"
        else -> "Still Awake?"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(
    subjects: List<Subject>,
    summaries: List<SubjectAttendanceSummary>,
    upcomingCount: Int,
    modifier: Modifier = Modifier
) {
    val atRisk = summaries.count { it.riskLevel == RiskLevel.DANGER || it.riskLevel == RiskLevel.CRITICAL }
    val avgPct = if (summaries.isEmpty()) 0f else summaries.map { it.percentage }.average().toFloat()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label  = "Subjects",
            value  = subjects.size.toString(),
            icon   = Icons.Rounded.LibraryBooks,
            color  = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label  = "Avg Attend.",
            value  = if (summaries.isEmpty() || summaries.all { it.totalClasses == 0 }) "—" else "${avgPct.toInt()}%",
            icon   = Icons.Rounded.Percent,
            color  = if (avgPct >= 75) Green500 else Red500,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label  = "Due Soon",
            value  = upcomingCount.toString(),
            icon   = Icons.Rounded.Alarm,
            color  = if (upcomingCount > 0) Amber500 else Green500,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label  = "At Risk",
            value  = atRisk.toString(),
            icon   = Icons.Rounded.ErrorOutline,
            color  = if (atRisk > 0) Red500 else Green500,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    SphereCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                     modifier = Modifier.size(17.dp), tint = color)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSlider(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    var showDatePicker by remember { mutableStateOf(false) }

    // Display a range of dates: 2 weeks back to 2 weeks forward
    val dates = remember {
        (-14..14).map { today.plusDays(it.toLong()) }
    }
    
    // Initial index to keep today in the middle. 
    // With -14 to 14, today is at index 14.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 11)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.ChevronLeft,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dates) { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    
                    Surface(
                        onClick = { onDateSelected(date) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) 
                                else if (isToday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) 
                                 else if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) 
                                 else null,
                        modifier = Modifier.width(54.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("EEE")),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            IconButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = java.time.Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(date)
                    }
                    showDatePicker = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }, 
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = Color.White,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun TodayLectureCard(
    todayLecture: TodayLecture,
    onMark: (AttendanceStatus) -> Unit,
    canMark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val startH = todayLecture.lecture.startTimeHour
    val startM = todayLecture.lecture.startTimeMinute
    val endH   = todayLecture.lecture.endTimeHour
    val endM   = todayLecture.lecture.endTimeMinute
    val timeStr = "%02d:%02d – %02d:%02d".format(startH, startM, endH, endM)

    SphereCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(todayLecture.subject.name,
                         style = MaterialTheme.typography.titleSmall,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.onBackground)
                    if (todayLecture.lecture.id == -1L) {
                        Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = Amber500)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Rounded.Schedule, null,
                             Modifier.size(12.dp),
                             tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(timeStr, style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (todayLecture.lecture.room.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Rounded.LocationOn, null,
                                 Modifier.size(12.dp),
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(todayLecture.lecture.room,
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            // Quick mark buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AttendanceStatusChip(todayLecture.attendanceRecord?.status)
                if (todayLecture.attendanceRecord == null && canMark) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        QuickMarkButton("P", Green500) { onMark(AttendanceStatus.PRESENT) }
                        QuickMarkButton("A", Red500) { onMark(AttendanceStatus.ABSENT) }
                        QuickMarkButton("C", Amber500) { onMark(AttendanceStatus.CANCELLED) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMarkButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AttendanceHealthCard(
    summary: SubjectAttendanceSummary,
    onClick: () -> Unit
) {
    SphereCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(summary.subject.name,
                         style = MaterialTheme.typography.titleSmall,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.onBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (summary.totalClasses == 0) "—" else "${summary.percentage.toInt()}%",
                             style = MaterialTheme.typography.titleSmall,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.onBackground)
                        RiskIndicator(summary.riskLevel, compact = true)
                    }
                }
                AttendanceProgressBar(
                    percentage   = summary.percentage,
                    minThreshold = summary.subject.minAttendancePercent,
                    colorHex     = "",
                    totalClasses = summary.totalClasses
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${summary.attended}/${summary.totalClasses} attended",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (summary.canSkip > 0) {
                        Text("Can skip ${summary.canSkip} more",
                             style = MaterialTheme.typography.labelSmall,
                             color = Green500,
                             fontWeight = FontWeight.Bold)
                    } else if (summary.mustAttend > 0) {
                        Text("Attend ${summary.mustAttend} to recover",
                             style = MaterialTheme.typography.labelSmall,
                             color = Red500,
                             fontWeight = FontWeight.Bold)
                    }
                }
            }
            Icon(Icons.Rounded.ChevronRight, null,
                 Modifier.size(18.dp),
                 tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun DashboardAssignmentCard(
    ua: UpcomingAssignment,
    modifier: Modifier = Modifier
) {
    val urgencyColor = when {
        ua.daysUntilDue < 0 -> Red600
        ua.daysUntilDue == 0L -> Red500
        ua.daysUntilDue <= 2 -> Amber500
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dueLabel = when {
        ua.daysUntilDue < 0  -> "Overdue"
        ua.daysUntilDue == 0L -> "Today"
        ua.daysUntilDue == 1L -> "Tomorrow"
        else                  -> "In ${ua.daysUntilDue}d"
    }

    SphereCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(urgencyColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Assignment, null,
                     Modifier.size(18.dp), tint = urgencyColor)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(ua.assignment.title,
                     style = MaterialTheme.typography.titleSmall,
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onBackground,
                     maxLines = 1)
                SubjectChip(ua.subject)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(dueLabel, style = MaterialTheme.typography.labelSmall,
                     color = urgencyColor, fontWeight = FontWeight.Bold)
                PriorityBadge(ua.assignment.priority)
            }
        }
    }
}
