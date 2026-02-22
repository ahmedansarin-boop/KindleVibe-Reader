package com.kindlevibe.reader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderOverlay(
    title: String,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    lineHeight: Float,
    onLineHeightChange: (Float) -> Unit,
    onSearchClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onBookmarksListClick: () -> Unit,
    onTocClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search in book"
                    )
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark this page"
                    )
                }
                IconButton(onClick = onBookmarksListClick) {
                    Icon(
                        imageVector = Icons.Outlined.Bookmarks,
                        contentDescription = "View bookmarks"
                    )
                }
                IconButton(onClick = onTocClick) {
                    Icon(
                        imageVector = Icons.Outlined.FormatListBulleted,
                        contentDescription = "Table of contents"
                    )
                }
            }
        )

        // Bottom settings panel
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {

                // Theme chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    listOf("day" to "Day", "sepia" to "Sepia", "night" to "Night")
                        .forEach { (value, label) ->
                            FilterChip(
                                selected = currentTheme == value,
                                onClick = { onThemeChange(value) },
                                label = { Text(label) }
                            )
                        }
                }

                Spacer(Modifier.height(8.dp))

                // Font size slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Font Size: ${String.format(Locale.US, "%.1f", fontSize)}x",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = fontSize,
                        onValueChange = onFontSizeChange,
                        valueRange = 0.5f..3.0f,
                        steps = 24,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Line height slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Line Height: ${String.format(Locale.US, "%.1f", lineHeight)}x",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = lineHeight,
                        onValueChange = onLineHeightChange,
                        valueRange = 1.0f..2.5f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
