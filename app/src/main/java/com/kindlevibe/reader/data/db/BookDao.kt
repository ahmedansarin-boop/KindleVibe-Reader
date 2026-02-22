package com.kindlevibe.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kindlevibe.reader.data.db.entities.BookEntity
import kotlinx.coroutines.flow.Flow

data class ReadingState(
    val lastReadPage: Int,
    val lastFontSize: Float,
    val lastTheme: String
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity)

    @Update
    suspend fun update(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE books SET lastOpenedAt = :ts, lastReadAt = :ts WHERE id = :id")
    suspend fun updateLastOpened(id: String, ts: Long)

    @Query("UPDATE books SET lastReadAt = :time, lastOpenedAt = :time WHERE id = :id")
    suspend fun updateLastRead(id: String, time: Long)

    @Query("UPDATE books SET progress = :progress, lastProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Double)

    @Query("UPDATE books SET readingTimeSeconds = readingTimeSeconds + :seconds WHERE id = :id")
    suspend fun addReadingTime(id: String, seconds: Long)

    @Query("UPDATE books SET lastLocatorJson = :json, progress = :progress, lastProgress = :progress WHERE id = :id")
    suspend fun updateLocator(id: String, json: String, progress: Double)

    @Query("""
        UPDATE books 
        SET lastReadPage = :page,
            lastFontSize = :fontSize,
            lastTheme = :theme
        WHERE id = :bookId
    """)
    suspend fun updateReadingState(
        bookId: String,
        page: Int,
        fontSize: Float,
        theme: String
    )

    @Query("""
        SELECT lastReadPage, lastFontSize, lastTheme 
        FROM books WHERE id = :bookId
    """)
    suspend fun getReadingState(bookId: String): ReadingState?
}
