package com.studysphere.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.studysphere.data.models.*
import com.studysphere.ui.theme.*

// ─── SphereCard ───────────────────────────────────────────────────────────────

@Composable
fun SphereCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant
                         else MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.6f else 0.8f)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Column(content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Column(content = content)
        }
    }
}

// ─── SubjectColorDot ──────────────────────────────────────────────────────────

@Composable
fun SubjectColorDot(colorHex: String, size: Dp = 10.dp) {
    val color = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (e: Exception) { Indigo500 }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

// ─── SubjectChip ──────────────────────────────────────────────────────────────

@Composable
fun SubjectChip(subject: Subject, modifier: Modifier = Modifier) {
    val color = remember(subject.colorHex) {
        try { Color(android.graphics.Color.parseColor(subject.colorHex)) }
        catch (e: Exception) { Indigo500 }
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        SubjectColorDot(subject.colorHex, size = 7.dp)
        Text(
            text = subject.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── AttendanceStatusChip ─────────────────────────────────────────────────────

@Composable
fun AttendanceStatusChip(status: AttendanceStatus?) {
    val (label, bgColor, fgColor, icon) = when (status) {
        AttendanceStatus.PRESENT   -> Quad("Present", Green500.copy(0.15f), Green600, Icons.Rounded.CheckCircle)
        AttendanceStatus.ABSENT    -> Quad("Absent", Red500.copy(0.12f), Red500, Icons.Rounded.Cancel)
        AttendanceStatus.CANCELLED -> Quad("Cancelled", Amber500.copy(0.15f), Amber500, Icons.Rounded.RemoveCircle)
        null                       -> Quad("Not Marked", Slate500.copy(0.1f), Slate500, Icons.Rounded.RadioButtonUnchecked)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = fgColor)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = fgColor, fontWeight = FontWeight.SemiBold)
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ─── PriorityBadge ────────────────────────────────────────────────────────────

@Composable
fun PriorityBadge(priority: Priority) {
    val (label, color) = when (priority) {
        Priority.LOW    -> "Low" to Teal500
        Priority.MEDIUM -> "Medium" to Amber500
        Priority.HIGH   -> "High" to Red500
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall,
             color = color, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

// ─── RiskLevelBar ─────────────────────────────────────────────────────────────

@Composable
fun AttendanceProgressBar(
    percentage: Float,
    minThreshold: Float,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    val color = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (e: Exception) { Indigo500 }
    }
    val progress = (percentage / 100f).coerceIn(0f, 1f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
            // Threshold marker
            val markerPos = (minThreshold / 100f).coerceIn(0.02f, 0.98f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(markerPos)
                    .wrapContentWidth(Alignment.End)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                )
            }
        }
    }
}

// ─── SectionHeader ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─── EmptyState ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
             color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
                Text(actionLabel)
            }
        }
    }
}

// ─── RiskIndicator ────────────────────────────────────────────────────────────

@Composable
fun RiskIndicator(riskLevel: RiskLevel, compact: Boolean = false) {
    val (label, bgColor, fgColor, icon) = when (riskLevel) {
        RiskLevel.SAFE     -> Quad("Safe", Green500.copy(0.12f), Green600, Icons.Rounded.Shield)
        RiskLevel.WARNING  -> Quad("Warning", Amber500.copy(0.12f), Amber500, Icons.Rounded.Warning)
        RiskLevel.DANGER   -> Quad("Danger", Red500.copy(0.12f), Red500, Icons.Rounded.Error)
        RiskLevel.CRITICAL -> Quad("Critical", Red600.copy(0.18f), Red600, Icons.Rounded.GppBad)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null,
             modifier = Modifier.size(if (compact) 12.dp else 14.dp), tint = fgColor)
        if (!compact) {
            Text(text = label, style = MaterialTheme.typography.labelSmall,
                 color = fgColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── ConfirmDeleteDialog ──────────────────────────────────────────────────────

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.DeleteForever, contentDescription = null,
                 tint = MaterialTheme.colorScheme.error)
        },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ─── LoadingSpinner ───────────────────────────────────────────────────────────

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ─── TopBarGradient ────────────────────────────────────────────────────────────

@Composable
fun GradientDivider() {
    val isDark = LocalDarkTheme.current
    Divider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.3f else 0.4f),
        thickness = 0.5.dp
    )
}
