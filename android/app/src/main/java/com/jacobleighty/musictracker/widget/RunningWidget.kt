package com.jacobleighty.musictracker.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.RunningWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RunningWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (thisWeek, weekTotal) = fetchThisWeek(context)
        provideContent { RunningWidgetContent(thisWeek, weekTotal) }
    }

    private suspend fun fetchThisWeek(context: Context): Pair<RunningWeek?, Float> = withContext(Dispatchers.IO) {
        try {
            val weeks = ApiService.create().getRunningWeeks()
            val latest = weeks.maxByOrNull { it.weekStart }
            Pair(latest, latest?.total ?: 0f)
        } catch (_: Exception) {
            Pair(null, 0f)
        }
    }
}

class OpenRunningAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("l80://running")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

private val DAY_COLORS_WIDGET = listOf(
    Color(0xFF4A6FA5), Color(0xFF47A025), Color(0xFF8A3ABA),
    Color(0xFFEC6F00), Color(0xFFD63030), Color(0xFF1696B6), Color(0xFFB8A000),
)
private val DAY_LABELS_SHORT = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
private fun RunningWidgetContent(week: RunningWeek?, weekTotal: Float) {
    val accent  = ColorProvider(Color(0xFF4A6FA5))
    val bgColor = ColorProvider(Color(0xFF0f0f0f))
    val textPri = ColorProvider(Color(0xFFE8E8E8))
    val textSec = ColorProvider(Color(0xFF888888))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .background(bgColor)
            .clickable(actionRunCallback<OpenRunningAction>())
            .padding(12.dp),
    ) {
        Text(
            text = "Running",
            style = TextStyle(color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(bottom = 4.dp),
        )
        if (week == null) {
            Text("No data yet", style = TextStyle(color = textSec, fontSize = 12.sp))
            return@Column
        }

        Text(
            text = "This week",
            style = TextStyle(color = textSec, fontSize = 10.sp),
        )
        Text(
            text = "%.1f mi".format(weekTotal),
            style = TextStyle(color = textPri, fontSize = 28.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(top = 2.dp, bottom = 8.dp),
        )

        // Day breakdown
        val days = listOf(week.mon, week.tue, week.wed, week.thu, week.fri, week.sat, week.sun)
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.Start) {
            days.zip(DAY_COLORS_WIDGET).zip(DAY_LABELS_SHORT).forEach { (pair, label) ->
                val (miles, color) = pair
                Column(
                    modifier = GlanceModifier.padding(end = 6.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Text(label, style = TextStyle(color = ColorProvider(color.copy(alpha = 0.6f)), fontSize = 8.sp))
                    Text(
                        text = if (miles > 0f) "%.0f".format(miles) else "—",
                        style = TextStyle(
                            color = if (miles > 0f) ColorProvider(color) else textSec,
                            fontSize = 11.sp,
                            fontWeight = if (miles > 0f) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }

        if (week.weekStart.isNotEmpty()) {
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "Week of ${week.weekStart}",
                style = TextStyle(color = textSec, fontSize = 9.sp),
            )
        }
    }
}
