package com.jacobleighty.musictracker.data

import com.google.gson.annotations.SerializedName

data class RunningEntry(
    val id: Int = 0,
    val date: String = "",   // YYYY-MM-DD
    val miles: Float = 0f,
)

data class RunningWeek(
    @SerializedName("weekStart") val weekStart: String = "",  // YYYY-MM-DD Monday
    val year: Int = 0,
    val total: Float = 0f,
    val mon: Float = 0f,
    val tue: Float = 0f,
    val wed: Float = 0f,
    val thu: Float = 0f,
    val fri: Float = 0f,
    val sat: Float = 0f,
    val sun: Float = 0f,
    val entries: List<RunningDayEntry> = emptyList(),
)

data class RunningDayEntry(
    val id: Int = 0,
    val date: String = "",
    val miles: Float = 0f,
)
