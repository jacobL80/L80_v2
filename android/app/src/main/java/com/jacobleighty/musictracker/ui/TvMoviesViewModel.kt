package com.jacobleighty.musictracker.ui

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.jacobleighty.musictracker.Constants
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.data.TvShow
import com.jacobleighty.musictracker.widget.TvMoviesWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TvViewType { SCHEDULE, HISTORY }

data class TvMoviesUiState(
    val loading: Boolean = true,
    val fetchError: Boolean = false,
    val toWatch: List<TvShow> = emptyList(),
    val upcoming: List<TvShow> = emptyList(),
    val expected: List<TvShow> = emptyList(),
    val watchlist: List<TvShow> = emptyList(),
    val watched: List<TvShow> = emptyList(),
    val view: TvViewType = TvViewType.SCHEDULE,
    val isEditing: Boolean = false,
    val editToken: String? = null,
    val saveError: String? = null,
    val showPasswordDialog: Boolean = false,
    val editingShow: TvShow? = null,
    val pendingAdd: Boolean = false,
    val pendingWatch: TvShow? = null,
)

class TvMoviesViewModel(app: Application) : AndroidViewModel(app) {
    private val api   = ApiService.create()
    private val prefs = app.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TvMoviesUiState())
    val uiState: StateFlow<TvMoviesUiState> = _uiState.asStateFlow()

    init {
        val token = prefs.getString(Constants.PREF_EDIT_TOKEN, null)
        if (token != null) _uiState.update { it.copy(editToken = token, isEditing = true) }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, fetchError = false) }
            runCatching { api.getTvShows() }.onSuccess { shows ->
                _uiState.update { it.copy(loading = false) }
                updateSections(shows)
                warmWidgetCache(shows)
            }.onFailure {
                _uiState.update { it.copy(loading = false, fetchError = true) }
            }
        }
    }

    private fun updateSections(shows: List<TvShow>) {
        val today     = java.time.LocalDate.now()
        val toWatch   = shows.filter { !it.watched && it.date.isNotEmpty() && DateUtils.hasFullDate(it.date)
            && !DateUtils.parseDate(it.date).isAfter(today) }
            .sortedBy { DateUtils.parseDate(it.date) }
        val upcoming  = shows.filter { !it.watched && it.date.isNotEmpty() && DateUtils.hasFullDate(it.date)
            && DateUtils.parseDate(it.date).isAfter(today) }
            .sortedBy { DateUtils.parseDate(it.date) }
        val expected  = shows.filter { !it.watched && it.date.isNotEmpty() && !DateUtils.hasFullDate(it.date) }
            .sortedBy { DateUtils.getYear(it.date).toIntOrNull() ?: 9999 }
        val watchlist = shows.filter { !it.watched && it.date.isEmpty() }
        val watched   = shows.filter { it.watched }
        _uiState.update { it.copy(toWatch = toWatch, upcoming = upcoming, expected = expected, watchlist = watchlist, watched = watched) }
    }

    fun handleAddNew() {
        if (_uiState.value.isEditing) _uiState.update { it.copy(editingShow = TvShow()) }
        else _uiState.update { it.copy(pendingAdd = true, showPasswordDialog = true) }
    }

    fun enterEditMode(password: String) {
        prefs.edit().putString(Constants.PREF_EDIT_TOKEN, password).apply()
        val pending      = _uiState.value.pendingAdd
        val pendingWatch = _uiState.value.pendingWatch
        _uiState.update { it.copy(editToken = password, isEditing = true, showPasswordDialog = false,
            pendingAdd = false, pendingWatch = null,
            editingShow = if (pending) TvShow() else null) }
        if (pendingWatch != null) markWatched(pendingWatch)
    }

    fun exitEditMode() {
        prefs.edit().remove(Constants.PREF_EDIT_TOKEN).apply()
        _uiState.update { it.copy(editToken = null, isEditing = false) }
    }

    fun openEdit(show: TvShow) = _uiState.update { it.copy(editingShow = show) }
    fun closeEdit() = _uiState.update { it.copy(editingShow = null, saveError = null) }
    fun dismissPasswordDialog() = _uiState.update { it.copy(showPasswordDialog = false, pendingAdd = false, pendingWatch = null) }

    fun handleMarkWatched(show: TvShow) {
        if (_uiState.value.isEditing) markWatched(show)
        else _uiState.update { it.copy(pendingWatch = show, showPasswordDialog = true) }
    }
    fun setView(v: TvViewType) = _uiState.update { it.copy(view = v) }

    fun saveShow(show: TvShow) {
        val token = _uiState.value.editToken ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(saveError = null) }
            val result = runCatching {
                if (show.id == 0) {
                    val res = api.createTvShow(token, show)
                    if (res.code() == 401) error("UNAUTHORIZED")
                    res.body() ?: error("Empty response")
                } else {
                    val res = api.updateTvShow(show.id, token, show)
                    if (res.code() == 401) error("UNAUTHORIZED")
                    res.body() ?: error("Empty response")
                }
            }
            result.onSuccess { saved ->
                val all = allShows().let { list ->
                    if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it }
                    else list + saved
                }
                _uiState.update { it.copy(editingShow = null) }
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

    fun deleteShow(id: Int) {
        val token = _uiState.value.editToken ?: return
        viewModelScope.launch {
            runCatching { api.deleteTvShow(id, token) }.onSuccess {
                val all = allShows().filter { it.id != id }
                _uiState.update { it.copy(editingShow = null) }
                updateSections(all)
            }
        }
    }

    fun markWatched(show: TvShow) = saveShow(show.copy(watched = true))

    private fun allShows() = with(_uiState.value) { toWatch + upcoming + expected + watchlist + watched }

    private fun warmWidgetCache(shows: List<TvShow>) {
        val ctx = getApplication<Application>()
        val upcoming = shows
            .filter { !it.watched && it.date.isNotEmpty() && DateUtils.hasFullDate(it.date) }
            .sortedBy { DateUtils.parseDate(it.date) }
            .take(10)
        ctx.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
            .edit().putString("tvmovies_json", Gson().toJson(upcoming)).apply()
        viewModelScope.launch {
            val manager = GlanceAppWidgetManager(ctx)
            manager.getGlanceIds(TvMoviesWidget::class.java).forEach { TvMoviesWidget().update(ctx, it) }
        }
    }
}
