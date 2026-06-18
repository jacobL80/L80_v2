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
import androidx.glance.action.actionParametersOf
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
import com.jacobleighty.musictracker.data.AllItem
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.ui.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TYPE_COLORS = mapOf(
    "music"   to Color(0xFFEC6F00),
    "concert" to Color(0xFF1696B6),
    "tv"      to Color(0xFF7C3AED),
)
private val TYPE_LABELS = mapOf(
    "music"   to "Music",
    "concert" to "Concert",
    "tv"      to "TV",
)

private fun badgeLabel(item: AllItem): String =
    if (item.type == "tv" && item.showType.isNotEmpty()) item.showType
    else TYPE_LABELS[item.type] ?: item.type

class AllWidget : GlanceAppWidget() {
    companion object {
        val SINGLE_COLUMN_KEY = booleanPreferencesKey("single_column")
    }

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = runCatching { fetchUpcoming(context) }.getOrDefault(emptyList())
        if (items.isEmpty()) return  // keep last rendered frame rather than blanking
        provideContent {
            val prefs = currentState<Preferences>()
            val singleColumn = prefs[SINGLE_COLUMN_KEY] ?: false
            val size = LocalSize.current
            val twoColumn = !singleColumn && size.width >= 250.dp
            AllWidgetContent(items, twoColumn, singleColumn)
        }
    }

    private suspend fun fetchUpcoming(context: Context): List<AllItem> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
        val cached: List<AllItem>? = prefs.getString("all_items_json", null)?.let { json ->
            try { Gson().fromJson(json, object : TypeToken<List<AllItem>>() {}.type) }
            catch (_: Exception) { null }
        }?.takeIf { (it as List<*>).isNotEmpty() }
        cached ?: try {
            val today = java.time.LocalDate.now()
            val items = ApiService.create().getAllItems()
                .filter { DateUtils.hasFullDate(it.date) && DateUtils.parseDate(it.date) >= today }
                .sortedBy { DateUtils.parseDate(it.date) }
                .take(10)
            if (items.isNotEmpty()) prefs.edit().putString("all_items_json", Gson().toJson(items)).commit()
            items
        } catch (_: Exception) { emptyList() }
    }
}

private val ITEM_TYPE_KEY = ActionParameters.Key<String>("item_type")

class OpenAllAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("l80://all")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

class OpenItemTypeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val host = when (parameters[ITEM_TYPE_KEY]) {
            "music"   -> "music"
            "concert" -> "concerts"
            "tv"      -> "tv"
            else      -> "all"
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("l80://$host")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

class ToggleAllColumnAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().also {
                it[AllWidget.SINGLE_COLUMN_KEY] = !(it[AllWidget.SINGLE_COLUMN_KEY] ?: false)
            }
        }
        AllWidget().update(context, glanceId)
    }
}

@Composable
private fun AllWidgetContent(items: List<AllItem>, twoColumn: Boolean, singleColumn: Boolean) {
    val bgColor = ColorProvider(Color(0xFF0f0f0f))
    val textSec = ColorProvider(Color(0xFF888888))
    val accent  = ColorProvider(Color(0xFFEC6F00))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .background(bgColor)
            .clickable(actionRunCallback<OpenAllAction>())
            .padding(8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "All Upcoming",
                style = TextStyle(color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = if (singleColumn) "⊞" else "⊟",
                style = TextStyle(color = ColorProvider(Color(0xFF555555)), fontSize = 13.sp),
                modifier = GlanceModifier
                    .padding(end = 4.dp)
                    .clickable(actionRunCallback<ToggleAllColumnAction>()),
            )
        }
        if (items.isEmpty()) {
            Text("No upcoming items", style = TextStyle(color = textSec, fontSize = 12.sp))
            return@Column
        }
        if (twoColumn) {
            val chunks = items.chunked(2)
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(chunks, itemId = { row -> row.first().id.toLong() }) { rowItems ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalAlignment = Alignment.Horizontal.Start,
                    ) {
                        rowItems.forEach { item ->
                            AllWidgetCard(item, modifier = GlanceModifier.defaultWeight().padding(horizontal = 3.dp), large = false)
                        }
                        if (rowItems.size == 1) Spacer(GlanceModifier.defaultWeight())
                    }
                }
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(items, itemId = { it.id.toLong() }) { item ->
                    AllWidgetCard(item, modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), large = true)
                }
            }
        }
    }
}

@Composable
private fun AllWidgetCard(item: AllItem, modifier: GlanceModifier = GlanceModifier, large: Boolean = true) {
    val typeColor     = TYPE_COLORS[item.type] ?: Color(0xFFEC6F00)
    val typeLabel     = badgeLabel(item)
    val (month, day, _) = DateUtils.parseParts(item.date)
    val daysUntil     = DateUtils.daysUntil(item.date)
    val imminent      = daysUntil in 0..6
    val cardBg        = if (imminent) ColorProvider(typeColor.copy(alpha = 0.15f)) else ColorProvider(Color(0xFF1e1e1e))
    val textPrimary   = ColorProvider(Color(0xFFE8E8E8))
    val textSecondary = ColorProvider(Color(0xFF888888))
    val accentProvider = ColorProvider(typeColor)

    val dayFontSize   = if (large) 20.sp else 16.sp
    val monthFontSize = if (large) 8.sp  else 7.sp
    val nameFontSize  = if (large) 13.sp else 10.sp
    val subFontSize   = if (large) 11.sp else 9.sp
    val labelFontSize = if (large) 10.sp else 8.sp
    val dateColWidth  = if (large) 36.dp else 28.dp
    val stripeHeight  = if (large) 44.dp else 36.dp

    Column(modifier = modifier) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(cardBg)
                .cornerRadius(8.dp)
                .clickable(actionRunCallback<OpenItemTypeAction>(actionParametersOf(ITEM_TYPE_KEY to item.type)))
                .padding(6.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(3.dp)
                    .height(stripeHeight)
                    .background(accentProvider)
                    .cornerRadius(2.dp),
            ) {}
            Spacer(GlanceModifier.width(6.dp))
            Column(
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                modifier = GlanceModifier.width(dateColWidth),
            ) {
                if (month != null) {
                    Text(month, style = TextStyle(color = accentProvider, fontSize = monthFontSize, fontWeight = FontWeight.Bold))
                }
                if (day != null) {
                    Text(day.toString(), style = TextStyle(color = textPrimary, fontSize = dayFontSize, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(GlanceModifier.width(8.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = item.title,
                    style = TextStyle(color = textPrimary, fontSize = nameFontSize, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                if (item.subtitle.isNotEmpty()) {
                    Text(
                        text = item.subtitle,
                        style = TextStyle(color = textSecondary, fontSize = subFontSize),
                        maxLines = 1,
                    )
                }
                if (imminent) {
                    val label = when (daysUntil.toInt()) {
                        0    -> "Today"
                        1    -> "Tomorrow"
                        else -> "${daysUntil}d"
                    }
                    Text(label, style = TextStyle(color = accentProvider, fontSize = labelFontSize, fontWeight = FontWeight.Bold))
                }
            }
            if (large) {
                Text(
                    text = typeLabel,
                    style = TextStyle(color = accentProvider, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(end = 2.dp),
                )
            }
        }
    }
}
