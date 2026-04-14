package com.studysphere.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

const val OTHER_SUBJECT_ID: Long = -1L
const val OTHER_SUBJECT_NAME: String = "Other"
const val OTHER_SUBJECT_COLOR_HEX: String = "#334155"

fun otherSubject(): Subject = Subject(
    id = OTHER_SUBJECT_ID,
    name = OTHER_SUBJECT_NAME,
    colorHex = OTHER_SUBJECT_COLOR_HEX,
    minAttendancePercent = 0f
)

// ─── Subject ─────────────────────────────────────────────────────────────────

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6366F1", // default indigo
    val minAttendancePercent: Float = 75f
)

// ─── Lecture ─────────────────────────────────────────────────────────────────

@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int,          // DayOfWeek.value (1=Mon … 7=Sun)
    val startTimeHour: Int,
    val startTimeMinute: Int,
    val endTimeHour: Int,
    val endTimeMinute: Int,
    val room: String = ""
)

// ─── AttendanceRecord ─────────────────────────────────────────────────────────

enum class AttendanceStatus { PRESENT, ABSENT, CANCELLED }

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lectureId: Long,
    val subjectId: Long,
    val date: String,            // ISO-8601 "yyyy-MM-dd"
    val status: AttendanceStatus = AttendanceStatus.PRESENT
)

// ─── Assignment ───────────────────────────────────────────────────────────────

enum class Priority { LOW, MEDIUM, HIGH }
enum class AssignmentStatus { PENDING, COMPLETED, CANCELLED }

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val description: String = "",
    val dueDate: String,         // ISO-8601 "yyyy-MM-dd"
    val priority: Priority = Priority.MEDIUM,
    val status: AssignmentStatus = AssignmentStatus.PENDING,
    val createdAt: String        // ISO-8601 "yyyy-MM-dd"
)

// ─── View / computed models ───────────────────────────────────────────────────

data class SubjectAttendanceSummary(
    val subject: Subject,
    val totalClasses: Int,
    val attended: Int,
    val cancelled: Int,
    val percentage: Float,
    val canSkip: Int,            // how many more can be skipped and still meet minimum
    val mustAttend: Int,         // classes needed to recover if below threshold
    val riskLevel: RiskLevel
)

enum class RiskLevel { SAFE, WARNING, DANGER, CRITICAL }

data class TodayLecture(
    val lecture: Lecture,
    val subject: Subject,
    val attendanceRecord: AttendanceRecord? = null
)

data class UpcomingAssignment(
    val assignment: Assignment,
    val subject: Subject,
    val daysUntilDue: Long
)

// ─── Extensions ──────────────────────────────────────────────────────────────

fun Lecture.startTime(): LocalTime = LocalTime.of(startTimeHour, startTimeMinute)
fun Lecture.endTime(): LocalTime = LocalTime.of(endTimeHour, endTimeMinute)
fun Lecture.dayOfWeekEnum(): DayOfWeek = DayOfWeek.of(dayOfWeek)

fun SubjectAttendanceSummary.riskColor(): String = when (riskLevel) {
    RiskLevel.SAFE     -> "#22C55E"
    RiskLevel.WARNING  -> "#F59E0B"
    RiskLevel.DANGER   -> "#EF4444"
    RiskLevel.CRITICAL -> "#DC2626"
}
