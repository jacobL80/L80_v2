package com.jacobleighty.musictracker.data

import com.google.gson.annotations.SerializedName

data class HistoryEntry(
    val id: Int = 0,
    @SerializedName("artist_name")  val artistName: String = "",
    @SerializedName("album_title")  val albumTitle: String = "",
    @SerializedName("release_date") val releaseDate: String = "",
    @SerializedName("acquired_at")  val acquiredAt: String = "",
)
