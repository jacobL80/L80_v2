package com.jacobleighty.musictracker.data

import com.google.gson.annotations.SerializedName

data class Concert(
    val id: Int = 0,
    val band: String = "",
    @SerializedName("tourName") val tourName: String = "",
    val venue: String = "",
    val date: String = "",
    val notes: String = "",
    val attended: Boolean = false,
    val attendees: String = "",   // comma-separated names
)
