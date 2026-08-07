package com.studysphere.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
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
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── DataStore singleton ──────────────────────────────────────────────────────

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// ─── DataStore keys ───────────────────────────────────────────────────────────

private val DARK_MODE_KEY       = booleanPreferencesKey("dark_mode")
// Tri-state theme key: "system" | "light" | "dark"
private val THEME_MODE_KEY      = stringPreferencesKey("theme_mode")
private val KEY_WORKING_DAYS    = intPreferencesKey("working_days_per_week")
private val KEY_DEADLINE_WINDOW = intPreferencesKey("deadline_window_days")
private val KEY_LAST_BACKUP     = stringPreferencesKey("last_backup_at")

// ─── MainViewModel ────────────────────────────────────────────────────────────

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudySphereDatabase.getInstance(application)
    private val repository = StudySphereRepository(
        db.subjectDao(), db.lectureDao(), db.attendanceDao(), db.assignmentDao()
    )

    // Direct DAO references used by import coroutines so they can call
    // findExistingLecture() and insertIfNotExists() without going through
    // the repository's higher-level methods.
    private val subjectDao    = db.subjectDao()
    private val lectureDao    = db.lectureDao()
    private val attendanceDao = db.attendanceDao()
    private val assignmentDao = db.assignmentDao()

    private val ds = application.dataStore

    // ── Theme ─────────────────────────────────────────────────────────────────

    /**
     * "system" = follow system, "light" = always light, "dark" = always dark.
     * Exposed to SettingsScreen for the segmented button.
     */
    val themeMode: StateFlow<String> = ds.data
        .map { prefs ->
            prefs[THEME_MODE_KEY] ?: run {
                // Migrate legacy boolean key if present
                when (prefs[DARK_MODE_KEY]) {
                    true  -> "dark"
                    false -> "light"
                    null  -> "system"
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    /**
     * Nullable Boolean for MainActivity: null = system default,
     * true = dark, false = light.
     */
    val isDarkModeNullable: StateFlow<Boolean?> = themeMode
        .map { mode ->
            when (mode) {
                "dark"  -> true
                "light" -> false
                else    -> null   // "system" → let Theme use isSystemInDarkTheme()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Kept for backward compat; false when mode is "system".
     */
    val isDarkMode: StateFlow<Boolean> = themeMode
        .map { mode -> mode == "dark" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            ds.edit { prefs ->
                prefs[THEME_MODE_KEY] = mode
                when (mode) {
                    "dark"  -> prefs[DARK_MODE_KEY] = true
                    "light" -> prefs[DARK_MODE_KEY] = false
                    else    -> prefs.remove(DARK_MODE_KEY)
                }
            }
        }
    }

    /** Legacy toggle kept for safety; not used by the new Settings UI. */
    fun toggleTheme() {
        viewModelScope.launch {
            val next = if (themeMode.value == "dark") "light" else "dark"
            setThemeMode(next)
        }
    }

    // ── Working days per week ─────────────────────────────────────────────────

    val workingDaysPerWeek: StateFlow<Int> = ds.data
        .map { it[KEY_WORKING_DAYS] ?: 5 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    fun setWorkingDays(days: Int) {
        viewModelScope.launch { ds.edit { prefs -> prefs[KEY_WORKING_DAYS] = days } }
    }

    // ── Upcoming deadline window ──────────────────────────────────────────────

    val deadlineWindowDays: StateFlow<Int> = ds.data
        .map { it[KEY_DEADLINE_WINDOW] ?: 3 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)

    fun setDeadlineWindow(days: Int) {
        viewModelScope.launch { ds.edit { prefs -> prefs[KEY_DEADLINE_WINDOW] = days } }
    }

    // ── Last backup timestamp ─────────────────────────────────────────────────

    val lastBackupAt: StateFlow<String> = ds.data
        .map { it[KEY_LAST_BACKUP] ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private fun setLastBackupTimestamp() {
        viewModelScope.launch {
            ds.edit { prefs -> prefs[KEY_LAST_BACKUP] = Instant.now().toString() }
        }
    }

    // ── Subjects ──────────────────────────────────────────────────────────────

    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSubject(name: String, colorHex: String, minAttendance: Float) {
        viewModelScope.launch {
            repository.insertSubject(
                Subject(name = name, colorHex = colorHex, minAttendancePercent = minAttendance)
            )
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

    private val _summaryRefreshTrigger = MutableStateFlow(0L)
    private val _isRefreshing          = MutableStateFlow(false)
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

    /**
     * Feature 10: upcomingAssignments uses deadlineWindowDays dynamically.
     * The "Due Soon" stat card on Dashboard uses this to count assignments due
     * within the configured window.
     */
    val upcomingAssignments: StateFlow<List<UpcomingAssignment>> =
        combine(subjects, deadlineWindowDays) { subjectList, _ -> subjectList }
            .flatMapLatest { subjectList ->
                repository.getUpcomingAssignments(subjectList)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // ── Feature 5: New Semester Reset ─────────────────────────────────────────

    fun clearSemesterData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearSemesterData()
            withContext(Dispatchers.Main) { refreshAll() }
        }
    }

    // ── Feature 6: Clear All Assignments ──────────────────────────────────────

    fun clearAllAssignments() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllAssignments()
            withContext(Dispatchers.Main) { refreshAll() }
        }
    }

    // ── Feature 13: Reset All Data ────────────────────────────────────────────

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            db.clearAllTables()
            ds.edit { prefs ->
                prefs.remove(KEY_LAST_BACKUP)
                // Keep user preferences (theme, working days, deadline window)
            }
            withContext(Dispatchers.Main) { refreshAll() }
        }
    }

    // ── Features 1 & 3: Export (Timetable / Full Backup) ─────────────────────

    /**
     * Exports the timetable (subjects + lectures) as JSON to the given SAF URI.
     * Uses one-shot suspend DAO queries — never reads from StateFlow.value.
     */
    suspend fun exportTimetable(uri: Uri, context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val subjects = subjectDao.getAllSubjectsList()
                val lectures = lectureDao.getAllLecturesList()
                val json     = buildTimetableJson(subjects, lectures)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open output stream")
                setLastBackupTimestamp()
            }
        }

    /**
     * Exports the full backup (subjects + lectures + attendance + assignments)
     * as JSON to the given SAF URI.
     * Uses one-shot suspend DAO queries — never reads from StateFlow.value.
     */
    suspend fun exportFullBackup(uri: Uri, context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val subjects    = subjectDao.getAllSubjectsList()
                val lectures    = lectureDao.getAllLecturesList()
                val attendance  = attendanceDao.getAllRecordsList()
                val assignments = assignmentDao.getAllAssignmentsList()
                val json        = buildFullBackupJson(subjects, lectures, attendance, assignments)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open output stream")
                setLastBackupTimestamp()
            }
        }

    // ── Features 2 & 4: Import (Timetable / Full Backup) ─────────────────────

    /**
     * Imports a timetable JSON (subjects + lectures) from the given SAF URI.
     *
     * Bug 1 fix: lecture import now checks for an existing slot with the same
     * (subjectId, dayOfWeek, startTimeHour, startTimeMinute) before inserting,
     * so re-importing the same file does not create duplicate lecture rows.
     *
     * Returns Result<ImportResult> with actual counts of newly inserted rows
     * (duplicates are skipped and not counted).
     */
    suspend fun importTimetable(uri: Uri, context: Context): Result<ImportResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: error("Could not open file")
                val root = JSONObject(text)
                if (root.optInt("version", 0) != 1) error("version_mismatch")

                val subjectsJson = root.getJSONArray("subjects")
                val lecturesJson = root.getJSONArray("lectures")

                // ── Subject import (dedup by name) ───────────────────────────

                val existingSubjects = subjectDao.getAllSubjectsList()
                // Maps JSON subject id → DB subject id (new or existing)
                val subjectIdMap  = mutableMapOf<Long, Long>()
                var subjectsAdded = 0

                for (i in 0 until subjectsJson.length()) {
                    val s      = subjectsJson.getJSONObject(i)
                    val oldId  = s.getLong("id")
                    val name   = s.getString("name")
                    val exists = existingSubjects.find { it.name == name }
                    if (exists != null) {
                        subjectIdMap[oldId] = exists.id
                    } else {
                        val newId = subjectDao.insertSubject(
                            Subject(
                                name                 = name,
                                colorHex             = s.optString("colorHex", "#6366F1"),
                                minAttendancePercent = s.optDouble("minAttendancePercent", 75.0).toFloat()
                            )
                        )
                        subjectIdMap[oldId] = newId
                        subjectsAdded++
                    }
                }

                // ── Lecture import (Bug 1 fix: dedup by slot identity) ────────

                var lecturesAdded = 0

                for (i in 0 until lecturesJson.length()) {
                    val l              = lecturesJson.getJSONObject(i)
                    val remappedSubId  = subjectIdMap[l.getLong("subjectId")] ?: continue
                    val startHour      = l.getInt("startTimeHour")
                    val startMinute    = l.getInt("startTimeMinute")
                    val dayOfWeek      = l.getInt("dayOfWeek")

                    val existing = lectureDao.findExistingLecture(
                        subjectId      = remappedSubId,
                        dayOfWeek      = dayOfWeek,
                        startTimeHour  = startHour,
                        startTimeMinute = startMinute
                    )

                    if (existing != null) continue  // duplicate — skip silently

                    lectureDao.insertLecture(
                        Lecture(
                            id              = 0L,
                            subjectId       = remappedSubId,
                            dayOfWeek       = dayOfWeek,
                            startTimeHour   = startHour,
                            startTimeMinute = startMinute,
                            endTimeHour     = l.getInt("endTimeHour"),
                            endTimeMinute   = l.getInt("endTimeMinute"),
                            room            = l.optString("room", "")
                        )
                    )
                    lecturesAdded++
                }

                withContext(Dispatchers.Main) { refreshAll() }
                ImportResult(subjectsAdded, lecturesAdded, 0, 0)
            }
        }

    /**
     * Imports a full backup JSON from the given SAF URI.
     *
     * Bug fixes applied:
     *  • Bug 1 (duplicate lectures): lecture loop calls findExistingLecture()
     *    and builds lectureIdMap even for skipped duplicates so attendance
     *    records can still resolve their lectureId.
     *  • Bug 2A (OTHER_SUBJECT_ID dropped): subjectIdMap is pre-seeded with
     *    -1L → -1L so "Other / General" assignments are not silently dropped.
     *  • Bug 2B (PK conflict on re-import): Assignment is constructed with
     *    id = 0L to force Room's autoincrement instead of re-using the backup id.
     *  • Attendance dedup: uses insertIfNotExists() (IGNORE strategy) so
     *    re-importing does not duplicate or overwrite existing records; only
     *    newly inserted rows are counted.
     *
     * Returns Result<ImportResult> with actual counts of newly inserted rows.
     */
    suspend fun importFullBackup(uri: Uri, context: Context): Result<ImportResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: error("Could not open file")
                val root = JSONObject(text)
                if (root.optInt("version", 0) != 1) error("version_mismatch")

                val subjectsJson    = root.getJSONArray("subjects")
                val lecturesJson    = root.getJSONArray("lectures")
                val attendanceJson  = root.optJSONArray("attendanceRecords") ?: JSONArray()
                val assignmentsJson = root.optJSONArray("assignments") ?: JSONArray()

                // ── Subject import (dedup by name) ───────────────────────────

                val existingSubjects = subjectDao.getAllSubjectsList()

                // Bug 2A fix: pre-seed OTHER_SUBJECT_ID so "Other" assignments
                // are never dropped by the ?: continue guard below.
                val subjectIdMap  = mutableMapOf<Long, Long>()
                subjectIdMap[OTHER_SUBJECT_ID] = OTHER_SUBJECT_ID

                var subjectsAdded = 0

                for (i in 0 until subjectsJson.length()) {
                    val s      = subjectsJson.getJSONObject(i)
                    val oldId  = s.getLong("id")
                    val name   = s.getString("name")
                    val exists = existingSubjects.find { it.name == name }
                    if (exists != null) {
                        subjectIdMap[oldId] = exists.id
                    } else {
                        val newId = subjectDao.insertSubject(
                            Subject(
                                name                 = name,
                                colorHex             = s.optString("colorHex", "#6366F1"),
                                minAttendancePercent = s.optDouble("minAttendancePercent", 75.0).toFloat()
                            )
                        )
                        subjectIdMap[oldId] = newId
                        subjectsAdded++
                    }
                }

                // ── Lecture import (Bug 1 fix: dedup by slot identity) ────────

                // lectureIdMap is populated even for existing (skipped) lectures
                // so the attendance import can resolve old lecture ids correctly.
                val lectureIdMap  = mutableMapOf<Long, Long>()
                var lecturesAdded = 0

                for (i in 0 until lecturesJson.length()) {
                    val l             = lecturesJson.getJSONObject(i)
                    val oldLecId      = l.getLong("id")
                    val remappedSubId = subjectIdMap[l.getLong("subjectId")] ?: continue
                    val startHour     = l.getInt("startTimeHour")
                    val startMinute   = l.getInt("startTimeMinute")
                    val dayOfWeek     = l.getInt("dayOfWeek")

                    val existing = lectureDao.findExistingLecture(
                        subjectId       = remappedSubId,
                        dayOfWeek       = dayOfWeek,
                        startTimeHour   = startHour,
                        startTimeMinute = startMinute
                    )

                    if (existing != null) {
                        // Still map the old id → existing id so attendance
                        // records that reference this lecture resolve correctly.
                        lectureIdMap[oldLecId] = existing.id
                        continue
                    }

                    val newLecId = lectureDao.insertLecture(
                        Lecture(
                            id              = 0L,
                            subjectId       = remappedSubId,
                            dayOfWeek       = dayOfWeek,
                            startTimeHour   = startHour,
                            startTimeMinute = startMinute,
                            endTimeHour     = l.getInt("endTimeHour"),
                            endTimeMinute   = l.getInt("endTimeMinute"),
                            room            = l.optString("room", "")
                        )
                    )
                    lectureIdMap[oldLecId] = newLecId
                    lecturesAdded++
                }

                // ── Attendance import (dedup via IGNORE strategy) ─────────────

                var recordsAdded = 0

                for (i in 0 until attendanceJson.length()) {
                    val r             = attendanceJson.getJSONObject(i)
                    val remappedSubId = subjectIdMap[r.getLong("subjectId")] ?: continue
                    val remappedLecId = lectureIdMap[r.getLong("lectureId")]  ?: continue
                    val status        = runCatching {
                        AttendanceStatus.valueOf(r.getString("status"))
                    }.getOrDefault(AttendanceStatus.PRESENT)

                    // insertIfNotExists returns -1L when the IGNORE conflict
                    // strategy fires (i.e. the row already exists).
                    val newRowId = attendanceDao.insertIfNotExists(
                        AttendanceRecord(
                            id        = 0L,
                            lectureId = remappedLecId,
                            subjectId = remappedSubId,
                            date      = r.getString("date"),
                            status    = status
                        )
                    )
                    if (newRowId != -1L) recordsAdded++
                }

                // ── Assignment import (Bug 2B fix: id = 0L, OTHER passthrough) ─

                var assignmentsAdded = 0

                for (i in 0 until assignmentsJson.length()) {
                    val a             = assignmentsJson.getJSONObject(i)
                    // Bug 2A passthrough: OTHER_SUBJECT_ID (-1L) is already in
                    // subjectIdMap so assignments with no real subject are kept.
                    val remappedSubId = subjectIdMap[a.getLong("subjectId")] ?: continue
                    val priority      = runCatching {
                        Priority.valueOf(a.getString("priority"))
                    }.getOrDefault(Priority.MEDIUM)
                    val status        = runCatching {
                        AssignmentStatus.valueOf(a.getString("status"))
                    }.getOrDefault(AssignmentStatus.PENDING)

                    // Bug 2B fix: always set id = 0L to let Room autoincrement
                    // generate a fresh primary key instead of re-using the
                    // backed-up id, which would cause PK conflicts on re-import.
                    assignmentDao.insertAssignment(
                        Assignment(
                            id          = 0L,
                            subjectId   = remappedSubId,
                            title       = a.getString("title"),
                            description = a.optString("description", ""),
                            dueDate     = a.getString("dueDate"),
                            priority    = priority,
                            status      = status,
                            createdAt   = a.optString("createdAt", LocalDate.now().toString())
                        )
                    )
                    assignmentsAdded++
                }

                withContext(Dispatchers.Main) { refreshAll() }
                ImportResult(subjectsAdded, lecturesAdded, recordsAdded, assignmentsAdded)
            }
        }

    // ── JSON builders ─────────────────────────────────────────────────────────

    private fun buildTimetableJson(subjects: List<Subject>, lectures: List<Lecture>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", Instant.now().toString())

        val subArr = JSONArray()
        subjects.forEach { s ->
            subArr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("colorHex", s.colorHex)
                put("minAttendancePercent", s.minAttendancePercent.toDouble())
            })
        }
        root.put("subjects", subArr)

        val lecArr = JSONArray()
        lectures.forEach { l ->
            lecArr.put(JSONObject().apply {
                put("id", l.id)
                put("subjectId", l.subjectId)
                put("dayOfWeek", l.dayOfWeek)
                put("startTimeHour", l.startTimeHour)
                put("startTimeMinute", l.startTimeMinute)
                put("endTimeHour", l.endTimeHour)
                put("endTimeMinute", l.endTimeMinute)
                put("room", l.room)
            })
        }
        root.put("lectures", lecArr)
        return root.toString(2)
    }

    private fun buildFullBackupJson(
        subjects: List<Subject>,
        lectures: List<Lecture>,
        attendance: List<AttendanceRecord>,
        assignments: List<Assignment>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", Instant.now().toString())

        val subArr = JSONArray()
        subjects.forEach { s ->
            subArr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("colorHex", s.colorHex)
                put("minAttendancePercent", s.minAttendancePercent.toDouble())
            })
        }
        root.put("subjects", subArr)

        val lecArr = JSONArray()
        lectures.forEach { l ->
            lecArr.put(JSONObject().apply {
                put("id", l.id)
                put("subjectId", l.subjectId)
                put("dayOfWeek", l.dayOfWeek)
                put("startTimeHour", l.startTimeHour)
                put("startTimeMinute", l.startTimeMinute)
                put("endTimeHour", l.endTimeHour)
                put("endTimeMinute", l.endTimeMinute)
                put("room", l.room)
            })
        }
        root.put("lectures", lecArr)

        val attArr = JSONArray()
        attendance.forEach { r ->
            attArr.put(JSONObject().apply {
                put("id", r.id)
                put("lectureId", r.lectureId)
                put("subjectId", r.subjectId)
                put("date", r.date)
                put("status", r.status.name)
            })
        }
        root.put("attendanceRecords", attArr)

        val assArr = JSONArray()
        assignments.forEach { a ->
            assArr.put(JSONObject().apply {
                put("id", a.id)
                put("subjectId", a.subjectId)
                put("title", a.title)
                put("description", a.description)
                put("dueDate", a.dueDate)
                put("priority", a.priority.name)
                put("status", a.status.name)
                put("createdAt", a.createdAt)
            })
        }
        root.put("assignments", assArr)
        return root.toString(2)
    }
}

// ─── Import result data class ─────────────────────────────────────────────────

data class ImportResult(
    val subjectsAdded: Int,
    val lecturesAdded: Int,
    val recordsAdded: Int,
    val assignmentsAdded: Int
)
