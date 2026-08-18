package com.studysphere.widget.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
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
import com.studysphere.data.models.Priority
import com.studysphere.widget.WidgetAssignment

// ─── Priority colors (semantic) ───────────────────────────────────────────────

private val colorHigh   = Color(0xFFFF453A)
private val colorMedium = Color(0xFFFFD60A)
private val colorLow    = Color(0xFF0A84FF)

// ─── AssignmentPage ───────────────────────────────────────────────────────────

@Composable
fun AssignmentPage(assignments: List<WidgetAssignment>) {
    if (assignments.isEmpty()) {
        Box(
            modifier         = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✓",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 32.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text  = "All Clear!",
                    style = TextStyle(
                        color      = GlanceTheme.colors.onSurface,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text  = "No pending assignments",
                    style = TextStyle(
                        color    = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                )
            }
        }
    } else {
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(assignments) { assignment ->
                AssignmentRow(assignment = assignment)
            }
        }
    }
}

// ─── AssignmentRow ────────────────────────────────────────────────────────────

@Composable
private fun AssignmentRow(assignment: WidgetAssignment) {
    val openAssignmentsIntent = Intent(
        LocalContext.current,
        MainActivity::class.java
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("start_route", "assignments")
    }

    val priority = assignment.priorityEnum()
    val priorityColor = when (priority) {
        Priority.HIGH   -> colorHigh
        Priority.MEDIUM -> colorMedium
        Priority.LOW    -> colorLow
    }
    val priorityLabel = when (priority) {
        Priority.HIGH   -> "High"
        Priority.MEDIUM -> "Med"
        Priority.LOW    -> "Low"
    }

    Row(
        modifier          = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(actionStartActivity(openAssignmentsIntent)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority dot
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(priorityColor)
                .cornerRadius(4.dp)
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Title (error color if overdue)
        Text(
            text     = assignment.title,
            style    = TextStyle(
                color      = if (assignment.isOverdue) GlanceTheme.colors.error
                             else                      GlanceTheme.colors.onSurface,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        // Due label
        Text(
            text  = assignment.dueLabel,
            style = TextStyle(
                color    = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        // Priority chip
        Box(
            modifier = GlanceModifier
                .background(priorityColor.copy(alpha = 0.2f))
                .cornerRadius(8.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = priorityLabel,
                style = TextStyle(
                    color      = ColorProvider(priorityColor),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
