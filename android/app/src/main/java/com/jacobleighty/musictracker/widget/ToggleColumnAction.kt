package com.jacobleighty.musictracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

class ToggleColumnAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().also {
                it[UpcomingWidget.SINGLE_COLUMN_KEY] = !(it[UpcomingWidget.SINGLE_COLUMN_KEY] ?: false)
            }
        }
        UpcomingWidget().update(context, glanceId)
    }
}
