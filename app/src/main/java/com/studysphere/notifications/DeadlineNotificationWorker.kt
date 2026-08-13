package com.studysphere.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studysphere.MainActivity
import com.studysphere.R
import com.studysphere.data.db.StudySphereDatabase
import com.studysphere.data.models.OTHER_SUBJECT_NAME
import com.studysphere.viewmodel.dataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * CoroutineWorker that runs daily (scheduled by [NotificationScheduler]).
 *
 * Each run:
 *  1. Reads `deadline_window_days` from DataStore (same key used by Settings).
 *  2. Queries Room for PENDING assignments due between today and today + window.
 *  3. Posts one system notification per assignment, using the assignment's
 *     database ID as the notification ID — so re-runs update rather than stack.
 *  4. Assignments marked COMPLETE, CANCELLED, or outside the window produce
 *     no notification automatically.
 */
class DeadlineNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID   = "deadline_alerts"
        const val CHANNEL_NAME = "Assignment Deadlines"
        private val KEY_DEADLINE_WINDOW = intPreferencesKey("deadline_window_days")
    }

    override suspend fun doWork(): Result {
        // Always create the channel first — safe to call repeatedly,
        // no-op if the channel already exists.
        createNotificationChannel()

        // ── 1. Read user's deadline window from DataStore ─────────────────────
        val prefs       = appContext.dataStore.data.first()
        val windowDays  = prefs[KEY_DEADLINE_WINDOW] ?: 3

        // ── 2. Query assignments due within the window ────────────────────────
        val db    = StudySphereDatabase.getInstance(appContext)
        val today = LocalDate.now()
        val end   = today.plusDays(windowDays.toLong())

        val assignments = db.assignmentDao()
            .getPendingAssignmentsDueBetween(
                from = today.toString(),   // "yyyy-MM-dd"
                to   = end.toString()
            )

        if (assignments.isEmpty()) return Result.success()

        // ── 3. Build a subject-name lookup for notification bodies ────────────
        val subjectMap = assignments
            .map { it.subjectId }
            .distinct()
            .mapNotNull { id -> db.subjectDao().getSubjectById(id) }
            .associateBy { it.id }

        // ── 4. Post one notification per assignment ───────────────────────────
        val notifManager = NotificationManagerCompat.from(appContext)

        for (assignment in assignments) {
            val dueDate    = LocalDate.parse(assignment.dueDate)
            val daysUntil  = ChronoUnit.DAYS.between(today, dueDate)
            val subject    = subjectMap[assignment.subjectId]
            val subjectLabel = subject?.name ?: OTHER_SUBJECT_NAME

            val dueLabel = when (daysUntil) {
                0L   -> "Due TODAY"
                1L   -> "Due TOMORROW"
                else -> "Due in $daysUntil days"
            }
            val bodyText = "$dueLabel · $subjectLabel"

            // Tapping the notification opens MainActivity (which shows the
            // Assignments tab if the user left it there, or the Dashboard).
            val tapIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // Pass the assignment id so the app could navigate later.
                putExtra("assignmentId", assignment.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                // Use assignment.id as request code to get one unique
                // PendingIntent per assignment.
                assignment.id.toInt(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(assignment.title)
                .setContentText(bodyText)
                // BigTextStyle shows the full body even for long subject names.
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                // Group all deadline notifications together in the shade.
                .setGroup("deadline_group")
                .build()

            try {
                // Notification ID = assignment.id.toInt() — updates the same
                // notification slot on re-runs instead of posting duplicates.
                notifManager.notify(assignment.id.toInt(), notification)
            } catch (e: SecurityException) {
                // POST_NOTIFICATIONS permission not granted — skip silently.
                // The next run will try again once permission is granted.
            }
        }

        return Result.success()
    }

    /**
     * Creates the "Assignment Deadlines" notification channel.
     * Required on API 26+. Safe to call every run — Android no-ops if the
     * channel already exists.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                // IMPORTANCE_HIGH = shows as a heads-up / peek notification.
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when assignment deadlines are approaching"
            }
            val manager = appContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
