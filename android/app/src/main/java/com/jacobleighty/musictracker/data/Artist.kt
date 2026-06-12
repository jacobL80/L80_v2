package com.jacobleighty.musictracker.data

import com.google.gson.annotations.SerializedName

data class Artist(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("lastRelease")     val lastRelease: String = "",
    @SerializedName("nextRelease")     val nextRelease: String = "",
    @SerializedName("albumTitle")      val albumTitle: String = "",
    val confirmed: Boolean = false,
    @SerializedName("incompleteCollection") val incompleteCollection: Boolean = false,
    val notes: String = "",
    val url: String = "",
    val hiatus: Boolean = false,
)
