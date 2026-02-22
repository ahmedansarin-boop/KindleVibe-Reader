package com.kindlevibe.reader.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index(value = ["uri"], unique = true)]
)
data class BookEntity(
    @PrimaryKey val id: String,           // UUID string
    val uri: String,                       // SAF content:// URI string
    val title: String?,
    val author: String?,
    val lastReadAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val progress: Double = 0.0,
    val readingTimeSeconds: Long = 0L,
    val lastOpenedAt: Long? = null,
    val lastLocatorJson: String? = null,
    val lastProgress: Double? = null,
    val fileType: String = "epub",         // "epub" or "pdf"
    val lastReadPage: Int = 0,            // PDF: 0 = never opened
    val lastFontSize: Float = 0f,          // 0f = use default (16f)
    val lastTheme: String = "",            // "" = use default ("day")
) {
    @Ignore
    var coverPath: String? = null
}

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["bookId", "progression"])]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,           // UUID string
    val bookId: String,
    val locatorJson: String,
    val progression: Double?,
    val label: String?,
    val createdAt: Long                    // epoch ms
)

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["bookId"])]
)
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val openedAt: Long,
    val closedAt: Long?,
    val startProgress: Double?,
    val endProgress: Double?
)
