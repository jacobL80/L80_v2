package com.jacobleighty.musictracker.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.TvShow
import com.jacobleighty.musictracker.ui.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TvMoviesWidget : GlanceAppWidget() {
    companion object {
        val SINGLE_COLUMN_KEY = booleanPreferencesKey("single_column")
    }

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val upcoming = fetchUpcoming(context)
        provideContent {
            val prefs = currentState<Preferences>()
            val singleColumn = prefs[SINGLE_COLUMN_KEY] ?: false
            val size = LocalSize.current
            val twoColumn = !singleColumn && size.width >= 250.dp
            TvMoviesWidgetContent(upcoming, twoColumn, singleColumn)
        }
    }

    private suspend fun fetchUpcoming(context: Context): List<TvShow> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
        try {
            val shows = ApiService.create().getTvShows()
                .filter { !it.watched && it.date.isNotEmpty() && DateUtils.hasFullDate(it.date) }
                .sortedBy { DateUtils.parseDate(it.date) }
                .take(10)
            prefs.edit().putString("tvmovies_json", Gson().toJson(shows)).apply()
            shows
        } catch (_: Exception) {
            val json = prefs.getString("tvmovies_json", null) ?: return@withContext emptyList()
            try {
                val all: List<TvShow> = Gson().fromJson(json, object : TypeToken<List<TvShow>>() {}.type)
                all.take(10)
            } catch (_: Exception) { emptyList() }
        }
    }
}

class OpenTvMoviesAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("l80://tv")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

class ToggleTvMoviesColumnAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().also {
                it[TvMoviesWidget.SINGLE_COLUMN_KEY] = !(it[TvMoviesWidget.SINGLE_COLUMN_KEY] ?: false)
            }
        }
        TvMoviesWidget().update(context, glanceId)
    }
}

@Composable
private fun TvMoviesWidgetContent(shows: List<TvShow>, twoColumn: Boolean, singleColumn: Boolean) {
    val accent  = ColorProvider(Color(0xFF7C3AED))
    val bgColor = ColorProvider(Color(0xFF0f0f0f))
    val textSec = ColorProvider(Color(0xFF888888))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .background(bgColor)
            .clickable(actionRunCallback<OpenTvMoviesAction>())
            .padding(8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "Upcoming Shows",
                style = TextStyle(color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = if (singleColumn) "⊞" else "⊟",
                style = TextStyle(color = ColorProvider(Color(0xFF555555)), fontSize = 13.sp),
                modifier = GlanceModifier
                    .padding(end = 4.dp)
                    .clickable(actionRunCallback<ToggleTvMoviesColumnAction>()),
            )
        }
        if (shows.isEmpty()) {
            Text("No upcoming shows", style = TextStyle(color = textSec, fontSize = 12.sp))
            return@Column
        }
        if (twoColumn) {
            val chunks = shows.chunked(2)
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(chunks, itemId = { row -> row.first().id.toLong() }) { rowShows ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.Horizontal.Start,
                    ) {
                        rowShows.forEach { show ->
                            TvCard(show, accent, modifier = GlanceModifier.defaultWeight().padding(horizontal = 3.dp), large = false)
                        }
                        if (rowShows.size == 1) Spacer(GlanceModifier.defaultWeight())
                    }
                }
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(shows, itemId = { it.id.toLong() }) { show ->
                    TvCard(show, accent, modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), large = true)
                }
            }
        }
    }
}

@Composable
private fun TvCard(show: TvShow, accent: ColorProvider, modifier: GlanceModifier = GlanceModifier, large: Boolean = true) {
    val (month, day, _) = DateUtils.parseParts(show.date)
    val textPri = ColorProvider(Color(0xFFE8E8E8))
    val textSec = ColorProvider(Color(0xFF888888))

    val dayFontSize   = if (large) 20.sp else 16.sp
    val monthFontSize = if (large) 9.sp  else 7.sp
    val nameFontSize  = if (large) 13.sp else 10.sp
    val subFontSize   = if (large) 11.sp else 9.sp
    val dateColWidth  = if (large) 40.dp else 30.dp

    Column(modifier = modifier) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFF1e1e1e)))
                .cornerRadius(8.dp)
                .padding(6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                modifier = GlanceModifier.width(dateColWidth),
            ) {
                if (month != null) Text(month, style = TextStyle(color = accent, fontSize = monthFontSize, fontWeight = FontWeight.Bold))
                if (day   != null) Text(day.toString(), style = TextStyle(color = textPri, fontSize = dayFontSize, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(show.programName, style = TextStyle(color = textPri, fontSize = nameFontSize, fontWeight = FontWeight.Bold), maxLines = 1)
                if (show.service.isNotEmpty()) {
                    Text(show.service, style = TextStyle(color = textSec, fontSize = subFontSize), maxLines = 1)
                }
            }
        }
    }
}
