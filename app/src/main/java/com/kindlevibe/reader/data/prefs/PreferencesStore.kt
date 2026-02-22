package com.kindlevibe.reader.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class PreferencesStore(private val dataStore: DataStore<Preferences>) {

    val prefsFlow: Flow<UserPrefs> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            UserPrefs(
                theme      = prefs[PrefKeys.THEME]        ?: "day",
                fontSize   = prefs[PrefKeys.FONT_SIZE]    ?: 1.0f,
                lineHeight = prefs[PrefKeys.LINE_HEIGHT]  ?: 1.4f,
                pageMargins= prefs[PrefKeys.PAGE_MARGINS] ?: 1.0f,
                pagedMode  = prefs[PrefKeys.PAGED_MODE]   ?: true
            )
        }

    suspend fun updateTheme(theme: String) {
        dataStore.edit { it[PrefKeys.THEME] = theme }
    }
    suspend fun updateFontSize(size: Float) {
        dataStore.edit { it[PrefKeys.FONT_SIZE] = size }
    }
    suspend fun updateLineHeight(height: Float) {
        dataStore.edit { it[PrefKeys.LINE_HEIGHT] = height }
    }
    suspend fun updatePageMargins(v: Float) {
        dataStore.edit { it[PrefKeys.PAGE_MARGINS] = v }
    }
    suspend fun updatePagedMode(v: Boolean) {
        dataStore.edit { it[PrefKeys.PAGED_MODE] = v }
    }
    suspend fun resetAll() {
        dataStore.edit { it.clear() }
    }
}

// Simple data class — lives in this file
data class UserPrefs(
    val theme: String,
    val fontSize: Float,
    val lineHeight: Float,
    val pageMargins: Float,
    val pagedMode: Boolean
)
