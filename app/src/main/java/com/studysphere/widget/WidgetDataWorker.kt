package com.studysphere.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.*
import com.studysphere.data.db.StudySphereDatabase
import com.studysphere.data.models.AssignmentStatus
import com.studysphere.data.repository.StudySphereRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "WidgetWorker"

class WidgetDataWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork started")
        return try {
            val today = LocalDate.now()
            val todayString = today.toString()          // "yyyy-MM-dd"
            val dayOfWeek = today.dayOfWeek.value       // Int: 1=Mon … 7=Sun
            val tomorrowString = today.plusDays(1).toString()

            // ── Database & repository ─────────────────────────────────────────
            val db = StudySphereDatabase.getInstance(appContext)
            val repository = StudySphereRepository(
                db.subjectDao(),
                db.lectureDao(),
                db.attendanceDao(),
                db.assignmentDao()
            )

            // ── Build subject id → name map ───────────────────────────────────
            val subjectMap = repository.getAllSubjectsList()
                .associateBy({ it.id }, { it.name })
            Log.d(TAG, "Subjects loaded: ${subjectMap.size}")

            // ── Fetch today's lectures ────────────────────────────────────────
            val lectures = repository.getLecturesByDay(dayOfWeek).first()
            Log.d(TAG, "Lectures fetched: ${lectures.size}")

            val widgetLectures = lectures.map { lecture ->
                val subjectName = subjectMap[lecture.subjectId] ?: "Unknown"
                val record = repository.getRecordByLectureAndDate(lecture.id, todayString)
                val timeRange = "%02d:%02d – %02d:%02d".format(
                    lecture.startTimeHour,
                    lecture.startTimeMinute,
                    lecture.endTimeHour,
                    lecture.endTimeMinute
                )
                WidgetLecture(
                    lectureId = lecture.id,
                    subjectId = lecture.subjectId,
                    subjectName = subjectName,
                    timeRange = timeRange,
                    room = lecture.room,
                    status = record?.status?.name  // null if unmarked
                )
            }

            // ── Fetch pending assignments ─────────────────────────────────────
            val pendingAssignments = repository.pendingAssignments.first()
                .filter { it.status == AssignmentStatus.PENDING }
                .sortedBy { it.dueDate }
                .take(5)
            Log.d(TAG, "Assignments fetched: ${pendingAssignments.size}")

            val widgetAssignments = pendingAssignments.map { assignment ->
                val dueDate = LocalDate.parse(assignment.dueDate)
                val dueLabel = when (assignment.dueDate) {
                    todayString    -> "Due today"
                    tomorrowString -> "Due tomorrow"
                    else           -> dueDate.format(
                        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                    )
                }
                val isOverdue = dueDate.isBefore(today)
                WidgetAssignment(
                    id = assignment.id,
                    title = assignment.title,
                    dueLabel = dueLabel,
                    isOverdue = isOverdue,
                    priority = assignment.priority.name
                )
            }

            // ── Serialize & push to Glance state ─────────────────────────────
            val widgetData = WidgetData(
                lectures = widgetLectures,
                assignments = widgetAssignments
            )
            val jsonString = Json.encodeToString(widgetData)
            Log.d(TAG, "JSON encoded (${jsonString.length} chars)")

            val manager = GlanceAppWidgetManager(appContext)
            val glanceIds = manager.getGlanceIds(StudySphereWidget::class.java)
            Log.d(TAG, "Glance IDs found: ${glanceIds.size}")

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(appContext, glanceId) { prefs ->
                    prefs[widgetDataKey] = jsonString
                }
                StudySphereWidget().update(appContext, glanceId)
            }

            Log.d(TAG, "State written successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "widget_data_refresh"

        fun enqueue(context: Context) {
            // Note: setExpedited() requires the FOREGROUND_SERVICE permission which
            // this app does not declare. Using a plain OneTimeWorkRequest instead
            // so the work is never silently rejected by the system.
            val request = OneTimeWorkRequestBuilder<WidgetDataWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
