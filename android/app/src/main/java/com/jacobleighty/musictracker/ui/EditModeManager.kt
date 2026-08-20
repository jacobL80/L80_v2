package com.jacobleighty.musictracker.ui

import android.content.Context
import android.content.SharedPreferences
import com.jacobleighty.musictracker.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for edit mode, shared by Music, Concerts, TV/Movies, and Running.
 * Toggled from the Settings screen; each screen's ViewModel just observes [editToken].
 */
object EditModeManager {
    private lateinit var prefs: SharedPreferences

    private val _editToken = MutableStateFlow<String?>(null)
    val editToken: StateFlow<String?> = _editToken.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
        _editToken.value = prefs.getString(Constants.PREF_EDIT_TOKEN, null)
    }

    fun enterEditMode(password: String) {
        prefs.edit().putString(Constants.PREF_EDIT_TOKEN, password).apply()
        _editToken.value = password
    }

    fun exitEditMode() {
        prefs.edit().remove(Constants.PREF_EDIT_TOKEN).apply()
        _editToken.value = null
    }
}
