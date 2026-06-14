package com.jacobleighty.musictracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jacobleighty.musictracker.data.AllItem
import com.jacobleighty.musictracker.data.ApiService
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
            }.onFailure {
                _uiState.update { it.copy(loading = false, fetchError = true) }
            }
        }
    }
}
