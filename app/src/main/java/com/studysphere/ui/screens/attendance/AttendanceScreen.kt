package com.studysphere.ui.screens.attendance

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studysphere.data.models.*
import com.studysphere.ui.components.*
import com.studysphere.ui.theme.*
import com.studysphere.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    onSubjectDetail: (Long) -> Unit,
    onNavigateToSubjects: () -> Unit
) {
    val summaries by viewModel.attendanceSummariesRefreshed.collectAsState()
    val subjects  by viewModel.subjects.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { viewModel.refreshAll() }
    )

    LaunchedEffect(Unit) { viewModel.refreshSummaries() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        if (subjects.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    icon        = Icons.Rounded.School,
                    title       = "No Subjects Added",
                    subtitle    = "Add subjects to start tracking your attendance",
                    actionLabel = "Manage Subjects",
                    onAction    = onNavigateToSubjects
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AttendanceOverviewCard(summaries = summaries)
                    Spacer(Modifier.height(4.dp))
                }

                items(summaries, key = { it.subject.id }) { summary ->
                    AttendanceSummaryCard(
                        summary  = summary,
                        onClick  = { onSubjectDetail(summary.subject.id) }
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AttendanceOverviewCard(summaries: List<SubjectAttendanceSummary>) {
    val safe     = summaries.count { it.riskLevel == RiskLevel.SAFE }
    val warning  = summaries.count { it.riskLevel == RiskLevel.WARNING }
    val danger   = summaries.count { it.riskLevel == RiskLevel.DANGER }
    val critical = summaries.count { it.riskLevel == RiskLevel.CRITICAL }
    val avgPct   = if (summaries.isEmpty()) 0f else summaries.map { it.percentage }.average().toFloat()

    SphereCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Overview", style = MaterialTheme.typography.titleMedium,
                         fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text("${summaries.size} subjects tracked",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${avgPct.toInt()}%",
                         style = MaterialTheme.typography.displaySmall,
                         fontWeight = FontWeight.Bold,
                         color = if (avgPct >= 75) Green500 else Red500)
                    Text("avg attendance",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Risk breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiskCountChip("Safe", safe, Green500, Modifier.weight(1f))
                RiskCountChip("Warn", warning, Amber500, Modifier.weight(1f))
                RiskCountChip("Risk", danger, Red500, Modifier.weight(1f))
                RiskCountChip("Crit", critical, Red600, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RiskCountChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(count.toString(), style = MaterialTheme.typography.titleMedium,
             fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun AttendanceSummaryCard(
    summary: SubjectAttendanceSummary,
    onClick: () -> Unit
) {
    SphereCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubjectColorDot(summary.subject.colorHex, size = 10.dp)
                    Text(summary.subject.name,
                         style = MaterialTheme.typography.titleSmall,
                         fontWeight = FontWeight.SemiBold,
                         color = MaterialTheme.colorScheme.onBackground)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RiskIndicator(summary.riskLevel)
                    Icon(Icons.Rounded.ChevronRight, null,
                         Modifier.size(16.dp),
                         tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                }
            }

            // Progress bar
            AttendanceProgressBar(
                percentage   = summary.percentage,
                minThreshold = summary.subject.minAttendancePercent,
                colorHex     = summary.subject.colorHex
            )

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttendanceStat("Present", summary.attended.toString(), Green500)
                AttendanceStat("Absent",
                    (summary.totalClasses - summary.attended).toString(), Red500)
                AttendanceStat("Cancelled", summary.cancelled.toString(), Amber500)
                AttendanceStat("Total",
                    (summary.totalClasses + summary.cancelled).toString(),
                    MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Insight banner
            val insightColor: Color
            val insightText: String
            val insightIcon: androidx.compose.ui.graphics.vector.ImageVector
            when {
                summary.totalClasses == 0 -> {
                    insightColor = MaterialTheme.colorScheme.onSurfaceVariant
                    insightText = "No classes recorded yet — mark attendance to see insights"
                    insightIcon = Icons.Rounded.Info
                }
                summary.riskLevel == RiskLevel.SAFE && summary.canSkip > 0 -> {
                    insightColor = Green600
                    insightText = "You can safely skip up to ${summary.canSkip} more class${if (summary.canSkip == 1) "" else "es"}"
                    insightIcon = Icons.Rounded.CheckCircle
                }
                summary.mustAttend > 0 -> {
                    insightColor = Red500
                    insightText = "Attend ${summary.mustAttend} consecutive class${if (summary.mustAttend == 1) "" else "es"} to reach ${summary.subject.minAttendancePercent.toInt()}%"
                    insightIcon = Icons.Rounded.Warning
                }
                else -> {
                    insightColor = Amber500
                    insightText = "Borderline — don't miss any more classes"
                    insightIcon = Icons.Rounded.TrendingDown
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(insightColor.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(insightIcon, null, Modifier.size(14.dp), tint = insightColor)
                Text(insightText, style = MaterialTheme.typography.labelSmall,
                     color = insightColor)
            }
        }
    }
}

@Composable
private fun AttendanceStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleSmall,
             fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
