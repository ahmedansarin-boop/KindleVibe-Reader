package com.kindlevibe.reader.data.prefs

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey

object PrefKeys {
    val THEME          = stringPreferencesKey("theme")           // "day"|"sepia"|"night"
    val FONT_SIZE      = floatPreferencesKey("font_size")        // default 1.0f
    val LINE_HEIGHT    = floatPreferencesKey("line_height")      // default 1.4f
    val PAGE_MARGINS   = floatPreferencesKey("page_margins")     // default 1.0f
    val PAGED_MODE     = booleanPreferencesKey("paged_mode")     // default true
}
