package com.jacobleighty.musictracker.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

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
