package com.jacobleighty.musictracker.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jacobleighty.musictracker.MainActivity
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.Artist
import com.jacobleighty.musictracker.ui.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpcomingWidget : GlanceAppWidget() {

    companion object {
        val SINGLE_COLUMN_KEY = booleanPreferencesKey("single_column")
    }

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val upcoming = runCatching { fetchUpcoming(context) }.getOrDefault(emptyList())
        if (upcoming.isEmpty()) return  // keep last rendered frame rather than blanking
        provideContent {
            val prefs = currentState<Preferences>()
            val singleColumn = prefs[SINGLE_COLUMN_KEY] ?: false
            val size = LocalSize.current
            val twoColumn = !singleColumn && size.width >= 250.dp
            WidgetContent(upcoming, twoColumn, singleColumn)
        }
    }

    private suspend fun fetchUpcoming(context: Context): List<Artist> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
        val cached = loadCachedArtists(prefs).takeIf { it.isNotEmpty() }
        cached ?: try {
            val artists = ApiService.create().getArtists()
                .filter { it.nextRelease.isNotEmpty() && DateUtils.hasFullDate(it.nextRelease) }
                .sortedBy { DateUtils.parseDate(it.nextRelease) }
            if (artists.isNotEmpty()) prefs.edit().putString("artists_json", Gson().toJson(artists)).commit()
            artists
        } catch (_: Exception) { emptyList() }
    }

    private fun loadCachedArtists(prefs: SharedPreferences): List<Artist> {
        val json = prefs.getString("artists_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Artist>>() {}.type
            Gson().fromJson(json, type)
        } catch (_: Exception) { emptyList() }
    }
}

@Composable
private fun WidgetContent(artists: List<Artist>, twoColumn: Boolean, singleColumn: Boolean) {
    val bgColor = ColorProvider(Color(0xFF0f0f0f))
    val openApp = actionStartActivity<MainActivity>()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .background(bgColor)
            .clickable(openApp)
            .padding(8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "Upcoming Releases",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFEC6F00)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = if (singleColumn) "⊞" else "⊟",
                style = TextStyle(color = ColorProvider(Color(0xFF555555)), fontSize = 13.sp),
                modifier = GlanceModifier
                    .padding(end = 4.dp)
                    .clickable(actionRunCallback<ToggleColumnAction>()),
            )
        }

        if (artists.isEmpty()) {
            Text(
                text = "No upcoming releases",
                style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 12.sp),
            )
            return@Column
        }

        if (twoColumn) {
            val chunks = artists.chunked(2)
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(chunks, itemId = { row -> row.first().name.hashCode().toLong() }) { rowArtists ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.Horizontal.Start,
                    ) {
                        rowArtists.forEach { a ->
                            WidgetCard(a, modifier = GlanceModifier.defaultWeight().padding(horizontal = 3.dp))
                        }
                        if (rowArtists.size == 1) Spacer(GlanceModifier.defaultWeight())
                    }
                }
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(artists, itemId = { it.name.hashCode().toLong() }) { a ->
                    WidgetCard(a, modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), large = true)
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(artist: Artist, modifier: GlanceModifier = GlanceModifier, large: Boolean = false) {
    val (month, day, year) = DateUtils.parseParts(artist.nextRelease)
    val daysUntil  = DateUtils.daysUntil(artist.nextRelease)
    val imminent   = daysUntil in 0..6

    val cardBg        = if (imminent) ColorProvider(Color(0xFF2E1600)) else ColorProvider(Color(0xFF1e1e1e))
    val accentColor   = ColorProvider(Color(0xFFEC6F00))
    val textPrimary   = ColorProvider(Color(0xFFe8e8e8))
    val textSecondary = ColorProvider(Color(0xFF888888))
    val imminentColor = ColorProvider(Color(0xFFEC6F00))
    val dayColor      = if (imminent) imminentColor else textPrimary

    val dayFontSize   = if (large) 22.sp else 18.sp
    val monthFontSize = if (large) 10.sp else 8.sp
    val yearFontSize  = if (large) 11.sp else 9.sp
    val nameFontSize  = if (large) 17.sp else 11.sp
    val albumFontSize = if (large) 13.sp else 9.sp
    val labelFontSize = if (large) 11.sp else 9.sp
    val dateColWidth  = if (large) 44.dp else 36.dp

    Column(modifier = modifier) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().background(cardBg).cornerRadius(8.dp).padding(6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                modifier = GlanceModifier.width(dateColWidth),
            ) {
                if (month != null) {
                    Text(month, style = TextStyle(color = accentColor, fontSize = monthFontSize, fontWeight = FontWeight.Bold))
                }
                if (day != null) {
                    Text(day.toString(), style = TextStyle(color = dayColor, fontSize = dayFontSize, fontWeight = FontWeight.Bold))
                }
                if (year != null) {
                    Text(year.toString(), style = TextStyle(color = textSecondary, fontSize = yearFontSize))
                }
            }

            Spacer(GlanceModifier.width(6.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = artist.name,
                    style = TextStyle(color = textPrimary, fontSize = nameFontSize, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                if (artist.albumTitle.isNotEmpty()) {
                    Text(
                        text = artist.albumTitle,
                        style = TextStyle(color = textSecondary, fontSize = albumFontSize),
                        maxLines = 1,
                    )
                }
                if (imminent) {
                    val label = when (daysUntil.toInt()) {
                        0    -> "Today"
                        1    -> "Tomorrow"
                        else -> "${daysUntil}d"
                    }
                    Text(label, style = TextStyle(color = imminentColor, fontSize = labelFontSize, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
