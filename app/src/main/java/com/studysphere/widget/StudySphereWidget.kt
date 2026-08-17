package com.studysphere.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.studysphere.widget.ui.AssignmentPage
import com.studysphere.widget.ui.LecturePage
import kotlinx.serialization.json.Json
import android.util.Log

class StudySphereWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("WidgetCompose", "provideGlance called for id: $id")
        provideContent {
            Log.d("WidgetCompose", "provideContent block entered")
            GlanceTheme {
                Content()
            }
        }
    }

    @Composable
    private fun Content() {
        val prefs       = currentState<androidx.datastore.preferences.core.Preferences>()
        val jsonString  = prefs[widgetDataKey]
        val currentPage = prefs[currentPageKey] ?: 0

        val widgetData: WidgetData? = jsonString?.takeIf { it.isNotBlank() }?.let {
            try {
                Json.decodeFromString<WidgetData>(it)
            } catch (e: Exception) {
                Log.e("WidgetCompose", "JSON decode error for string: $it", e)
                null
            }
        }
        Log.d("WidgetCompose", "WidgetData is null? ${widgetData == null}, jsonString empty? ${jsonString.isNullOrBlank()}")

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {

                // ── Header row ────────────────────────────────────────────────
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "StudySphere",
                        style = TextStyle(
                            color      = GlanceTheme.colors.onSurface,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    // Refresh icon button
                    Box(
                        modifier = GlanceModifier
                            .size(28.dp)
                            .clickable(actionRunCallback<RefreshWidgetCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 18.sp
                            )
                        )
                    }
                }

                // ── Page content ──────────────────────────────────────────────
                Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    if (widgetData == null) {
                        Box(
                            modifier          = GlanceModifier.fillMaxSize(),
                            contentAlignment  = Alignment.Center
                        ) {
                            Text(
                                text  = "Loading…",
                                style = TextStyle(
                                    color    = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    } else {
                        when (currentPage) {
                            0    -> LecturePage(lectures = widgetData.lectures)
                            1    -> AssignmentPage(assignments = widgetData.assignments)
                            else -> LecturePage(lectures = widgetData.lectures)
                        }
                    }
                }

                // ── Page indicator dots ───────────────────────────────────────
                Row(
                    modifier              = GlanceModifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Left half tap zone → page 0 (Schedule)
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(vertical = 6.dp)
                            .clickable(
                                actionRunCallback<NavigatePageCallback>(
                                    actionParametersOf(pageKey to 0)
                                )
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        PageDot(active = currentPage == 0)
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Right half tap zone → page 1 (Assignments)
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(vertical = 6.dp)
                            .clickable(
                                actionRunCallback<NavigatePageCallback>(
                                    actionParametersOf(pageKey to 1)
                                )
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        PageDot(active = currentPage == 1)
                    }
                }
            }
        }
    }

    @Composable
    private fun PageDot(active: Boolean) {
        Box(
            modifier = GlanceModifier
                .size(if (active) 8.dp else 6.dp)
                .background(
                    if (active) GlanceTheme.colors.primary
                    else        GlanceTheme.colors.onSurfaceVariant
                )
                .cornerRadius(4.dp)
        ) {}
    }
}
