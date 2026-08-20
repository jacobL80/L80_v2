package com.jacobleighty.musictracker.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.jacobleighty.musictracker.data.Artist
import com.jacobleighty.musictracker.data.ArtistRepository
import com.jacobleighty.musictracker.data.HistoryEntry
import com.jacobleighty.musictracker.widget.updateAllWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ViewType { SCHEDULE, HISTORY }

data class MusicUiState(
    val loading: Boolean = true,
    val fetchError: Boolean = false,
    val upcoming: List<Artist> = emptyList(),
    val expected: List<Artist> = emptyList(),
    val watching: List<Artist> = emptyList(),
    val hiatus: List<Artist> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val view: ViewType = ViewType.SCHEDULE,
    val isEditing: Boolean = false,
    val editToken: String? = null,
    val saveError: String? = null,
    val editingArtist: Artist? = null,
)

class MusicViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ArtistRepository()

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

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
            val artistsResult = repo.fetchArtists()
            val historyResult = repo.fetchHistory()
            if (artistsResult.isFailure) {
                _uiState.update { it.copy(loading = false, fetchError = true) }
                return@launch
            }
            val artists = artistsResult.getOrDefault(emptyList())
            val history = historyResult.getOrDefault(emptyList())
            _uiState.update { it.copy(loading = false, history = history) }
            updateSections(artists)
            warmWidgetCache(artists)
        }
    }

    private fun updateSections(artists: List<Artist>) {
        val hiatusYear = LocalDate.now().year - 8

        val upcoming = artists
            .filter { it.nextRelease.isNotEmpty() && DateUtils.hasFullDate(it.nextRelease) }
            .sortedBy { DateUtils.parseDate(it.nextRelease) }

        val expected = artists
            .filter { it.nextRelease.isNotEmpty() && !DateUtils.hasFullDate(it.nextRelease) }
            .sortedWith(compareBy(
                { DateUtils.getYear(it.nextRelease).toIntOrNull() ?: 9999 },
                { if (it.nextRelease.split("/").size >= 2) 0 else 1 },
                { it.nextRelease.split("/").firstOrNull()?.toIntOrNull() ?: 0 },
                { DateUtils.sortKey(it.name) }
            ))

        val watching = artists
            .filter { a ->
                a.nextRelease.isEmpty() && !a.hiatus &&
                    !(a.lastRelease.isNotEmpty() && (DateUtils.getYear(a.lastRelease).toIntOrNull() ?: 9999) <= hiatusYear)
            }
            .sortedWith(compareBy(
                { if (it.lastRelease.isNotEmpty()) DateUtils.parseDate(it.lastRelease) else java.time.LocalDate.MIN },
                { DateUtils.sortKey(it.name) }
            ))

        val hiatus = artists
            .filter { a ->
                a.nextRelease.isEmpty() &&
                    (a.hiatus || (a.lastRelease.isNotEmpty() && (DateUtils.getYear(a.lastRelease).toIntOrNull() ?: 9999) <= hiatusYear))
            }
            .sortedWith(compareBy(
                { DateUtils.getYear(it.lastRelease).toIntOrNull() ?: 0 },
                { DateUtils.sortKey(it.name) }
            ))

        _uiState.update { it.copy(upcoming = upcoming, expected = expected, watching = watching, hiatus = hiatus) }
    }

    fun handleAddNew() {
        if (_uiState.value.isEditing) _uiState.update { it.copy(editingArtist = Artist()) }
    }

    fun openEdit(artist: Artist) = _uiState.update { it.copy(editingArtist = artist) }
    fun closeEdit() = _uiState.update { it.copy(editingArtist = null, saveError = null) }
    fun setView(v: ViewType) = _uiState.update { it.copy(view = v) }

    fun saveArtist(artist: Artist) {
        val token = _uiState.value.editToken ?: return
        val toSave = if (artist.id == 0) {
            val existing = allArtistsList().find { it.name.equals(artist.name, ignoreCase = true) }
            if (existing != null) {
                existing.copy(
                    name = artist.name,
                    albumTitle = artist.albumTitle.ifBlank { existing.albumTitle },
                    nextRelease = artist.nextRelease.ifBlank { existing.nextRelease },
                    lastRelease = artist.lastRelease.ifBlank { existing.lastRelease },
                    notes = artist.notes.ifBlank { existing.notes },
                    url = artist.url.ifBlank { existing.url },
                    hiatus = artist.hiatus || existing.hiatus,
                    incompleteCollection = artist.incompleteCollection || existing.incompleteCollection,
                )
            } else artist
        } else artist
        val normalized = toSave.copy(
            nextRelease = DateUtils.lockYearIfMD(toSave.nextRelease),
            lastRelease = DateUtils.lockYearIfMD(toSave.lastRelease),
        )
        viewModelScope.launch {
            _uiState.update { it.copy(saveError = null) }
            val result = repo.saveArtist(normalized, token)
            result.onSuccess { saved ->
                val allArtists = buildUpdatedList(saved)
                _uiState.update { it.copy(editingArtist = null) }
                updateSections(allArtists)
                DataChangeEvents.emit()
                viewModelScope.launch(Dispatchers.IO) { updateAllWidgets(getApplication()) }
            }.onFailure { err ->
                if (err.message == "UNAUTHORIZED") {
                    EditModeManager.exitEditMode()
                    _uiState.update { it.copy(saveError = "Incorrect password — re-enable Edit Mode in Settings.") }
                } else {
                    _uiState.update { it.copy(saveError = "Save failed — please try again.") }
                }
            }
        }
    }

    fun deleteArtist(id: Int) {
        val token = _uiState.value.editToken ?: return
        viewModelScope.launch {
            repo.deleteArtist(id, token).onSuccess {
                val allArtists = allArtistsList().filter { it.id != id }
                _uiState.update { it.copy(editingArtist = null) }
                updateSections(allArtists)
                DataChangeEvents.emit()
            }.onFailure { err ->
                if (err.message == "UNAUTHORIZED") {
                    EditModeManager.exitEditMode()
                    _uiState.update { it.copy(saveError = "Incorrect password.") }
                }
            }
        }
    }

    fun acquireArtist(artist: Artist) {
        val token = _uiState.value.editToken ?: return
        viewModelScope.launch {
            repo.acquireArtist(artist, token).onSuccess { entry ->
                _uiState.update { it.copy(history = listOf(entry) + it.history) }
            }
            saveArtist(artist.copy(nextRelease = "", albumTitle = "", lastRelease = artist.nextRelease))
        }
    }

    private fun buildUpdatedList(saved: Artist): List<Artist> {
        val current = allArtistsList()
        return if (current.any { it.id == saved.id }) {
            current.map { if (it.id == saved.id) saved else it }
        } else {
            current + saved
        }
    }

    private fun allArtistsList(): List<Artist> {
        val s = _uiState.value
        return s.upcoming + s.expected + s.watching + s.hiatus
    }

    private fun warmWidgetCache(artists: List<Artist>) {
        val ctx = getApplication<Application>()
        val upcoming = artists
            .filter { it.nextRelease.isNotEmpty() && DateUtils.hasFullDate(it.nextRelease) }
            .sortedBy { DateUtils.parseDate(it.nextRelease) }
        if (upcoming.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ctx.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
                .edit().putString("artists_json", Gson().toJson(upcoming)).commit()
            updateAllWidgets(ctx)
        }
    }
}
