package com.studysphere.widget.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.studysphere.MainActivity
import com.studysphere.data.models.AttendanceStatus
import com.studysphere.widget.MarkAttendanceCallback
import com.studysphere.widget.WidgetLecture
import com.studysphere.widget.lectureIdKey
import com.studysphere.widget.statusKey
import com.studysphere.widget.subjectIdKey

// ─── Semantic status badge colors (fixed, not theme-driven) ──────────────────

private val colorPresent   = Color(0xFF30D158)
private val colorAbsent    = Color(0xFFFF453A)
private val colorCancelled = Color(0xFFFFD60A)
private val colorDark      = Color(0xFF1C1C1E)

// ─── LecturePage ──────────────────────────────────────────────────────────────

@Composable
fun LecturePage(lectures: List<WidgetLecture>) {
    if (lectures.isEmpty()) {
        Box(
            modifier         = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📅",
                    style = TextStyle(fontSize = 32.sp)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text  = "No classes today",
                    style = TextStyle(
                        color    = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
            }
        }
    } else {
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(lectures) { lecture ->
                LectureRow(lecture = lecture)
            }
        }
    }
}

// ─── LectureRow ───────────────────────────────────────────────────────────────

@Composable
private fun LectureRow(lecture: WidgetLecture) {
    val status = lecture.attendanceStatus()

    val openAttendanceIntent = Intent(
        LocalContext.current,
        MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("start_route", "attendance")
    }

    Row(
        modifier          = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left info column — tapping opens Attendance tab
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(openAttendanceIntent))
        ) {
            Text(
                text     = lecture.subjectName,
                style    = TextStyle(
                    color      = GlanceTheme.colors.onSurface,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text     = if (lecture.room.isNotBlank()) "${lecture.timeRange}  ·  ${lecture.room}"
                           else lecture.timeRange,
                style    = TextStyle(
                    color    = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Right side: P/A/C buttons or read-only status badge
        if (status == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AttendanceButton(
                    label      = "P",
                    bgColor    = colorPresent,
                    lectureId  = lecture.lectureId,
                    subjectId  = lecture.subjectId,
                    statusName = AttendanceStatus.PRESENT.name
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                AttendanceButton(
                    label      = "A",
                    bgColor    = colorAbsent,
                    lectureId  = lecture.lectureId,
                    subjectId  = lecture.subjectId,
                    statusName = AttendanceStatus.ABSENT.name
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                AttendanceButton(
                    label      = "C",
                    bgColor    = colorCancelled,
                    lectureId  = lecture.lectureId,
                    subjectId  = lecture.subjectId,
                    statusName = AttendanceStatus.CANCELLED.name
                )
            }
        } else {
            StatusBadge(status = status)
        }
    }
}

// ─── AttendanceButton ─────────────────────────────────────────────────────────

@Composable
private fun AttendanceButton(
    label: String,
    bgColor: Color,
    lectureId: Long,
    subjectId: Long,
    statusName: String
) {
    Box(
        modifier = GlanceModifier
            .size(28.dp)
            .background(bgColor.copy(alpha = 0.2f))
            .cornerRadius(14.dp)
            .clickable(
                actionRunCallback<MarkAttendanceCallback>(
                    actionParametersOf(
                        lectureIdKey to lectureId,
                        subjectIdKey to subjectId,
                        statusKey    to statusName
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = label,
            style = TextStyle(
                color      = ColorProvider(bgColor),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// ─── StatusBadge ──────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: AttendanceStatus) {
    val (bgColor, textColor, label) = when (status) {
        AttendanceStatus.PRESENT   -> Triple(colorPresent,   Color.White, "Present")
        AttendanceStatus.ABSENT    -> Triple(colorAbsent,    Color.White, "Absent")
        AttendanceStatus.CANCELLED -> Triple(colorCancelled, colorDark,   "Cancelled")
    }

    Box(
        modifier = GlanceModifier
            .background(bgColor)
            .cornerRadius(12.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = label,
            style = TextStyle(
                color      = ColorProvider(textColor),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
