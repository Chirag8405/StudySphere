package com.studysphere.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.studysphere.data.db.StudySphereDatabase
import com.studysphere.data.models.AttendanceStatus
import com.studysphere.data.repository.StudySphereRepository
import java.time.LocalDate

// ─── ActionParameters keys ────────────────────────────────────────────────────

val lectureIdKey = ActionParameters.Key<Long>("lecture_id")
val subjectIdKey = ActionParameters.Key<Long>("subject_id")
val statusKey    = ActionParameters.Key<String>("status")
val pageKey      = ActionParameters.Key<Int>("page")

// ─── PreferencesKey for widget data JSON ──────────────────────────────────────

val widgetDataKey  = androidx.datastore.preferences.core.stringPreferencesKey("widget_data")
val currentPageKey = androidx.datastore.preferences.core.intPreferencesKey("current_page")

// ─── Attendance Mark Callback (P / A / C buttons) ────────────────────────────

class MarkAttendanceCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val lectureId = parameters[lectureIdKey] ?: return
        val subjectId = parameters[subjectIdKey] ?: return
        val statusStr = parameters[statusKey]    ?: return
        val status    = runCatching { AttendanceStatus.valueOf(statusStr) }.getOrNull() ?: return

        val db = StudySphereDatabase.getInstance(context.applicationContext)
        val repository = StudySphereRepository(
            db.subjectDao(),
            db.lectureDao(),
            db.attendanceDao(),
            db.assignmentDao()
        )

        repository.markAttendance(
            lectureId = lectureId,
            subjectId = subjectId,
            date      = LocalDate.now().toString(),
            status    = status
        )

        // Re-enqueue the data worker to push updated state to all widget instances
        WidgetDataWorker.enqueue(context.applicationContext)
    }
}

// ─── Page Navigation Callback (indicator dot taps) ───────────────────────────

class NavigatePageCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val targetPage = parameters[pageKey] ?: return

        val manager   = GlanceAppWidgetManager(context.applicationContext)
        val glanceIds = manager.getGlanceIds(StudySphereWidget::class.java)

        glanceIds.forEach { id ->
            // updateAppWidgetState defaults to PreferencesGlanceStateDefinition in 1.1.0
            updateAppWidgetState(context.applicationContext, id) { prefs ->
                prefs[currentPageKey] = targetPage
            }
            StudySphereWidget().update(context.applicationContext, id)
        }
    }
}

// ─── Refresh Callback (header refresh button) ─────────────────────────────────

class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetDataWorker.enqueue(context.applicationContext)
    }
}
