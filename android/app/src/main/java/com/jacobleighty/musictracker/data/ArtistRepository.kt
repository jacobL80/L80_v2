package com.jacobleighty.musictracker.data

import com.jacobleighty.musictracker.ui.DateUtils

class ArtistRepository(private val api: ApiService = ApiService.create()) {

    suspend fun fetchArtists(): Result<List<Artist>> = runCatching { api.getArtists() }

    suspend fun fetchHistory(): Result<List<HistoryEntry>> = runCatching { api.getHistory() }

    suspend fun saveArtist(artist: Artist, token: String): Result<Artist> = runCatching {
        if (artist.id == 0) {
            val res = api.createArtist(token, artist.withConfirmed())
            if (res.code() == 401) error("UNAUTHORIZED")
            res.body() ?: error("Empty response")
        } else {
            val res = api.updateArtist(artist.id, token, artist.withConfirmed())
            if (res.code() == 401) error("UNAUTHORIZED")
            res.body() ?: error("Empty response")
        }
    }

    suspend fun deleteArtist(id: Int, token: String): Result<Unit> = runCatching {
        val res = api.deleteArtist(id, token)
        if (res.code() == 401) error("UNAUTHORIZED")
    }

    suspend fun acquireArtist(artist: Artist, token: String): Result<HistoryEntry> = runCatching {
        val body = mapOf(
            "artist_name"  to artist.name,
            "album_title"  to artist.albumTitle,
            "release_date" to artist.nextRelease,
        )
        val res = api.createHistoryEntry(token, body)
        if (res.code() == 401) error("UNAUTHORIZED")
        res.body() ?: error("Empty response")
    }

    private fun Artist.withConfirmed() = copy(confirmed = DateUtils.hasFullDate(nextRelease))
}
