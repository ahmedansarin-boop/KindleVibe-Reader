package com.kindlevibe.reader.reader

import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Color
import org.readium.r2.navigator.preferences.Theme
import com.kindlevibe.reader.data.prefs.UserPrefs

object PreferencesMapper {

    fun toEpubPreferences(prefs: UserPrefs): EpubPreferences {
        val (bgColor, textColor) = when (prefs.theme) {
            "night" -> 0xFF111418.toInt() to 0xFFD4CDBE.toInt()
            "sepia" -> 0xFFF5E6C8.toInt() to 0xFF3B2E1A.toInt()
            "paper" -> 0xFFEDE0CC.toInt() to 0xFF2C1F0E.toInt()
            else    -> 0xFFFAF8F5.toInt() to 0xFF1A1208.toInt()
        }

        return EpubPreferences(
            theme           = when (prefs.theme) {
                "night" -> Theme.DARK
                "sepia" -> Theme.SEPIA
                else    -> Theme.LIGHT
            },
            backgroundColor = Color(bgColor),
            textColor       = Color(textColor),
            fontSize        = prefs.fontSize.toDouble(),
            lineHeight      = prefs.lineHeight.toDouble(),
            pageMargins     = prefs.pageMargins.toDouble(),
            publisherStyles = false,
            scroll          = false
        )
    }
}
