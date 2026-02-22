package com.kindlevibe.reader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.userPrefsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_prefs")
