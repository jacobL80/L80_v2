package com.jacobleighty.musictracker.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.jacobleighty.musictracker.Constants
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.Concert
import com.jacobleighty.musictracker.widget.updateAllWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ConcertViewType { SCHEDULE, HISTORY, BANDS }

enum class BandSort(val label: String) {
    NAME_ASC("A–Z"),
    NAME_DESC("Z–A"),
    RECENT_DESC("Most recent"),
    RECENT_ASC("Least recent"),
    COUNT_DESC("Most concerts"),
    COUNT_ASC("Fewest concerts"),
}

data class ConcertsUiState(
    val loading: Boolean = true,
    val fetchError: Boolean = false,
    val upcoming: List<Concert> = emptyList(),
    val attended: List<Concert> = emptyList(),
    val view: ConcertViewType = ConcertViewType.SCHEDULE,
    val bandSort: BandSort = BandSort.NAME_ASC,
    val expandedBands: Set<String> = emptySet(),
    val isEditing: Boolean = false,
    val editToken: String? = null,
    val saveError: String? = null,
    val editingConcert: Concert? = null,
)

class ConcertsViewModel(app: Application) : AndroidViewModel(app) {
    private val api = ApiService.create()
    private val prefs = app.getSharedPreferences(Constants.NOTIF_PREFS, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ConcertsUiState(bandSort = savedBandSort()))
    val uiState: StateFlow<ConcertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            EditModeManager.editToken.collect { token ->
                _uiState.update { it.copy(editToken = token, isEditing = token != null) }
            }
        }
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
    }

    fun openEdit(concert: Concert) = _uiState.update { it.copy(editingConcert = concert) }
    fun closeEdit() = _uiState.update { it.copy(editingConcert = null, saveError = null) }
    fun setView(v: ConcertViewType) = _uiState.update { it.copy(view = v) }
    fun toggleBand(key: String) = _uiState.update {
        it.copy(expandedBands = if (key in it.expandedBands) it.expandedBands - key else it.expandedBands + key)
    }

    // The band sort is remembered across launches
    fun setBandSort(sort: BandSort) {
        _uiState.update { it.copy(bandSort = sort) }
        prefs.edit().putString(Constants.PREF_BAND_SORT, sort.name).apply()
    }

    private fun savedBandSort(): BandSort {
        val saved = prefs.getString(Constants.PREF_BAND_SORT, null)
        return BandSort.entries.find { it.name == saved } ?: BandSort.NAME_ASC
    }

    fun saveConcert(concert: Concert) {
        val token = _uiState.value.editToken ?: return
        // Unlike artists and shows, a new concert is never merged into an existing one by name:
        // a band can have any number of concerts
        val normalized = concert.copy(date = DateUtils.lockYearIfMD(concert.date))
        viewModelScope.launch {
            _uiState.update { it.copy(saveError = null) }
            val result = runCatching {
                if (normalized.id == 0) {
                    val res = api.createConcert(token, normalized)
                    if (res.code() == 401) error("UNAUTHORIZED")
                    res.body() ?: error("Empty response")
                } else {
                    val res = api.updateConcert(normalized.id, token, normalized)
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
                DataChangeEvents.emit()
                warmWidgetCache(all)
            }.onFailure { err ->
                if (err.message == "UNAUTHORIZED") {
                    EditModeManager.exitEditMode()
                    _uiState.update { it.copy(saveError = "Incorrect password.") }
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
                DataChangeEvents.emit()
                warmWidgetCache(all)
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
        viewModelScope.launch(Dispatchers.IO) {
            ctx.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
                .edit().putString("concerts_json", Gson().toJson(upcoming)).commit()
            updateAllWidgets(ctx)
        }
    }
}
