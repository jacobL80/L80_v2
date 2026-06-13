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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.Concert
import com.jacobleighty.musictracker.ui.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConcertsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val upcoming = fetchUpcoming(context)
        provideContent { ConcertsWidgetContent(upcoming) }
    }

    private suspend fun fetchUpcoming(context: Context): List<Concert> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
        try {
            val concerts = ApiService.create().getConcerts()
                .filter { !it.attended && it.date.isNotEmpty() && DateUtils.hasFullDate(it.date) }
                .sortedBy { DateUtils.parseDate(it.date) }
            prefs.edit().putString("concerts_json", Gson().toJson(concerts)).apply()
            concerts
        } catch (_: Exception) {
            val json = prefs.getString("concerts_json", null) ?: return@withContext emptyList()
            try { Gson().fromJson(json, object : TypeToken<List<Concert>>() {}.type) }
            catch (_: Exception) { emptyList() }
        }
    }
}

class OpenConcertsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("l80://concerts")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

@Composable
private fun ConcertsWidgetContent(concerts: List<Concert>) {
    val accent    = ColorProvider(Color(0xFF1696B6))
    val bgColor   = ColorProvider(Color(0xFF0f0f0f))
    val textPri   = ColorProvider(Color(0xFFE8E8E8))
    val textSec   = ColorProvider(Color(0xFF888888))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .background(bgColor)
            .clickable(actionRunCallback<OpenConcertsAction>())
            .padding(8.dp),
    ) {
        Text(
            text = "Upcoming Concerts",
            style = TextStyle(color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(start = 6.dp, top = 8.dp, bottom = 6.dp),
        )
        if (concerts.isEmpty()) {
            Text("No upcoming concerts", style = TextStyle(color = textSec, fontSize = 12.sp))
            return@Column
        }
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(concerts, itemId = { it.id.toLong() }) { concert ->
                val (month, day, _) = DateUtils.parseParts(concert.date)
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(ColorProvider(Color(0xFF1e1e1e))).cornerRadius(8.dp).padding(6.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Column(
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        modifier = GlanceModifier.width(40.dp),
                    ) {
                        if (month != null) Text(month, style = TextStyle(color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                        if (day   != null) Text(day.toString(), style = TextStyle(color = textPri, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(concert.band, style = TextStyle(color = textPri, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                        if (concert.venue.isNotEmpty()) {
                            Text(concert.venue, style = TextStyle(color = textSec, fontSize = 11.sp), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
