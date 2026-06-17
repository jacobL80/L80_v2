package com.jacobleighty.musictracker.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.ui.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val api = ApiService.create()
        val prefs = ctx.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
        val manager = GlanceAppWidgetManager(ctx)

        runCatching {
            val weeks = api.getRunningWeeks().sortedBy { it.weekStart }
            prefs.edit().putString("running_weeks_json", Gson().toJson(weeks)).apply()
            manager.getGlanceIds(RunningWidget::class.java)
                .forEach { RunningWidget().update(ctx, it) }
        }

        runCatching {
            val artists = api.getArtists()
                .filter { it.nextRelease.isNotEmpty() && DateUtils.hasFullDate(it.nextRelease) }
                .sortedBy { DateUtils.parseDate(it.nextRelease) }
            prefs.edit().putString("artists_json", Gson().toJson(artists)).apply()
            manager.getGlanceIds(UpcomingWidget::class.java)
                .forEach { UpcomingWidget().update(ctx, it) }
        }

        runCatching {
            val today = java.time.LocalDate.now()
            val concerts = api.getConcerts()
                .filter { !it.attended && it.date.isNotEmpty() && DateUtils.hasFullDate(it.date) }
                .sortedBy { DateUtils.parseDate(it.date) }
                .take(10)
            prefs.edit().putString("concerts_json", Gson().toJson(concerts)).apply()
            manager.getGlanceIds(ConcertsWidget::class.java)
                .forEach { ConcertsWidget().update(ctx, it) }
        }

        runCatching {
            val shows = api.getTvShows()
                .filter { !it.watched && it.date.isNotEmpty() && DateUtils.hasFullDate(it.date) }
                .sortedBy { DateUtils.parseDate(it.date) }
                .take(10)
            prefs.edit().putString("tvmovies_json", Gson().toJson(shows)).apply()
            manager.getGlanceIds(TvMoviesWidget::class.java)
                .forEach { TvMoviesWidget().update(ctx, it) }
        }

        runCatching {
            val today = java.time.LocalDate.now()
            val items = api.getAllItems()
                .filter { DateUtils.hasFullDate(it.date) && DateUtils.parseDate(it.date) >= today }
                .sortedBy { DateUtils.parseDate(it.date) }
                .take(10)
            prefs.edit().putString("all_items_json", Gson().toJson(items)).apply()
            manager.getGlanceIds(AllWidget::class.java)
                .forEach { AllWidget().update(ctx, it) }
        }

        Result.success()
    }
}
