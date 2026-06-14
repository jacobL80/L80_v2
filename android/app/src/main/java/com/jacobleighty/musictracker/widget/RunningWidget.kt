package com.jacobleighty.musictracker.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.RunningWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

private val RW_ACCENT = Color(0xFF47A025)

class RunningWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (weeks, yearTotal, weekTotal) = fetchData(context)
        val last10 = weeks.takeLast(10)
        provideContent { RunningWidgetContent(weekTotal, yearTotal, last10) }
    }

    private suspend fun fetchData(context: Context): Triple<List<RunningWeek>, Float, Float> =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
            val weeks: List<RunningWeek> = try {
                val result = ApiService.create().getRunningWeeks().sortedBy { it.weekStart }
                prefs.edit().putString("running_weeks_json", Gson().toJson(result)).apply()
                result
            } catch (_: Exception) {
                val json = prefs.getString("running_weeks_json", null)
                    ?: return@withContext Triple(emptyList<RunningWeek>(), 0f, 0f)
                try {
                    Gson().fromJson(json, object : TypeToken<List<RunningWeek>>() {}.type)
                } catch (_: Exception) {
                    return@withContext Triple(emptyList<RunningWeek>(), 0f, 0f)
                }
            }

            val today = LocalDate.now()
            val yearTotal = weeks.filter { it.year == today.year }.sumOf { it.total.toDouble() }.toFloat()
            val thisMonday = today.with(DayOfWeek.MONDAY).toString()
            val weekTotal = weeks.find { it.weekStart == thisMonday }?.total ?: 0f

            Triple(weeks, yearTotal, weekTotal)
        }
}

class OpenRunningAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("l80://running")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

private fun buildBarChart(weeks: List<RunningWeek>, bmpW: Int, bmpH: Int, density: Float): Bitmap {
    val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    if (weeks.isEmpty()) return bmp

    val today = LocalDate.now()
    val thisMonday = today.with(DayOfWeek.MONDAY).toString()
    val maxTotal = weeks.maxOf { it.total }.coerceAtLeast(1f)
    val count = weeks.size
    val gap = 4f * density
    val barW = (bmpW - gap * (count - 1)) / count
    val labelH = 14f * density
    val maxBarH = bmpH - labelH

    val pastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(140, 71, 160, 37)
    }
    val currentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#47A025")
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#666666")
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#aaaaaa")
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    weeks.forEachIndexed { i, week ->
        val x = i * (barW + gap)
        val barH = (week.total / maxTotal) * maxBarH
        val top = maxBarH - barH
        val isCurrent = week.weekStart == thisMonday

        canvas.drawRoundRect(RectF(x, top, x + barW, maxBarH), 3f, 3f, if (isCurrent) currentPaint else pastPaint)

        // Date label below bars for first, last, and current week
        if (i == 0 || i == count - 1 || isCurrent) {
            val parts = week.weekStart.split("-")
            if (parts.size >= 3) {
                canvas.drawText(
                    "${parts[1]}/${parts[2].trimStart('0')}",
                    x + barW / 2, bmpH.toFloat() - 1f, labelPaint,
                )
            }
        }

        // Miles value above the current week bar
        if (isCurrent && barH > 16f && week.total > 0f) {
            canvas.drawText("%.1f".format(week.total), x + barW / 2, top - 2f, valuePaint)
        }
    }

    return bmp
}

@Composable
private fun RunningWidgetContent(weekTotal: Float, yearTotal: Float, weeks: List<RunningWeek>) {
    val size = LocalSize.current
    val density = LocalContext.current.resources.displayMetrics.density
    val bmpW = (size.width.value * density).toInt().coerceAtLeast(200)
    val bmpH = (bmpW * 0.32f).toInt().coerceAtLeast(60)
    val chartBmp = buildBarChart(weeks, bmpW, bmpH, density)

    val bgColor = ColorProvider(Color(0xFF0f0f0f))
    val accent  = ColorProvider(RW_ACCENT)
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
            "RUNNING",
            style = TextStyle(color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "%.1f mi".format(weekTotal),
                    style = TextStyle(color = textPri, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                )
                Text("this week", style = TextStyle(color = textSec, fontSize = 10.sp))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "%.0f mi".format(yearTotal),
                    style = TextStyle(color = textPri, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                )
                Text("this year", style = TextStyle(color = textSec, fontSize = 10.sp))
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Image(
            provider = ImageProvider(chartBmp),
            contentDescription = "Last 10 weeks mileage",
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentScale = ContentScale.FillBounds,
        )
    }
}
