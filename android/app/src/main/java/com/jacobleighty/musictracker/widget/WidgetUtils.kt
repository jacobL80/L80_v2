package com.jacobleighty.musictracker.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

suspend fun updateAllWidgets(ctx: Context) {
    val manager = GlanceAppWidgetManager(ctx)
    manager.getGlanceIds(RunningWidget::class.java).forEach { RunningWidget().update(ctx, it) }
    manager.getGlanceIds(UpcomingWidget::class.java).forEach { UpcomingWidget().update(ctx, it) }
    manager.getGlanceIds(ConcertsWidget::class.java).forEach { ConcertsWidget().update(ctx, it) }
    manager.getGlanceIds(TvMoviesWidget::class.java).forEach { TvMoviesWidget().update(ctx, it) }
    manager.getGlanceIds(AllWidget::class.java).forEach { AllWidget().update(ctx, it) }
}
