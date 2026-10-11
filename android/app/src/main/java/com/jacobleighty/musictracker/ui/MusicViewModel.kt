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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val acquireLock = Mutex()

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
        val toSave = if (artist.id == 0) placeNewEntry(artist) else artist
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
                warmWidgetCache(allArtists)
            }.onFailure(::reportSaveFailure)
        }
    }

    private fun reportSaveFailure(err: Throwable) {
        if (err.message == "UNAUTHORIZED") {
            EditModeManager.exitEditMode()
            _uiState.update { it.copy(saveError = "Incorrect password — re-enable Edit Mode in Settings.") }
        } else {
            _uiState.update { it.copy(saveError = "Save failed — please try again.") }
        }
    }

    /**
     * Where a newly added entry goes when the band is already listed. A band can have more than
     * one release pending, so the new entry only updates an existing one if it is that same
     * release, or if that entry has nothing definite pending yet. Otherwise it is added beside it.
     */
    private fun placeNewEntry(new: Artist): Artist {
        val sameBand = allArtistsList().filter { it.name.equals(new.name, ignoreCase = true) }
        val existing = when {
            sameBand.isEmpty()        -> return new
            new.nextRelease.isBlank() -> sameBand.first()
            else -> sameBand.firstOrNull { isSameRelease(it, new) }
                ?: sameBand.firstOrNull { it.nextRelease.isBlank() }
                // an untitled "expected sometime" entry stands in for whatever gets announced
                ?: sameBand.firstOrNull { it.albumTitle.isBlank() && !DateUtils.hasFullDate(it.nextRelease) }
        }
        if (existing == null) {
            val known = sameBand.first()
            return new.copy(
                lastRelease = new.lastRelease.ifBlank { known.lastRelease },
                url = new.url.ifBlank { known.url },
                incompleteCollection = new.incompleteCollection || known.incompleteCollection,
            )
        }
        return existing.copy(
            name = new.name,
            albumTitle = new.albumTitle.ifBlank { existing.albumTitle },
            nextRelease = new.nextRelease.ifBlank { existing.nextRelease },
            lastRelease = new.lastRelease.ifBlank { existing.lastRelease },
            notes = new.notes.ifBlank { existing.notes },
            url = new.url.ifBlank { existing.url },
            hiatus = new.hiatus || existing.hiatus,
            incompleteCollection = new.incompleteCollection || existing.incompleteCollection,
        )
    }

    // Same album title; or, when a title is missing, the same date
    private fun isSameRelease(existing: Artist, new: Artist): Boolean = when {
        existing.nextRelease.isBlank() -> false
        existing.albumTitle.isNotBlank() && new.albumTitle.isNotBlank() ->
            existing.albumTitle.trim().equals(new.albumTitle.trim(), ignoreCase = true)
        else -> DateUtils.hasFullDate(existing.nextRelease) == DateUtils.hasFullDate(new.nextRelease) &&
            DateUtils.parseDate(existing.nextRelease) == DateUtils.parseDate(new.nextRelease)
    }

    fun deleteArtist(id: Int) {
        val token = _uiState.value.editToken ?: return
        viewModelScope.launch {
            repo.deleteArtist(id, token).onSuccess {
                val allArtists = allArtistsList().filter { it.id != id }
                _uiState.update { it.copy(editingArtist = null) }
                updateSections(allArtists)
                DataChangeEvents.emit()
                warmWidgetCache(allArtists)
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
            // One at a time, each against the latest data: two entries for the same band fold into
            // each other, so the second has to see what the first one did.
            acquireLock.withLock {
                val entry = allArtistsList().find { it.id == artist.id && it.nextRelease.isNotEmpty() } ?: return@withLock
                repo.acquireArtist(entry, token).onSuccess { logged ->
                    _uiState.update { it.copy(history = listOf(logged) + it.history) }
                }
                _uiState.update { it.copy(saveError = null) }
                val others = allArtistsList().filter { it.id != entry.id && it.name.equals(entry.name, ignoreCase = true) }
                runCatching {
                    if (others.isEmpty()) {
                        put(entry.copy(nextRelease = "", albumTitle = "",
                            lastRelease = laterRelease(entry.lastRelease, entry.nextRelease)), token)
                    } else {
                        foldRelease(entry, others, token)
                    }
                }.onFailure(::reportSaveFailure)
                DataChangeEvents.emit()
                warmWidgetCache(allArtistsList())
            }
        }
    }

    /**
     * A band with several releases pending has an entry for each. When one comes out it is folded
     * into the band's other entries and removed, rather than leaving the band listed twice.
     */
    private suspend fun foldRelease(released: Artist, others: List<Artist>, token: String) {
        others.forEach { other ->
            put(other.copy(
                lastRelease = laterRelease(other.lastRelease, released.nextRelease),
                url = other.url.ifBlank { released.url },
                notes = other.notes.ifBlank { released.notes },
                incompleteCollection = other.incompleteCollection || released.incompleteCollection,
            ), token)
        }
        repo.deleteArtist(released.id, token).getOrThrow()
        updateSections(allArtistsList().filter { it.id != released.id })
    }

    private suspend fun put(artist: Artist, token: String) {
        updateSections(buildUpdatedList(repo.saveArtist(artist, token).getOrThrow()))
    }

    // A band's last release only ever moves forward
    private fun laterRelease(current: String, released: String): String = DateUtils.lockYearIfMD(
        if (current.isNotBlank() && DateUtils.parseDate(current).isAfter(DateUtils.parseDate(released))) current else released
    )

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
        viewModelScope.launch(Dispatchers.IO) {
            ctx.getSharedPreferences("widget_cache", Context.MODE_PRIVATE)
                .edit().putString("artists_json", Gson().toJson(upcoming)).commit()
            updateAllWidgets(ctx)
        }
    }
}
