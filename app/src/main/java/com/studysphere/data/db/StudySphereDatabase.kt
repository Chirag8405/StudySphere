package com.studysphere.data.db

import android.content.Context
import androidx.room.*
import com.studysphere.data.models.*

@Database(
    entities = [Subject::class, Lecture::class, AttendanceRecord::class, Assignment::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudySphereDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun lectureDao(): LectureDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun assignmentDao(): AssignmentDao

    companion object {
        @Volatile private var INSTANCE: StudySphereDatabase? = null

        fun getInstance(context: Context): StudySphereDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StudySphereDatabase::class.java,
                    "studysphere.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
