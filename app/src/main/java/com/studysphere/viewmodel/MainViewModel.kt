package com.studysphere.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.*
import com.studysphere.data.db.StudySphereDatabase
import com.studysphere.data.models.*
import com.studysphere.data.repository.StudySphereRepository
import com.studysphere.ui.theme.SubjectColors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import android.content.Context

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudySphereDatabase.getInstance(application)
    private val repository = StudySphereRepository(
        db.subjectDao(), db.lectureDao(), db.attendanceDao(), db.assignmentDao()
    )

    // ── Theme ─────────────────────────────────────────────────────────────────

    private val initialDarkMode: Boolean = runBlocking {
        runCatching { application.dataStore.data.first()[DARK_MODE_KEY] ?: false }
            .getOrDefault(false)
    }

    val isDarkMode: StateFlow<Boolean> = application.dataStore.data
        .map { it[DARK_MODE_KEY] ?: initialDarkMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialDarkMode)

    fun toggleTheme() {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[DARK_MODE_KEY] = !(prefs[DARK_MODE_KEY] ?: false)
            }
        }
    }

    // ── Subjects ──────────────────────────────────────────────────────────────

    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSubject(name: String, colorHex: String, minAttendance: Float) {
        viewModelScope.launch {
            repository.insertSubject(Subject(name = name, colorHex = colorHex, minAttendancePercent = minAttendance))
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch { repository.updateSubject(subject) }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch { repository.deleteSubject(subject) }
    }

    fun nextSubjectColor(existingCount: Int): String =
        SubjectColors[existingCount % SubjectColors.size]

    // ── Lectures ──────────────────────────────────────────────────────────────

    val allLectures: StateFlow<List<Lecture>> = repository.allLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getLecturesBySubject(subjectId: Long) = repository.getLecturesBySubject(subjectId)

    fun addLecture(
        subjectId: Long, dayOfWeek: Int,
        startH: Int, startM: Int, endH: Int, endM: Int, room: String
    ) {
        viewModelScope.launch {
            repository.insertLecture(
                Lecture(
                    subjectId = subjectId, dayOfWeek = dayOfWeek,
                    startTimeHour = startH, startTimeMinute = startM,
                    endTimeHour = endH, endTimeMinute = endM, room = room
                )
            )
        }
    }

    fun deleteLecture(lecture: Lecture) {
        viewModelScope.launch { repository.deleteLecture(lecture) }
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    fun markAttendance(lectureId: Long, subjectId: Long, date: String, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.markAttendance(lectureId, subjectId, date, status)
        }
    }

    fun getRecordsBySubject(subjectId: Long) = repository.getRecordsBySubject(subjectId)

    // ── Attendance Summaries ──────────────────────────────────────────────────

    // Refresh summaries when records change
    private val _summaryRefreshTrigger = MutableStateFlow(0L)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val attendanceSummariesRefreshed: StateFlow<List<SubjectAttendanceSummary>> =
        combine(subjects, _summaryRefreshTrigger) { subs, _ -> subs }
            .flatMapLatest { subjectList ->
                if (subjectList.isEmpty()) flowOf(emptyList())
                else flow {
                    emit(subjectList.map { repository.getSubjectAttendanceSummary(it) })
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshSummaries() {
        _summaryRefreshTrigger.value = System.currentTimeMillis()
    }

    fun refreshAll() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshSummaries()
            delay(350)
            _isRefreshing.value = false
        }
    }

    // ── Today's Lectures ──────────────────────────────────────────────────────

    val todayLectures: StateFlow<List<TodayLecture>> =
        subjects.flatMapLatest { subjectList ->
            repository.getTodayLectures(subjectList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Assignments ───────────────────────────────────────────────────────────

    val allAssignments: StateFlow<List<Assignment>> = repository.allAssignments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingAssignments: StateFlow<List<UpcomingAssignment>> =
        subjects.flatMapLatest { subjectList ->
            repository.getUpcomingAssignments(subjectList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addAssignment(
        subjectId: Long, title: String, description: String,
        dueDate: String, priority: Priority
    ) {
        viewModelScope.launch {
            repository.insertAssignment(
                Assignment(
                    subjectId = subjectId, title = title, description = description,
                    dueDate = dueDate, priority = priority,
                    status = AssignmentStatus.PENDING,
                    createdAt = LocalDate.now().toString()
                )
            )
        }
    }

    fun updateAssignmentStatus(assignment: Assignment, status: AssignmentStatus) {
        viewModelScope.launch {
            repository.updateAssignment(assignment.copy(status = status))
        }
    }

    fun updateAssignment(assignment: Assignment) {
        viewModelScope.launch { repository.updateAssignment(assignment) }
    }

    fun deleteAssignment(assignment: Assignment) {
        viewModelScope.launch { repository.deleteAssignment(assignment) }
    }

    fun getAssignmentsBySubject(subjectId: Long) = repository.getAssignmentsBySubject(subjectId)

    // ── Quick Attendance Mark (from today's list) ──────────────────────────────

    fun quickMarkToday(lectureId: Long, subjectId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.markAttendance(lectureId, subjectId, LocalDate.now().toString(), status)
            refreshSummaries()
        }
    }
}
