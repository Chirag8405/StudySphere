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
    shape: Shape = RoundedCornerShape(12.dp),
    elevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    // Pure monochrome background - no purple tint
    val containerColor = if (isDark) DarkSurface else Color.White

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Column(content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
        ) {
            Column(content = content)
        }
    }
}

// ─── SubjectColorDot ──────────────────────────────────────────────────────────

@Composable
fun SubjectColorDot(colorHex: String, size: Dp = 10.dp) {
    // Hidden in monochrome theme as per request to remove dots
}

// ─── SubjectChip ──────────────────────────────────────────────────────────────

@Composable
fun SubjectChip(subject: Subject, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Dot removed as per request
        Text(
            text = subject.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── AttendanceStatusChip ─────────────────────────────────────────────────────

@Composable
fun AttendanceStatusChip(status: AttendanceStatus?) {
    val (label, bgColor, fgColor, icon) = when (status) {
        AttendanceStatus.PRESENT   -> Quad("Present", Green500.copy(0.1f), Green500, Icons.Rounded.CheckCircle)
        AttendanceStatus.ABSENT    -> Quad("Absent", Red500.copy(0.1f), Red500, Icons.Rounded.Cancel)
        AttendanceStatus.CANCELLED -> Quad("Cancelled", Amber500.copy(0.1f), Amber500, Icons.Rounded.RemoveCircle)
        null                       -> Quad("Not Marked", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.RadioButtonUnchecked)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = fgColor)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = fgColor, fontWeight = FontWeight.Bold)
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ─── PriorityBadge ────────────────────────────────────────────────────────────

@Composable
fun PriorityBadge(priority: Priority) {
    val (label, color) = when (priority) {
        Priority.LOW    -> "Low" to Gray500
        Priority.MEDIUM -> "Medium" to Amber500
        Priority.HIGH   -> "High" to Red500
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall,
             color = color, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
    }
}

// ─── RiskLevelBar ─────────────────────────────────────────────────────────────

@Composable
fun AttendanceProgressBar(
    percentage: Float,
    minThreshold: Float,
    colorHex: String, 
    modifier: Modifier = Modifier,
    totalClasses: Int = 1 // Added to handle 0/0 case
) {
    val progress = (percentage / 100f).coerceIn(0f, 1f)
    val color = when {
        totalClasses == 0 -> MaterialTheme.colorScheme.outlineVariant
        percentage >= minThreshold -> Green500
        else -> Red500
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (totalClasses == 0) 0f else progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
             color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple,
                    contentColor = Color.White
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}

// ─── RiskIndicator ────────────────────────────────────────────────────────────

@Composable
fun RiskIndicator(riskLevel: RiskLevel, compact: Boolean = false) {
    val (label, bgColor, fgColor, icon) = when (riskLevel) {
        RiskLevel.SAFE     -> Quad("Safe", Green500.copy(0.1f), Green500, Icons.Rounded.Shield)
        RiskLevel.WARNING  -> Quad("Warning", Amber500.copy(0.1f), Amber500, Icons.Rounded.Warning)
        RiskLevel.DANGER   -> Quad("Danger", Red500.copy(0.1f), Red500, Icons.Rounded.Error)
        RiskLevel.CRITICAL -> Quad("Critical", Red600.copy(0.1f), Red600, Icons.Rounded.GppBad)
        RiskLevel.NEUTRAL  -> Quad("No Data", Gray500.copy(0.1f), Gray500, Icons.Rounded.HorizontalRule)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null,
             modifier = Modifier.size(if (compact) 12.dp else 14.dp), tint = fgColor)
        if (!compact) {
            Text(text = label, style = MaterialTheme.typography.labelSmall,
                 color = fgColor, fontWeight = FontWeight.Bold)
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
    val isDark = LocalDarkTheme.current
    val shape = RoundedCornerShape(12.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, if (isDark) Gray700 else Gray400, shape),
        containerColor = if (isDark) PureBlack else Color.White,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        },
        shape = shape
    )
}

// ─── LoadingSpinner ───────────────────────────────────────────────────────────

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ─── TopBarGradient ────────────────────────────────────────────────────────────

@Composable
fun GradientDivider() {
    Divider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp
    )
}
