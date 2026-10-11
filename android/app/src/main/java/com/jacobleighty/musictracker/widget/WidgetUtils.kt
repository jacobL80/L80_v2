package com.jacobleighty.musictracker.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * The list cached under [key] in widget_cache, re-emitted every time that entry is rewritten.
 * A missing or unreadable entry is an empty list.
 */
fun <T> widgetCacheFlow(context: Context, key: String, itemClass: Class<T>): Flow<List<T>> {
    val prefs = context.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
    return callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) trySend(Unit)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
        .conflate()
        // Read on each signal rather than sending the value, so a late signal can't deliver old data.
        .map {
            val json = prefs.getString(key, null) ?: return@map emptyList<T>()
            try {
                val type = TypeToken.getParameterized(List::class.java, itemClass).type
                Gson().fromJson<List<T>>(json, type) ?: emptyList()
            } catch (_: Exception) { emptyList() }
        }
        .flowOn(Dispatchers.IO)
}

/**
 * Widget data as state, so the widget redraws when the cache changes.
 *
 * Widgets must read their data through this inside provideContent rather than loading it once in
 * provideGlance. GlanceAppWidget.update() only re-runs provideGlance when the widget has no live
 * session, and a session stays alive for ~45s after it renders (opening the app starts one). While
 * it is alive, update() just recomposes the existing content, so data captured outside the
 * composition stays stale no matter how many times update() is called.
 */
@Composable
fun <T> widgetCacheState(key: String, itemClass: Class<T>, initial: List<T>): State<List<T>> {
    val context = LocalContext.current
    return remember(key) { widgetCacheFlow(context, key, itemClass) }.collectAsState(initial)
}

/**
 * Re-renders every placed widget. This starts a session for any widget that doesn't have one;
 * widgets that do are kept current by [widgetCacheState], so write the cache before calling this.
 */
suspend fun updateAllWidgets(ctx: Context) {
    val manager = GlanceAppWidgetManager(ctx)
    manager.getGlanceIds(RunningWidget::class.java).forEach { RunningWidget().update(ctx, it) }
    manager.getGlanceIds(UpcomingWidget::class.java).forEach { UpcomingWidget().update(ctx, it) }
    manager.getGlanceIds(ConcertsWidget::class.java).forEach { ConcertsWidget().update(ctx, it) }
    manager.getGlanceIds(TvMoviesWidget::class.java).forEach { TvMoviesWidget().update(ctx, it) }
    manager.getGlanceIds(AllWidget::class.java).forEach { AllWidget().update(ctx, it) }
}

private const val WIDGET_REFRESH_WORK_NAME = "widget_refresh"

/**
 * Enqueues a full network-backed widget refresh. Uses REPLACE so bursts of triggers
 * (app foreground + a data mutation landing around the same time) collapse into one run.
 */
fun enqueueWidgetRefresh(context: Context) {
    val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        WIDGET_REFRESH_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        request,
    )
}
