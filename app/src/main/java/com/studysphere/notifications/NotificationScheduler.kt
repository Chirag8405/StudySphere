package com.studysphere.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Schedules (or re-schedules) the daily deadline notification job.
 *
 * Call this from MainActivity.onCreate() — WorkManager deduplicates using
 * [ExistingPeriodicWorkPolicy.UPDATE], so it's safe to call on every launch.
 * The worker fires daily at approximately 08:00 AM local time. If the device
 * is in Doze mode, WorkManager will run the job at the next opportunity.
 */
object NotificationScheduler {

    private const val WORK_NAME = "deadline_notifications"

    fun schedule(context: Context) {
        val now     = LocalDateTime.now()
        val target  = now.toLocalDate().atTime(LocalTime.of(8, 0))

        // If 8 AM has already passed today, aim for 8 AM tomorrow.
        val nextRun = if (now.isBefore(target)) target else target.plusDays(1)
        val initialDelayMinutes = Duration.between(now, nextRun).toMinutes()

        val request = PeriodicWorkRequestBuilder<DeadlineNotificationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // UPDATE: if a work with this name already exists, replace its
            // schedule (useful when the user changes the deadline window or
            // if the initial delay needs to be recalculated after an update).
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
