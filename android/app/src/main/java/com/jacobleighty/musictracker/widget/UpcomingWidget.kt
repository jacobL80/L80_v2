package com.jacobleighty.musictracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jacobleighty.musictracker.MainActivity
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.Artist
import com.jacobleighty.musictracker.ui.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpcomingWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val upcoming = fetchUpcoming()
        provideContent {
            val size = LocalSize.current
            val twoColumn = size.width >= 250.dp
            val availableHeight = size.height.value - 32f  // subtract title + padding
            val rows = maxOf(1, (availableHeight / 44f).toInt())
            val maxCards = minOf(10, rows * if (twoColumn) 2 else 1)
            WidgetContent(upcoming.take(maxCards), twoColumn)
        }
    }

    private suspend fun fetchUpcoming(): List<Artist> = withContext(Dispatchers.IO) {
        try {
            ApiService.create().getArtists()
                .filter { it.nextRelease.isNotEmpty() && DateUtils.hasFullDate(it.nextRelease) }
                .sortedBy { DateUtils.parseDate(it.nextRelease) }
        } catch (_: Exception) { emptyList() }
    }
}

@Composable
private fun WidgetContent(artists: List<Artist>, twoColumn: Boolean) {
    val bgColor = ColorProvider(Color(0xFF0f0f0f))
    val openApp = actionStartActivity<MainActivity>()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(openApp)
            .padding(8.dp),
    ) {
        Text(
            text = "Upcoming Releases",
            style = TextStyle(
                color = ColorProvider(Color(0xFFEC6F00)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.padding(start = 6.dp, bottom = 6.dp),
        )

        if (artists.isEmpty()) {
            Text(
                text = "No upcoming releases",
                style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 12.sp),
            )
            return@Column
        }

        if (twoColumn) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                artists.chunked(2).forEach { rowArtists ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        horizontalAlignment = Alignment.Horizontal.Start,
                    ) {
                        rowArtists.forEach { a ->
                            WidgetCard(a, modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(2.dp))
                        }
                        if (rowArtists.size == 1) Spacer(GlanceModifier.defaultWeight())
                    }
                }
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                artists.forEach { a ->
                    WidgetCard(a, modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(artist: Artist, modifier: GlanceModifier = GlanceModifier) {
    val (month, day, year) = DateUtils.parseParts(artist.nextRelease)
    val daysUntil  = DateUtils.daysUntil(artist.nextRelease)
    val imminent   = daysUntil in 0..6

    val cardBg        = if (imminent) ColorProvider(Color(0xFF1a2a1a)) else ColorProvider(Color(0xFF1e1e1e))
    val accentColor   = ColorProvider(Color(0xFFEC6F00))
    val textPrimary   = ColorProvider(Color(0xFFe8e8e8))
    val textSecondary = ColorProvider(Color(0xFF888888))
    val imminentColor = ColorProvider(Color(0xFFEC6F00))

    Row(
        modifier = modifier.background(cardBg).padding(6.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            modifier = GlanceModifier.width(36.dp),
        ) {
            if (month != null) {
                Text(month, style = TextStyle(color = accentColor, fontSize = 8.sp, fontWeight = FontWeight.Bold))
            }
            if (day != null) {
                Text(day.toString(), style = TextStyle(color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
            if (year != null) {
                Text(year.toString(), style = TextStyle(color = textSecondary, fontSize = 9.sp))
            }
        }

        Spacer(GlanceModifier.width(6.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = artist.name,
                style = TextStyle(color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (artist.albumTitle.isNotEmpty()) {
                Text(
                    text = artist.albumTitle,
                    style = TextStyle(color = textSecondary, fontSize = 9.sp),
                    maxLines = 1,
                )
            }
            if (imminent) {
                val label = when (daysUntil.toInt()) {
                    0    -> "Today"
                    1    -> "Tomorrow"
                    else -> "${daysUntil}d"
                }
                Text(label, style = TextStyle(color = imminentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
