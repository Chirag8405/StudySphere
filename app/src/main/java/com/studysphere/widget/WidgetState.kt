package com.studysphere.widget

import com.studysphere.data.models.AttendanceStatus
import com.studysphere.data.models.Priority
import kotlinx.serialization.Serializable

@Serializable
data class WidgetLecture(
    val lectureId: Long,
    val subjectId: Long,
    val subjectName: String,
    val timeRange: String,       // "09:30 – 10:30"
    val room: String,
    val status: String?          // null = unmarked; stored as String to avoid enum serialization issues
) {
    fun attendanceStatus(): AttendanceStatus? =
        status?.let { runCatching { AttendanceStatus.valueOf(it) }.getOrNull() }
}

@Serializable
data class WidgetAssignment(
    val id: Long,
    val title: String,
    val dueLabel: String,
    val isOverdue: Boolean,
    val priority: String         // stored as String (Priority.name)
) {
    fun priorityEnum(): Priority =
        runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.MEDIUM)
}

@Serializable
data class WidgetData(
    val lectures: List<WidgetLecture> = emptyList(),
    val assignments: List<WidgetAssignment> = emptyList()
)
