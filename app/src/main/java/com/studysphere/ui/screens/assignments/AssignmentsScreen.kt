package com.studysphere.ui.screens.assignments

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studysphere.data.models.*
import com.studysphere.ui.components.*
import com.studysphere.ui.theme.*
import com.studysphere.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class AssignmentMetaFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    DONE("Done"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AssignmentsScreen(
    viewModel: MainViewModel
) {
    val subjects       by viewModel.subjects.collectAsState()
    val allAssignments by viewModel.allAssignments.collectAsState()
    val refreshing     by viewModel.isRefreshing.collectAsState()

    val assignmentSubjectOptions = remember(subjects) { listOf(otherSubject()) + subjects }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }
    var deleteTarget by remember { mutableStateOf<Assignment?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var metaFilter by remember { mutableStateOf(AssignmentMetaFilter.ALL) }
    var filterSubjectId by remember { mutableStateOf<Long?>(null) }

    val filtered = remember(allAssignments, searchQuery, metaFilter, filterSubjectId, subjects) {
        val normalizedQuery = searchQuery.trim()
        allAssignments.filter { a ->
            val assignmentSubject = resolveAssignmentSubject(a, subjects)
            val matchesQuery = normalizedQuery.isBlank() ||
                a.title.contains(normalizedQuery, ignoreCase = true) ||
                a.description.contains(normalizedQuery, ignoreCase = true) ||
                assignmentSubject.name.contains(normalizedQuery, ignoreCase = true)

            val matchesMeta = when (metaFilter) {
                AssignmentMetaFilter.ALL     -> true
                AssignmentMetaFilter.PENDING -> a.status == AssignmentStatus.PENDING
                AssignmentMetaFilter.DONE    -> a.status == AssignmentStatus.COMPLETED
                AssignmentMetaFilter.LOW     -> a.priority == Priority.LOW
                AssignmentMetaFilter.MEDIUM  -> a.priority == Priority.MEDIUM
                AssignmentMetaFilter.HIGH    -> a.priority == Priority.HIGH
            }

            val matchesSubject = filterSubjectId == null || a.subjectId == filterSubjectId

            matchesQuery && matchesMeta && matchesSubject
        }
    }

    val sorted = remember(filtered) {
        filtered.sortedWith(
            compareBy<Assignment>(
                {
                    when (it.status) {
                        AssignmentStatus.PENDING -> 0
                        AssignmentStatus.COMPLETED -> 1
                        AssignmentStatus.CANCELLED -> 2
                    }
                },
                { it.dueDate }
            )
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { viewModel.refreshAll() }
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = { showAddDialog = true },
                icon           = { Icon(Icons.Rounded.Add, null) },
                text           = { Text("Add Assignment") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AssignmentFilterControls(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedMetaFilter = metaFilter,
                        onMetaFilterChange = { metaFilter = it },
                        selectedSubjectId = filterSubjectId,
                        subjects = assignmentSubjectOptions,
                        onSubjectFilterChange = { filterSubjectId = it }
                    )
                }

                item {
                    AssignmentStatsRow(assignments = allAssignments)
                }

                if (sorted.isEmpty()) {
                    item {
                        SphereCard(modifier = Modifier.fillMaxWidth()) {
                            val hasFilter = searchQuery.isNotBlank() ||
                                metaFilter != AssignmentMetaFilter.ALL ||
                                filterSubjectId != null

                            EmptyState(
                                icon        = Icons.Rounded.AssignmentTurnedIn,
                                title       = "No Assignments",
                                subtitle    = if (hasFilter)
                                    "No assignments match your search or filters"
                                else
                                    "Tap + to add your first assignment",
                                modifier    = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    items(sorted, key = { it.id }) { assignment ->
                        val subject = resolveAssignmentSubject(assignment, subjects)
                        AssignmentCard(
                            assignment = assignment,
                            subject    = subject,
                            onStatusChange = { status ->
                                viewModel.updateAssignmentStatus(assignment, status)
                            },
                            onEdit   = { editingAssignment = assignment },
                            onDelete = { deleteTarget = assignment }
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

    if (showAddDialog) {
        AddEditAssignmentDialog(
            subjects  = assignmentSubjectOptions,
            existing  = null,
            onDismiss = { showAddDialog = false },
            onSave    = { subjectId, title, desc, dueDate, priority ->
                viewModel.addAssignment(subjectId, title, desc, dueDate, priority)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    if (editingAssignment != null) {
        AddEditAssignmentDialog(
            subjects  = assignmentSubjectOptions,
            existing  = editingAssignment,
            onDismiss = { editingAssignment = null },
            onSave    = { subjectId, title, desc, dueDate, priority ->
                viewModel.updateAssignment(
                    editingAssignment!!.copy(
                        subjectId = subjectId, title = title, description = desc,
                        dueDate = dueDate, priority = priority
                    )
                )
                editingAssignment = null
            }
        )
    }

    if (deleteTarget != null) {
        ConfirmDeleteDialog(
            title    = "Delete Assignment?",
            message  = "\"${deleteTarget!!.title}\" will be permanently removed.",
            onConfirm = {
                viewModel.deleteAssignment(deleteTarget!!)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

private fun resolveAssignmentSubject(assignment: Assignment, subjects: List<Subject>): Subject {
    return subjects.find { it.id == assignment.subjectId }
        ?: if (assignment.subjectId == OTHER_SUBJECT_ID) {
            otherSubject()
        } else {
            Subject(
                id = assignment.subjectId,
                name = "Deleted Subject",
                colorHex = "#6B7280",
                minAttendancePercent = 0f
            )
        }
}

@Composable
private fun AssignmentFilterControls(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedMetaFilter: AssignmentMetaFilter,
    onMetaFilterChange: (AssignmentMetaFilter) -> Unit,
    selectedSubjectId: Long?,
    subjects: List<Subject>,
    onSubjectFilterChange: (Long?) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compactLayout = maxWidth < 430.dp

        if (compactLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchAssignmentsField(query = query, onQueryChange = onQueryChange)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPriorityDropdown(
                        selectedFilter = selectedMetaFilter,
                        onFilterSelected = onMetaFilterChange,
                        modifier = Modifier.weight(1f)
                    )
                    SubjectDropdown(
                        selectedSubjectId = selectedSubjectId,
                        subjects = subjects,
                        onSubjectSelected = onSubjectFilterChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                SearchAssignmentsField(
                    query = query,
                    onQueryChange = onQueryChange,
                    modifier = Modifier.weight(1.3f)
                )
                StatusPriorityDropdown(
                    selectedFilter = selectedMetaFilter,
                    onFilterSelected = onMetaFilterChange,
                    modifier = Modifier.weight(0.9f)
                )
                SubjectDropdown(
                    selectedSubjectId = selectedSubjectId,
                    subjects = subjects,
                    onSubjectSelected = onSubjectFilterChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SearchAssignmentsField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search") },
        placeholder = { Text("Title, details, subject") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun StatusPriorityDropdown(
    selectedFilter: AssignmentMetaFilter,
    onFilterSelected: (AssignmentMetaFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Status / Priority",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedCard(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedFilter.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Open status and priority filters")
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AssignmentMetaFilter.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            expanded = false
                            onFilterSelected(option)
                        },
                        trailingIcon = {
                            if (option == selectedFilter) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectDropdown(
    selectedSubjectId: Long?,
    subjects: List<Subject>,
    onSubjectSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = when (selectedSubjectId) {
        null -> "All subjects"
        else -> subjects.find { it.id == selectedSubjectId }?.name ?: "Unknown"
    }

    Column(modifier = modifier) {
        Text(
            text = "Subject",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedCard(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Open subject filters")
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All subjects") },
                    onClick = {
                        expanded = false
                        onSubjectSelected(null)
                    },
                    trailingIcon = {
                        if (selectedSubjectId == null) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                subjects.forEach { subject ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SubjectColorDot(subject.colorHex)
                                Text(subject.name)
                            }
                        },
                        onClick = {
                            expanded = false
                            onSubjectSelected(subject.id)
                        },
                        trailingIcon = {
                            if (selectedSubjectId == subject.id) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignmentStatsRow(assignments: List<Assignment>) {
    val pending   = assignments.count { it.status == AssignmentStatus.PENDING }
    val completed = assignments.count { it.status == AssignmentStatus.COMPLETED }
    val overdue   = assignments.count {
        it.status == AssignmentStatus.PENDING &&
            runCatching { LocalDate.parse(it.dueDate) }.getOrNull()?.isBefore(LocalDate.now()) == true
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniStatPill("Pending", pending.toString(), Amber500, Modifier.weight(1f))
        MiniStatPill("Completed", completed.toString(), Green500, Modifier.weight(1f))
        MiniStatPill("Overdue", overdue.toString(), Red500, Modifier.weight(1f))
    }
}

@Composable
private fun MiniStatPill(label: String, value: String, color: Color, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall,
             fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun AssignmentCard(
    assignment: Assignment,
    subject: Subject,
    onStatusChange: (AssignmentStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val today   = LocalDate.now()
    val dueDate = remember(assignment.dueDate) {
        try { LocalDate.parse(assignment.dueDate) } catch (e: Exception) { today }
    }
    val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)
    val isCompleted = assignment.status == AssignmentStatus.COMPLETED
    val isCancelled = assignment.status == AssignmentStatus.CANCELLED
    val isOverdue   = assignment.status == AssignmentStatus.PENDING && dueDate.isBefore(today)

    val urgencyColor = when {
        isCompleted             -> Green500
        isCancelled             -> MaterialTheme.colorScheme.onSurfaceVariant
        isOverdue               -> Red600
        daysUntil == 0L         -> Red500
        daysUntil <= 2L         -> Amber500
        else                    -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val dueLabel = when {
        isCompleted     -> "Completed"
        isCancelled     -> "Cancelled"
        isOverdue       -> "Overdue by ${-daysUntil}d"
        daysUntil == 0L -> "Due Today"
        daysUntil == 1L -> "Due Tomorrow"
        else            -> "Due in ${daysUntil}d"
    }

    var menuExpanded by remember { mutableStateOf(false) }

    SphereCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isCompleted) Green500.copy(0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width  = 1.5.dp,
                        color  = if (isCompleted) Green500 else MaterialTheme.colorScheme.outline,
                        shape  = RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        if (!isCancelled) {
                            onStatusChange(
                                if (isCompleted) AssignmentStatus.PENDING
                                else AssignmentStatus.COMPLETED
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Green500)
                }
            }

            // Content
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = assignment.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted || isCancelled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (assignment.description.isNotBlank()) {
                    Text(
                        text = assignment.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectChip(subject)
                    PriorityBadge(assignment.priority)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isOverdue   -> Icons.Rounded.ErrorOutline
                            isCompleted -> Icons.Rounded.CheckCircle
                            else        -> Icons.Rounded.CalendarToday
                        },
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = urgencyColor
                    )
                    Text(
                        text = dueLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = urgencyColor,
                        fontWeight = if (isOverdue || daysUntil <= 1L) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = "· ${assignment.dueDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                }
            }

            // Menu
            Box {
                IconButton(
                    onClick  = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.MoreVert, null,
                         Modifier.size(18.dp),
                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded        = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text     = { Text("Edit", style = MaterialTheme.typography.bodySmall) },
                        onClick  = { menuExpanded = false; onEdit() },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp)) }
                    )
                    if (assignment.status == AssignmentStatus.PENDING) {
                        DropdownMenuItem(
                            text = { Text("Mark Cancelled", style = MaterialTheme.typography.bodySmall) },
                            onClick = { menuExpanded = false; onStatusChange(AssignmentStatus.CANCELLED) },
                            leadingIcon = { Icon(Icons.Rounded.Cancel, null, Modifier.size(16.dp)) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Mark Pending", style = MaterialTheme.typography.bodySmall) },
                            onClick = { menuExpanded = false; onStatusChange(AssignmentStatus.PENDING) },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp)) }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text("Delete", style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuExpanded = false; onDelete() },
                        leadingIcon = {
                            Icon(Icons.Rounded.Delete, null,
                                 Modifier.size(16.dp),
                                 tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssignmentDialog(
    subjects: List<Subject>,
    existing: Assignment?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Priority) -> Unit
) {
    var title       by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var dueDate     by remember { mutableStateOf(existing?.dueDate ?: LocalDate.now().plusDays(7).toString()) }
    var priority    by remember { mutableStateOf(existing?.priority ?: Priority.MEDIUM) }
    var subjectId   by remember { mutableStateOf(existing?.subjectId ?: subjects.firstOrNull()?.id ?: OTHER_SUBJECT_ID) }
    var showDatePicker by remember { mutableStateOf(false) }

    var titleError   by remember { mutableStateOf(false) }
    var dateError    by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }

    val selectedSubject = subjects.find { it.id == subjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (existing == null) "New Assignment" else "Edit Assignment",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                OutlinedTextField(
                    value       = title,
                    onValueChange = { title = it; titleError = false },
                    label       = { Text("Title") },
                    isError     = titleError,
                    supportingText = if (titleError) {{ Text("Title is required") }} else null,
                    singleLine  = true,
                    modifier    = Modifier.fillMaxWidth(),
                    shape       = RoundedCornerShape(12.dp)
                )

                // Description
                OutlinedTextField(
                    value       = description,
                    onValueChange = { description = it },
                    label       = { Text("Description (optional)") },
                    minLines    = 2,
                    maxLines    = 4,
                    modifier    = Modifier.fillMaxWidth(),
                    shape       = RoundedCornerShape(12.dp)
                )

                // Subject dropdown
                Text("Subject", style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    OutlinedCard(
                        onClick = { subjectMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedSubject != null) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SubjectColorDot(selectedSubject.colorHex)
                                    Text(selectedSubject.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                Text("Select subject", style = MaterialTheme.typography.bodyMedium,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(20.dp))
                        }
                    }
                    DropdownMenu(
                        expanded         = subjectMenuExpanded,
                        onDismissRequest = { subjectMenuExpanded = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SubjectColorDot(subject.colorHex)
                                        Text(subject.name, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = { subjectId = subject.id; subjectMenuExpanded = false },
                                trailingIcon = if (subject.id == subjectId) {
                                    { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp),
                                           tint = MaterialTheme.colorScheme.primary) }
                                } else null
                            )
                        }
                    }
                }

                // Due date
                Text("Due Date", style = MaterialTheme.typography.labelMedium,
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
                                    text = dueDate.toUiDateLabel(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = dueDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Rounded.EditCalendar,
                            contentDescription = "Select due date"
                        )
                    }
                }
                if (dateError) {
                    Text(
                        "Select a valid date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Priority
                Text("Priority", style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.values().forEach { p ->
                        val isSelected = priority == p
                        val pColor = when (p) {
                            Priority.LOW    -> Teal500
                            Priority.MEDIUM -> Amber500
                            Priority.HIGH   -> Red500
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick  = { priority = p },
                            label    = {
                                Text(p.name.lowercase().replaceFirstChar { it.uppercase() },
                                     style = MaterialTheme.typography.labelSmall)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pColor.copy(0.15f),
                                selectedLabelColor     = pColor
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
                    titleError = title.isBlank()
                    dateError  = runCatching { LocalDate.parse(dueDate) }.isFailure
                    if (!titleError && !dateError) {
                        onSave(subjectId, title.trim(), description.trim(), dueDate, priority)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (existing == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val initialDate = dueDate.toLocalDateOrNull() ?: LocalDate.now().plusDays(7)
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toEpochMillis())

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = pickerState.selectedDateMillis ?: initialDate.toEpochMillis()
                        dueDate = millis.toIsoDateString()
                        dateError = false
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

private fun String.toUiDateLabel(): String {
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
