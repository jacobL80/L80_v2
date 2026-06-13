package com.jacobleighty.musictracker.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jacobleighty.musictracker.Constants
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.RunningWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RunningUiState(
    val loading: Boolean = true,
    val fetchError: Boolean = false,
    val allWeeks: List<RunningWeek> = emptyList(),   // all years, for chart
    val filteredWeeks: List<RunningWeek> = emptyList(),
    val selectedYear: Int? = null,
    val availableYears: List<Int> = emptyList(),
    val isEditing: Boolean = false,
    val editToken: String? = null,
    val saveError: String? = null,
    val showPasswordDialog: Boolean = false,
    val showAddModal: Boolean = false,
    val pendingAdd: Boolean = false,
    val expandedWeekStart: String? = null,
)

class RunningViewModel(app: Application) : AndroidViewModel(app) {
    private val api   = ApiService.create()
    private val prefs = app.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(RunningUiState())
    val uiState: StateFlow<RunningUiState> = _uiState.asStateFlow()

    init {
        val token = prefs.getString(Constants.PREF_EDIT_TOKEN, null)
        if (token != null) _uiState.update { it.copy(editToken = token, isEditing = true) }
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, fetchError = false) }
            runCatching { api.getRunningWeeks() }.onSuccess { weeks ->
                val years = weeks.map { it.year }.distinct().sortedDescending()
                _uiState.update { it.copy(loading = false, allWeeks = weeks, filteredWeeks = weeks, availableYears = years) }
            }.onFailure {
                _uiState.update { it.copy(loading = false, fetchError = true) }
            }
        }
    }

    fun setYear(year: Int?) {
        _uiState.update { it.copy(selectedYear = year) }
        viewModelScope.launch {
            val weeks = if (year == null) {
                runCatching { api.getRunningWeeks() }.getOrDefault(emptyList())
            } else {
                runCatching { api.getRunningWeeksByYear(year) }.getOrDefault(emptyList())
            }
            _uiState.update { it.copy(filteredWeeks = weeks) }
        }
    }

    fun handleAddNew() {
        if (_uiState.value.isEditing) _uiState.update { it.copy(showAddModal = true) }
        else _uiState.update { it.copy(pendingAdd = true, showPasswordDialog = true) }
    }

    fun enterEditMode(password: String) {
        prefs.edit().putString(Constants.PREF_EDIT_TOKEN, password).apply()
        val pending = _uiState.value.pendingAdd
        _uiState.update { it.copy(editToken = password, isEditing = true, showPasswordDialog = false, pendingAdd = false,
            showAddModal = pending) }
    }

    fun exitEditMode() {
        prefs.edit().remove(Constants.PREF_EDIT_TOKEN).apply()
        _uiState.update { it.copy(editToken = null, isEditing = false) }
    }

    fun dismissPasswordDialog() = _uiState.update { it.copy(showPasswordDialog = false, pendingAdd = false) }
    fun closeAddModal() = _uiState.update { it.copy(showAddModal = false, saveError = null) }
    fun toggleExpanded(weekStart: String) = _uiState.update {
        it.copy(expandedWeekStart = if (it.expandedWeekStart == weekStart) null else weekStart)
    }

    fun addEntry(miles: Float, date: LocalDate) {
        val token = _uiState.value.editToken ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(saveError = null) }
            runCatching {
                val res = api.createRunEntry(token, mapOf("miles" to miles.toString(), "date" to date.toString()))
                if (res.code() == 401) error("UNAUTHORIZED")
                res.body() ?: error("Empty response")
            }.onSuccess {
                _uiState.update { it.copy(showAddModal = false) }
                loadAll()
            }.onFailure { err ->
                if (err.message == "UNAUTHORIZED") {
                    prefs.edit().remove(Constants.PREF_EDIT_TOKEN).apply()
                    _uiState.update { it.copy(editToken = null, isEditing = false, saveError = "Incorrect password.") }
                } else {
                    _uiState.update { it.copy(saveError = "Save failed — please try again.") }
                }
            }
        }
    }

    fun deleteEntry(id: Int) {
        val token = _uiState.value.editToken ?: return
        viewModelScope.launch {
            runCatching { api.deleteRunEntry(id, token) }.onSuccess { loadAll() }
        }
    }
}
