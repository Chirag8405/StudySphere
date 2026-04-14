package com.studysphere.data.db

import androidx.room.*
import com.studysphere.data.models.*
import kotlinx.coroutines.flow.Flow

// ─── SubjectDao ──────────────────────────────────────────────────────────────

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)
}

// ─── LectureDao ──────────────────────────────────────────────────────────────

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY dayOfWeek ASC, startTimeHour ASC, startTimeMinute ASC")
    fun getAllLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE dayOfWeek = :dayOfWeek ORDER BY startTimeHour ASC, startTimeMinute ASC")
    fun getLecturesByDay(dayOfWeek: Int): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE subjectId = :subjectId")
    fun getLecturesBySubject(subjectId: Long): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE id = :id")
    suspend fun getLectureById(id: Long): Lecture?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: Lecture): Long

    @Update
    suspend fun updateLecture(lecture: Lecture)

    @Delete
    suspend fun deleteLecture(lecture: Lecture)

    @Query("DELETE FROM lectures WHERE subjectId = :subjectId")
    suspend fun deleteLecturesBySubject(subjectId: Long)
}

// ─── AttendanceDao ────────────────────────────────────────────────────────────

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getRecordsBySubject(subjectId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY lectureId ASC")
    fun getRecordsByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE lectureId = :lectureId AND date = :date LIMIT 1")
    suspend fun getRecordByLectureAndDate(lectureId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND status != 'CANCELLED'")
    suspend fun getNonCancelledRecords(subjectId: Long): List<AttendanceRecord>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND status = 'PRESENT'")
    suspend fun getPresentCount(subjectId: Long): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND status != 'CANCELLED'")
    suspend fun getTotalNonCancelledCount(subjectId: Long): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND status = 'CANCELLED'")
    suspend fun getCancelledCount(subjectId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteRecordsBySubject(subjectId: Long)

    @Query("DELETE FROM attendance_records WHERE lectureId = :lectureId")
    suspend fun deleteRecordsByLecture(lectureId: Long)
}

// ─── AssignmentDao ────────────────────────────────────────────────────────────

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments ORDER BY dueDate ASC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE status = 'PENDING' ORDER BY dueDate ASC")
    fun getPendingAssignments(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE subjectId = :subjectId ORDER BY dueDate ASC")
    fun getAssignmentsBySubject(subjectId: Long): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun getAssignmentById(id: Long): Assignment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)

    @Query("DELETE FROM assignments WHERE subjectId = :subjectId")
    suspend fun deleteAssignmentsBySubject(subjectId: Long)
}
