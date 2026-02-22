package com.kindlevibe.reader.data.repo

import org.readium.r2.shared.publication.Locator
import com.kindlevibe.reader.data.db.BookDao
import com.kindlevibe.reader.data.db.entities.BookEntity
import com.kindlevibe.reader.reader.LocatorCodec

class ReaderRepository(private val bookDao: BookDao) {

    suspend fun getBook(id: String): BookEntity? =
        bookDao.getById(id)

    suspend fun saveLocator(bookId: String, locator: Locator) {
        val json     = LocatorCodec.encode(locator)
        val progress = locator.locations.progression ?: 0.0
        bookDao.updateLocator(bookId, json, progress)
    }

    fun loadLocator(book: BookEntity): Locator? =
        book.lastLocatorJson?.let { LocatorCodec.decode(it) }
}
