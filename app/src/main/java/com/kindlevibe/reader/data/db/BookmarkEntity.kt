package com.kindlevibe.reader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val locatorJson: String,
    val highlightText: String?,
    val highlightColor: String?,
    val note: String?,
    val chapterTitle: String?,
    val createdAt: Long = System.currentTimeMillis()
)
