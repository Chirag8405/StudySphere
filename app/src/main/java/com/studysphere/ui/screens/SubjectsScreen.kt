package com.studysphere.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studysphere.data.models.*
import com.studysphere.ui.components.*
import com.studysphere.ui.theme.*
import com.studysphere.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SubjectsScreen(viewModel: MainViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val lectures by viewModel.allLectures.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    
    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { viewModel.refreshAll() }
    )

    var showAddSubjectDialog   by remember { mutableStateOf(false) }
    var editingSubject         by remember { mutableStateOf<Subject?>(null) }
    var deleteSubjectTarget    by remember { mutableStateOf<Subject?>(null) }
    var showAddLectureDialog   by remember { mutableStateOf(false) }
    var editingLecture         by remember { mutableStateOf<Lecture?>(null) }
    var addLectureForSubjectId by remember { mutableStateOf<Long?>(null) }
    var deleteLectureTarget    by remember { mutableStateOf<Lecture?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = { showAddSubjectDialog = true },
                icon           = { Icon(Icons.Rounded.Add, null) },
                text           = { Text("Add Subject") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = Color.White
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            if (subjects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon        = Icons.Rounded.School,
                        title       = "No Subjects Yet",
                        subtitle    = "Add your first subject to get started",
                        actionLabel = "Add Subject",
                        onAction    = { showAddSubjectDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 2.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(subjects, key = { it.id }) { subject ->
                        val subjectLectures = lectures.filter { it.subjectId == subject.id }
                        SubjectCard(
                            subject  = subject,
                            lectures = subjectLectures,
                            onEdit   = { editingSubject = subject },
                            onDelete = { deleteSubjectTarget = subject },
                            onAddLecture = {
                                addLectureForSubjectId = subject.id
                                showAddLectureDialog = true
                            },
                            onEditLecture = { editingLecture = it },
                            onDeleteLecture = { deleteLectureTarget = it }
                        )
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

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        AddEditSubjectDialog(
            existing   = null,
            onDismiss  = { showAddSubjectDialog = false },
            onSave     = { name, colorHex, minAtt ->
                viewModel.addSubject(name, colorHex, minAtt)
                showAddSubjectDialog = false
            }
        )
    }

    // Edit Subject Dialog
    if (editingSubject != null) {
        AddEditSubjectDialog(
            existing   = editingSubject,
            onDismiss  = { editingSubject = null },
            onSave     = { name, colorHex, minAtt ->
                viewModel.updateSubject(
                    editingSubject!!.copy(name = name, colorHex = colorHex, minAttendancePercent = minAtt)
                )
                editingSubject = null
            }
        )
    }

    // Delete Subject confirm
    if (deleteSubjectTarget != null) {
        ConfirmDeleteDialog(
            title     = "Delete Subject?",
            message   = "This will permanently delete \"${deleteSubjectTarget!!.name}\" and ALL its attendance records and assignments.",
            onConfirm = {
                viewModel.deleteSubject(deleteSubjectTarget!!)
                deleteSubjectTarget = null
            },
            onDismiss = { deleteSubjectTarget = null }
        )
    }

    // Add/Edit Lecture Dialog
    if ((showAddLectureDialog && addLectureForSubjectId != null) || editingLecture != null) {
        val subjectId = addLectureForSubjectId ?: editingLecture?.subjectId
        val subject = subjects.find { it.id == subjectId }
        if (subject != null) {
            AddEditLectureDialog(
                subject   = subject,
                existing  = editingLecture,
                onDismiss = {
                    showAddLectureDialog   = false
                    addLectureForSubjectId = null
                    editingLecture         = null
                },
                onSave    = { dow, sH, sM, eH, eM, room ->
                    if (editingLecture != null) {
                        viewModel.updateLecture(
                            editingLecture!!.copy(
                                dayOfWeek = dow,
                                startTimeHour = sH, startTimeMinute = sM,
                                endTimeHour = eH, endTimeMinute = eM,
                                room = room
                            )
                        ) { success ->
                            if (!success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error: Time overlap with another lecture")
                                }
                            }
                        }
                    } else {
                        viewModel.addLecture(subject.id, dow, sH, sM, eH, eM, room) { success ->
                            if (!success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error: Time overlap with another lecture")
                                }
                            }
                        }
                    }
                    showAddLectureDialog   = false
                    addLectureForSubjectId = null
                    editingLecture         = null
                }
            )
        }
    }

    // Delete Lecture confirm
    if (deleteLectureTarget != null) {
        ConfirmDeleteDialog(
            title     = "Delete Lecture?",
            message   = "This will remove the lecture slot and all its attendance records.",
            onConfirm = {
                viewModel.deleteLecture(deleteLectureTarget!!)
                deleteLectureTarget = null
            },
            onDismiss = { deleteLectureTarget = null }
        )
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    lectures: List<Lecture>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddLecture: () -> Unit,
    onEditLecture: (Lecture) -> Unit,
    onDeleteLecture: (Lecture) -> Unit
) {
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var menuExpanded by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(true) }

    SphereCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subject.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column {
                        Text(subject.name, style = MaterialTheme.typography.titleSmall,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.onBackground)
                        Text("Min. ${subject.minAttendancePercent.toInt()}% attendance · ${lectures.size} lecture${if (lectures.size == 1) "" else "s"}/week",
                             style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.MoreVert, null, Modifier.size(18.dp),
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit", style = MaterialTheme.typography.bodySmall) },
                                onClick = { menuExpanded = false; onEdit() },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Lecture", style = MaterialTheme.typography.bodySmall) },
                                onClick = { menuExpanded = false; onAddLecture() },
                                leadingIcon = { Icon(Icons.Rounded.AddCircle, null, Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Delete", style = MaterialTheme.typography.bodySmall,
                                         color = MaterialTheme.colorScheme.error)
                                },
                                onClick = { menuExpanded = false; onDelete() },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, null, Modifier.size(16.dp),
                                         tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }

            // Lectures list
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (lectures.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClick = onAddLecture)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.AddCircleOutline, null,
                                 Modifier.size(16.dp),
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Add first lecture slot",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        lectures.sortedWith(
                            compareBy({ it.dayOfWeek }, { it.startTimeHour }, { it.startTimeMinute })
                        ).forEach { lecture ->
                            LectureRow(
                                lecture  = lecture,
                                dayNames = dayNames,
                                onEdit   = { onEditLecture(lecture) },
                                onDelete = { onDeleteLecture(lecture) }
                            )
                        }
                        TextButton(
                            onClick = onAddLecture,
                            modifier = Modifier.align(Alignment.Start),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text("Add lecture", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureRow(
    lecture: Lecture,
    dayNames: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeStr = "%02d:%02d – %02d:%02d".format(
        lecture.startTimeHour, lecture.startTimeMinute,
        lecture.endTimeHour, lecture.endTimeMinute
    )
    val dayName = dayNames.getOrElse(lecture.dayOfWeek - 1) { "?" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(dayName, style = MaterialTheme.typography.labelSmall,
                     fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Icon(Icons.Rounded.Schedule, null, Modifier.size(13.dp),
                 tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(timeStr, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onBackground)
            if (lecture.room.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Rounded.LocationOn, null, Modifier.size(12.dp),
                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(lecture.room, style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                         maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(15.dp),
                 tint = MaterialTheme.colorScheme.error.copy(0.7f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditSubjectDialog(
    existing: Subject?,
    onDismiss: () -> Unit,
    onSave: (String, String, Float) -> Unit
) {
    var name       by remember { mutableStateOf(existing?.name ?: "") }
    // Always use PrimaryPurple for subjects now
    val colorHex   = "#7F22FE" 
    var minAtt     by remember { mutableStateOf(existing?.minAttendancePercent ?: 75f) }
    var nameError  by remember { mutableStateOf(false) }

    val isDark = LocalDarkTheme.current
    val shape = RoundedCornerShape(20.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, if (isDark) Gray700 else Gray400, shape),
        containerColor = if (isDark) PureBlack else Color.White,
        shape = shape,
        title = {
            Text(if (existing == null) "New Subject" else "Edit Subject",
                 style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; nameError = false },
                    label         = { Text("Subject Name") },
                    isError       = nameError,
                    supportingText = { if (nameError) Text("Name is required") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp)
                )

                // Min attendance slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Minimum Attendance",
                             style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${minAtt.toInt()}%",
                             style = MaterialTheme.typography.labelMedium,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.onBackground)
                    }
                    Slider(
                        value        = minAtt,
                        onValueChange = { minAtt = it },
                        valueRange   = 50f..100f,
                        steps        = 9,
                        colors       = SliderDefaults.colors(
                            thumbColor       = MaterialTheme.colorScheme.onBackground,
                            activeTrackColor = MaterialTheme.colorScheme.onBackground,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("50%", style = MaterialTheme.typography.labelSmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100%", style = MaterialTheme.typography.labelSmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = name.isBlank()
                    if (!nameError) onSave(name.trim(), colorHex, minAtt)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) { Text(if (existing == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditLectureDialog(
    subject: Subject,
    existing: Lecture? = null,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int, Int, Int, String) -> Unit
) {
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var selectedDay by remember { mutableIntStateOf(existing?.dayOfWeek ?: 1) }
    var startHour   by remember { mutableIntStateOf(existing?.startTimeHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(existing?.startTimeMinute ?: 0) }
    var endHour     by remember { mutableIntStateOf(existing?.endTimeHour ?: 10) }
    var endMinute   by remember { mutableIntStateOf(existing?.endTimeMinute ?: 0) }
    var room        by remember { mutableStateOf(existing?.room ?: "") }
    var timeError   by remember { mutableStateOf(false) }

    val isDark = LocalDarkTheme.current
    val shape = RoundedCornerShape(20.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, if (isDark) Gray700 else Gray400, shape),
        containerColor = if (isDark) PureBlack else Color.White,
        shape = shape,
        title = {
            Text(if (existing == null) "Add Lecture to ${subject.name}" else "Edit ${subject.name} Lecture", 
                 style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Day selector
                Text("Day of Week", style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(dayNames) { index, dayName ->
                        val dow = index + 1
                        val isSelected = selectedDay == dow
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedDay = dow },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dayName.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.background
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Time pickers
                if (timeError) {
                    Text("End time must be after start time",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.error,
                         fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        TimeInput(
                            hour = startHour, minute = startMinute,
                            onHourChange   = { startHour = it },
                            onMinuteChange = { startMinute = it }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        TimeInput(
                            hour = endHour, minute = endMinute,
                            onHourChange   = { endHour = it },
                            onMinuteChange = { endMinute = it }
                        )
                    }
                }

                // Room
                OutlinedTextField(
                    value         = room,
                    onValueChange = { room = it },
                    label         = { Text("Room / Location (optional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    leadingIcon   = { Icon(Icons.Rounded.LocationOn, null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startTotal = startHour * 60 + startMinute
                    val endTotal   = endHour * 60 + endMinute
                    timeError = endTotal <= startTotal
                    if (!timeError) {
                        onSave(selectedDay, startHour, startMinute, endHour, endMinute, room.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape  = RoundedCornerShape(12.dp)
            ) { Text(if (existing == null) "Add Lecture" else "Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun TimeInput(
    hour: Int, minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NumberStepper(
            value     = hour,
            min       = 0,
            max       = 23,
            label     = "h",
            onChange  = onHourChange,
            modifier  = Modifier.weight(1f)
        )
        Text(":", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
             color = MaterialTheme.colorScheme.onBackground)
        NumberStepper(
            value     = minute,
            min       = 0,
            max       = 59,
            step      = 5,
            label     = "m",
            onChange  = onMinuteChange,
            modifier  = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NumberStepper(
    value: Int,
    min: Int,
    max: Int,
    label: String,
    onChange: (Int) -> Unit,
    step: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick  = { if (value + step <= max) onChange(value + step) else onChange(max) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Rounded.KeyboardArrowUp, null, Modifier.size(20.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("%02d".format(value),
                 style = MaterialTheme.typography.titleSmall,
                 fontWeight = FontWeight.Bold,
                 color = MaterialTheme.colorScheme.onBackground)
        }
        IconButton(
            onClick  = { if (value - step >= min) onChange(value - step) else onChange(min) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
