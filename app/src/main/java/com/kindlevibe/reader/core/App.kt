package com.kindlevibe.reader.core

import android.app.Application
import com.kindlevibe.reader.data.userPrefsDataStore
import com.kindlevibe.reader.data.db.AppDb
import com.kindlevibe.reader.data.prefs.PreferencesStore
import com.kindlevibe.reader.reader.ReadiumInit
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class KindleVibeApp : Application() {

    companion object {
        lateinit var prefsStore: PreferencesStore
            private set
    }

    val db by lazy { AppDb.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        ReadiumInit.init(this)
        prefsStore = PreferencesStore(applicationContext.userPrefsDataStore)
    }
}
