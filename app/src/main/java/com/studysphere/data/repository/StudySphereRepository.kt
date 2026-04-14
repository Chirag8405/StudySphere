package com.studysphere.data.repository

import com.studysphere.data.db.*
import com.studysphere.data.models.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

class StudySphereRepository(
    private val subjectDao: SubjectDao,
    private val lectureDao: LectureDao,
    private val attendanceDao: AttendanceDao,
    private val assignmentDao: AssignmentDao
) {

    // ── Subjects ─────────────────────────────────────────────────────────────

    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun insertSubject(subject: Subject) = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) {
        val id = subject.id
        attendanceDao.deleteRecordsBySubject(id)
        assignmentDao.deleteAssignmentsBySubject(id)
        lectureDao.deleteLecturesBySubject(id)
        subjectDao.deleteSubject(subject)
    }

    // ── Lectures ─────────────────────────────────────────────────────────────

    val allLectures: Flow<List<Lecture>> = lectureDao.getAllLectures()

    fun getLecturesByDay(dayOfWeek: Int) = lectureDao.getLecturesByDay(dayOfWeek)
    fun getLecturesBySubject(subjectId: Long) = lectureDao.getLecturesBySubject(subjectId)

    suspend fun insertLecture(lecture: Lecture) = lectureDao.insertLecture(lecture)
    suspend fun updateLecture(lecture: Lecture) = lectureDao.updateLecture(lecture)
    suspend fun deleteLecture(lecture: Lecture) {
        attendanceDao.deleteRecordsByLecture(lecture.id)
        lectureDao.deleteLecture(lecture)
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    fun getRecordsByDate(date: String) = attendanceDao.getRecordsByDate(date)
    fun getRecordsBySubject(subjectId: Long) = attendanceDao.getRecordsBySubject(subjectId)

    suspend fun markAttendance(
        lectureId: Long,
        subjectId: Long,
        date: String,
        status: AttendanceStatus
    ) {
        val existing = attendanceDao.getRecordByLectureAndDate(lectureId, date)
        if (existing != null) {
            attendanceDao.updateRecord(existing.copy(status = status))
        } else {
            attendanceDao.insertRecord(
                AttendanceRecord(
                    lectureId = lectureId,
                    subjectId = subjectId,
                    date = date,
                    status = status
                )
            )
        }
    }

    suspend fun getRecordByLectureAndDate(lectureId: Long, date: String) =
        attendanceDao.getRecordByLectureAndDate(lectureId, date)

    // ── Assignments ───────────────────────────────────────────────────────────

    val allAssignments: Flow<List<Assignment>> = assignmentDao.getAllAssignments()
    val pendingAssignments: Flow<List<Assignment>> = assignmentDao.getPendingAssignments()

    fun getAssignmentsBySubject(subjectId: Long) = assignmentDao.getAssignmentsBySubject(subjectId)

    suspend fun insertAssignment(assignment: Assignment) = assignmentDao.insertAssignment(assignment)
    suspend fun updateAssignment(assignment: Assignment) = assignmentDao.updateAssignment(assignment)
    suspend fun deleteAssignment(assignment: Assignment) = assignmentDao.deleteAssignment(assignment)

    // ── Attendance Intelligence ───────────────────────────────────────────────

    suspend fun getSubjectAttendanceSummary(subject: Subject): SubjectAttendanceSummary {
        val present = attendanceDao.getPresentCount(subject.id)
        val total   = attendanceDao.getTotalNonCancelledCount(subject.id)
        val cancelled = attendanceDao.getCancelledCount(subject.id)

        val percentage = if (total == 0) 100f else (present.toFloat() / total.toFloat()) * 100f
        val minPct = subject.minAttendancePercent / 100f

        // How many can be skipped while staying above threshold?
        // present / (total + extra) >= minPct => extra <= present/minPct - total
        val canSkip = if (total == 0) 0
        else max(0, ((present / minPct) - total).toInt())

        // How many consecutive classes must be attended to recover?
        // (present + needed) / (total + needed) >= minPct
        // present + needed >= minPct * total + minPct * needed
        // needed * (1 - minPct) >= minPct * total - present
        // needed >= (minPct * total - present) / (1 - minPct)
        val mustAttend = if (percentage >= subject.minAttendancePercent) 0
        else {
            val numerator = minPct * total - present
            val denominator = 1f - minPct
            if (denominator <= 0f) Int.MAX_VALUE
            else ceil(numerator / denominator).toInt()
        }

        val riskLevel = when {
            percentage >= subject.minAttendancePercent + 15 -> RiskLevel.SAFE
            percentage >= subject.minAttendancePercent      -> RiskLevel.WARNING
            percentage >= subject.minAttendancePercent - 10 -> RiskLevel.DANGER
            else                                            -> RiskLevel.CRITICAL
        }

        return SubjectAttendanceSummary(
            subject     = subject,
            totalClasses = total,
            attended    = present,
            cancelled   = cancelled,
            percentage  = percentage,
            canSkip     = canSkip,
            mustAttend  = mustAttend,
            riskLevel   = riskLevel
        )
    }

    // ── Today Lectures ────────────────────────────────────────────────────────

    fun getTodayLectures(subjects: List<Subject>): Flow<List<TodayLecture>> {
        val today = LocalDate.now()
        val todayDow = today.dayOfWeek.value
        val todayStr = today.toString()

        return lectureDao.getLecturesByDay(todayDow).combine(
            attendanceDao.getRecordsByDate(todayStr)
        ) { lectures, records ->
            lectures.mapNotNull { lecture ->
                val subject = subjects.find { it.id == lecture.subjectId } ?: return@mapNotNull null
                val record = records.find { it.lectureId == lecture.id }
                TodayLecture(lecture, subject, record)
            }.sortedBy { it.lecture.startTimeHour * 60 + it.lecture.startTimeMinute }
        }
    }

    // ── Upcoming Assignments ──────────────────────────────────────────────────

    fun getUpcomingAssignments(subjects: List<Subject>): Flow<List<UpcomingAssignment>> {
        val today = LocalDate.now()
        return pendingAssignments.map { assignments ->
            assignments.mapNotNull { assignment ->
                val subject = subjects.find { it.id == assignment.subjectId }
                    ?: if (assignment.subjectId == OTHER_SUBJECT_ID) otherSubject() else null
                    ?: return@mapNotNull null
                val dueDate = LocalDate.parse(assignment.dueDate)
                val days = ChronoUnit.DAYS.between(today, dueDate)
                UpcomingAssignment(assignment, subject, days)
            }.sortedBy { it.daysUntilDue }
        }
    }
}
