package com.jacobleighty.musictracker.ui

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.jacobleighty.musictracker.data.AllItem
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.widget.AllWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AllUiState(
    val loading: Boolean = true,
    val fetchError: Boolean = false,
    val upcoming: List<AllItem> = emptyList(),
    val past: List<AllItem> = emptyList(),
)

class AllViewModel(app: Application) : AndroidViewModel(app) {
    private val api = ApiService.create()

    private val _uiState = MutableStateFlow(AllUiState())
    val uiState: StateFlow<AllUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun warmWidgetCache(items: List<AllItem>) {
        val ctx = getApplication<Application>()
        val today = java.time.LocalDate.now()
        val upcoming = items
            .filter { DateUtils.hasFullDate(it.date) && DateUtils.parseDate(it.date) >= today }
            .sortedBy { DateUtils.parseDate(it.date) }
            .take(10)
        ctx.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
            .edit().putString("all_items_json", Gson().toJson(upcoming)).apply()
        viewModelScope.launch {
            val manager = GlanceAppWidgetManager(ctx)
            manager.getGlanceIds(AllWidget::class.java).forEach { AllWidget().update(ctx, it) }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, fetchError = false) }
            runCatching { api.getAllItems() }.onSuccess { items ->
                val today = java.time.LocalDate.now()
                val sorted = items.sortedBy { DateUtils.parseDate(it.date) }
                _uiState.update {
                    it.copy(
                        loading  = false,
                        upcoming = sorted.filter { i -> DateUtils.parseDate(i.date) >= today },
                        past     = sorted.filter { i -> DateUtils.parseDate(i.date) < today },
                    )
                }
                warmWidgetCache(items)
            }.onFailure {
                _uiState.update { it.copy(loading = false, fetchError = true) }
            }
        }
    }
}
