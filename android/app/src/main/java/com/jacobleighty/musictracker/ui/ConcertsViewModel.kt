package com.jacobleighty.musictracker.ui

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.jacobleighty.musictracker.Constants
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.Concert
import com.jacobleighty.musictracker.widget.ConcertsWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ConcertViewType { SCHEDULE, HISTORY }

data class ConcertsUiState(
    val loading: Boolean = true,
    val fetchError: Boolean = false,
    val upcoming: List<Concert> = emptyList(),
    val attended: List<Concert> = emptyList(),
    val view: ConcertViewType = ConcertViewType.SCHEDULE,
    val isEditing: Boolean = false,
    val editToken: String? = null,
    val saveError: String? = null,
    val showPasswordDialog: Boolean = false,
    val editingConcert: Concert? = null,
    val pendingAdd: Boolean = false,
)

class ConcertsViewModel(app: Application) : AndroidViewModel(app) {
    private val api   = ApiService.create()
    private val prefs = app.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ConcertsUiState())
    val uiState: StateFlow<ConcertsUiState> = _uiState.asStateFlow()

    init {
        val token = prefs.getString(Constants.PREF_EDIT_TOKEN, null)
        if (token != null) _uiState.update { it.copy(editToken = token, isEditing = true) }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, fetchError = false) }
            runCatching { api.getConcerts() }.onSuccess { concerts ->
                _uiState.update { it.copy(loading = false) }
                updateSections(concerts)
                warmWidgetCache(concerts)
            }.onFailure {
                _uiState.update { it.copy(loading = false, fetchError = true) }
            }
        }
    }

    private fun updateSections(concerts: List<Concert>) {
        val upcoming = concerts.filter { !it.attended }.sortedWith(
            compareBy(
                { when { it.date.isEmpty() -> 2; DateUtils.hasFullDate(it.date) -> 0; else -> 1 } },
                { if (it.date.isNotEmpty() && DateUtils.hasFullDate(it.date)) DateUtils.parseDate(it.date) else null },
                { DateUtils.getYear(it.date).toIntOrNull() ?: 9999 },
            )
        )
        val attended = concerts.filter { it.attended }
        _uiState.update { it.copy(upcoming = upcoming, attended = attended) }
    }

    fun handleAddNew() {
        if (_uiState.value.isEditing) _uiState.update { it.copy(editingConcert = Concert()) }
        else _uiState.update { it.copy(pendingAdd = true, showPasswordDialog = true) }
    }

    fun enterEditMode(password: String) {
        prefs.edit().putString(Constants.PREF_EDIT_TOKEN, password).apply()
        val pending = _uiState.value.pendingAdd
        _uiState.update { it.copy(editToken = password, isEditing = true, showPasswordDialog = false, pendingAdd = false,
            editingConcert = if (pending) Concert() else null) }
    }

    fun exitEditMode() {
        prefs.edit().remove(Constants.PREF_EDIT_TOKEN).apply()
        _uiState.update { it.copy(editToken = null, isEditing = false) }
    }

    fun openEdit(concert: Concert) = _uiState.update { it.copy(editingConcert = concert) }
    fun closeEdit() = _uiState.update { it.copy(editingConcert = null, saveError = null) }
    fun dismissPasswordDialog() = _uiState.update { it.copy(showPasswordDialog = false, pendingAdd = false) }
    fun setView(v: ConcertViewType) = _uiState.update { it.copy(view = v) }

    fun saveConcert(concert: Concert) {
        val token = _uiState.value.editToken ?: return
        val toSave = if (concert.id == 0) {
            val existing = allConcerts().find { it.band.equals(concert.band, ignoreCase = true) }
            if (existing != null) {
                existing.copy(
                    band = concert.band,
                    tourName = concert.tourName.ifBlank { existing.tourName },
                    venue = concert.venue.ifBlank { existing.venue },
                    date = concert.date.ifBlank { existing.date },
                    notes = concert.notes.ifBlank { existing.notes },
                    attendees = concert.attendees.ifBlank { existing.attendees },
                    attended = concert.attended || existing.attended,
                )
            } else concert
        } else concert
        viewModelScope.launch {
            _uiState.update { it.copy(saveError = null) }
            val result = runCatching {
                if (toSave.id == 0) {
                    val res = api.createConcert(token, toSave)
                    if (res.code() == 401) error("UNAUTHORIZED")
                    res.body() ?: error("Empty response")
                } else {
                    val res = api.updateConcert(toSave.id, token, toSave)
                    if (res.code() == 401) error("UNAUTHORIZED")
                    res.body() ?: error("Empty response")
                }
            }
            result.onSuccess { saved ->
                val all = allConcerts().let { list ->
                    if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it }
                    else list + saved
                }
                _uiState.update { it.copy(editingConcert = null) }
                updateSections(all)
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

    fun deleteConcert(id: Int) {
        val token = _uiState.value.editToken ?: return
        viewModelScope.launch {
            runCatching { api.deleteConcert(id, token) }.onSuccess {
                val all = allConcerts().filter { it.id != id }
                _uiState.update { it.copy(editingConcert = null) }
                updateSections(all)
            }
        }
    }

    fun markAttended(concert: Concert) {
        saveConcert(concert.copy(attended = true))
    }

    private fun allConcerts() = with(_uiState.value) { upcoming + attended }

    private fun warmWidgetCache(concerts: List<Concert>) {
        val ctx = getApplication<Application>()
        val upcoming = concerts
            .filter { !it.attended && it.date.isNotEmpty() && DateUtils.hasFullDate(it.date) }
            .sortedBy { DateUtils.parseDate(it.date) }
            .take(10)
        if (upcoming.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ctx.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
                .edit().putString("concerts_json", Gson().toJson(upcoming)).commit()
            val manager = GlanceAppWidgetManager(ctx)
            manager.getGlanceIds(ConcertsWidget::class.java).forEach { ConcertsWidget().update(ctx, it) }
        }
    }
}
