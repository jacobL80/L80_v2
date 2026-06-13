package com.jacobleighty.musictracker.data

import com.google.gson.annotations.SerializedName

data class TvShow(
    val id: Int = 0,
    @SerializedName("programName") val programName: String = "",
    val service: String = "",
    val date: String = "",
    val notes: String = "",
    val watched: Boolean = false,
)
