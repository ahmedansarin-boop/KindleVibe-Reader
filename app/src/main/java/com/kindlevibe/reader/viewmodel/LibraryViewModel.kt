package com.kindlevibe.reader.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kindlevibe.reader.core.Result
import com.kindlevibe.reader.data.db.AppDb
import com.kindlevibe.reader.data.db.entities.BookEntity
import com.kindlevibe.reader.data.prefs.SortOrder
import com.kindlevibe.reader.data.repo.LibraryRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db   = AppDb.getInstance(application)
    private val repo = LibraryRepository(db.bookDao(), application)

    private val _sortOrder = MutableStateFlow(SortOrder.LAST_READ)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val booksFlow = repo.observeBooks()
        .map { list ->
            list.map { book ->
                val coverFile = File(getApplication<Application>().filesDir, "covers/${book.id}.jpg")
                book.coverPath = coverFile.takeIf { it.exists() }?.absolutePath
                book
            }
        }

    val books: StateFlow<List<BookEntity>> = combine(
        booksFlow,
        _sortOrder,
        _searchQuery
    ) { list, sort, query ->
        val filtered = if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.title.orEmpty().contains(query, ignoreCase = true) ||
                    it.author.orEmpty().contains(query, ignoreCase = true)
            }
        }

        when (sort) {
            SortOrder.TITLE -> filtered.sortedBy { it.title.orEmpty().lowercase() }
            SortOrder.LAST_READ -> filtered.sortedByDescending { it.lastReadAt }
            SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.addedAt }
            SortOrder.PROGRESS -> filtered.sortedByDescending { it.progress }
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage

    fun setSortOrder(order: SortOrder) { _sortOrder.value = order }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun getBookById(id: String): BookEntity? = books.value.find { it.id == id }

    fun onBookSelected(uri: Uri) {
        viewModelScope.launch {
            val uriString = uri.toString()
            val existing = db.bookDao().getByUri(uriString)
            val displayName = resolveDisplayName(uri)
            val existingByName = displayName?.let { name ->
                books.value.any { it.title == name }
            } == true
            if (existing != null || existingByName) {
                _importError.value = null
                _importMessage.value = "This book is already in your library."
                return@launch
            }
            when (val result = repo.importBook(uri)) {
                is Result.Error -> {
                    _importError.value = result.exception.message
                        ?: "Couldn't open this file. Please select a valid EPUB."
                    _importMessage.value = null
                }
                is Result.Success -> {
                    _importError.value = null
                    _importMessage.value = null
                }
                else -> {
                    _importError.value = null
                    _importMessage.value = null
                }
            }
        }
    }

    fun clearError() { _importError.value = null }
    fun clearImportMessage() { _importMessage.value = null }

    fun removeBook(id: String) {
        viewModelScope.launch { repo.removeBook(id) }
    }

    fun markOpened(id: String) {
        viewModelScope.launch { repo.markOpened(id) }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val context = getApplication<Application>().applicationContext
        return context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}
