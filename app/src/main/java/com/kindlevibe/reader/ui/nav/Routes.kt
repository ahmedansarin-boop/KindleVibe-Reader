package com.kindlevibe.reader.ui.nav

sealed class Routes(val route: String) {
    object Library : Routes("library")
    object Reader : Routes("reader/{bookId}")
    object Settings : Routes("settings")
    object About : Routes("about")
}
