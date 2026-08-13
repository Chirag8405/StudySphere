package com.studysphere.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studysphere.data.models.*

// ─── Migrations ───────────────────────────────────────────────────────────────

/**
 * v1 → v2: Added extra-session support to [AttendanceRecord].
 * New columns: isExtra, startTimeHour, startTimeMinute, endTimeHour,
 *              endTimeMinute, room.
 * All default to their zero-value so existing rows are unaffected.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE attendance_records ADD COLUMN isExtra INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE attendance_records ADD COLUMN startTimeHour INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE attendance_records ADD COLUMN startTimeMinute INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE attendance_records ADD COLUMN endTimeHour INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE attendance_records ADD COLUMN endTimeMinute INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE attendance_records ADD COLUMN room TEXT NOT NULL DEFAULT ''")
    }
}

// Add new MIGRATION_X_Y constants here for every future schema bump.
// Never use fallbackToDestructiveMigration() — it silently wipes user data.

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [Subject::class, Lecture::class, AttendanceRecord::class, Assignment::class],
    version = 2,
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
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
        }
    }
}

