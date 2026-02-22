package com.kindlevibe.reader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kindlevibe.reader.core.KindleVibeApp
import com.kindlevibe.reader.data.db.AppDb
import com.kindlevibe.reader.data.db.BookmarkEntity
import com.kindlevibe.reader.data.db.entities.BookEntity
import com.kindlevibe.reader.data.prefs.UserPrefs
import com.kindlevibe.reader.data.repo.ReaderRepository
import com.kindlevibe.reader.reader.LocatorCodec
import com.kindlevibe.reader.reader.ReadiumInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.toUrl

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db           = AppDb.getInstance(application)
    private val readerRepo   = ReaderRepository(db.bookDao())
    private val prefsStore   = KindleVibeApp.prefsStore
    private val bookmarkDao  = db.bookmarkDao()

    // Publication state
    private val _publication = MutableStateFlow<Publication?>(null)
    val publication: StateFlow<Publication?> = _publication

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book

    private val _openError = MutableStateFlow<String?>(null)
    val openError: StateFlow<String?> = _openError

    // Reading progress for footer
    private val _currentLocator = MutableStateFlow<Locator?>(null)
    val currentLocator: StateFlow<Locator?> = _currentLocator

    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<Locator>>(emptyList())
    private val _isSearching = MutableStateFlow(false)

    val searchQuery: StateFlow<String> = _searchQuery
    val searchResults: StateFlow<List<Locator>> = _searchResults
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks

    // User preferences
    val userPrefs: StateFlow<UserPrefs> = prefsStore.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            UserPrefs("day", 1.0f, 1.4f, 1.0f, true))

    // Locator save job — debounce to avoid excessive DB writes
    private var saveJob: Job? = null
    private var bookmarksJob: Job? = null
    private var readingStartTime: Long = 0L

    fun loadBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = readerRepo.getBook(bookId)
            if (bookEntity == null) {
                _openError.value = "Book not found."
                return@launch
            }
            _book.value = bookEntity
            loadBookmarks()
            onReaderStart()
            db.bookDao().updateLastRead(bookId, System.currentTimeMillis())

            try {
                val uri = android.net.Uri.parse(bookEntity.uri)

                val absoluteUrl = uri.toUrl() as? org.readium.r2.shared.util.AbsoluteUrl
                    as? org.readium.r2.shared.util.AbsoluteUrl
                    ?: run {
                        _openError.value = "Invalid book path."
                        return@launch
                    }

                val assetTry = ReadiumInit.assetRetriever.retrieve(absoluteUrl)
                if (assetTry is org.readium.r2.shared.util.Try.Failure) {
                    _openError.value = "Couldn't access this file. Please re-import."
                    return@launch
                }
                val asset = (assetTry as org.readium.r2.shared.util.Try.Success).value

                val pubTry = ReadiumInit.publicationOpener
                    .open(asset, allowUserInteraction = false)
                if (pubTry is org.readium.r2.shared.util.Try.Failure) {
                    _openError.value = "This book can't be rendered."
                    return@launch
                }

                _publication.value =
                    (pubTry as org.readium.r2.shared.util.Try.Success).value

            } catch (e: Exception) {
                _openError.value = "Unexpected error: ${e.localizedMessage}"
            }
        }
    }

    @OptIn(ExperimentalReadiumApi::class)
    fun searchInBook(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        val pub = _publication.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                val results = mutableListOf<Locator>()
                val iterator = pub.search(query)
                if (iterator != null) {
                    try {
                        while (results.size < 50) {
                            when (val page = iterator.next()) {
                                is Try.Success -> {
                                    val locatorPage = page.value ?: break
                                    results.addAll(locatorPage.locators)
                                    if (locatorPage.locators.isEmpty()) break
                                }
                                is Try.Failure -> break
                            }
                        }
                    } finally {
                        iterator.close()
                    }
                }
                _searchResults.value = results.take(50)
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun loadBookmarks() {
        val bookId = _book.value?.id ?: return
        bookmarksJob?.cancel()
        bookmarksJob = viewModelScope.launch {
            bookmarkDao.getBookmarksForBook(bookId).collect { items ->
                _bookmarks.value = items
            }
        }
    }

    fun addBookmark() {
        val locator = _currentLocator.value ?: return
        val bookId = _book.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkDao.insert(
                BookmarkEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    bookId = bookId,
                    locatorJson = LocatorCodec.encode(locator),
                    highlightText = null,
                    highlightColor = null,
                    note = null,
                    chapterTitle = locator.title
                )
            )
        }
    }

    fun addHighlight(text: String, color: String = "yellow") {
        val locator = _currentLocator.value ?: return
        val bookId = _book.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkDao.insert(
                BookmarkEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    bookId = bookId,
                    locatorJson = LocatorCodec.encode(locator),
                    highlightText = text,
                    highlightColor = color,
                    note = null,
                    chapterTitle = locator.title
                )
            )
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkDao.deleteById(id)
        }
    }

    // Called by navigator on every page turn
    fun onLocatorChanged(locator: Locator) {
        _currentLocator.value = locator
        val progress = locator.locations.progression ?: 0.0
        // Debounce DB save: only write after 3 seconds of no changes
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(3000)
            val bookId = _book.value?.id ?: return@launch
            readerRepo.saveLocator(bookId, locator)
            db.bookDao().updateProgress(bookId, progress)
        }
    }

    fun onReaderStart() {
        readingStartTime = System.currentTimeMillis()
    }

    // Force-save on lifecycle stop (app backgrounded/killed)
    fun onReaderStop() {
        val bookId  = _book.value?.id ?: return
        val seconds = (System.currentTimeMillis() - readingStartTime) / 1000
        val locator = _currentLocator.value
        val locatorProgress = locator?.locations?.progression ?: 0.0

        if (seconds > 5) {
            viewModelScope.launch(Dispatchers.IO) {
                db.bookDao().addReadingTime(bookId, seconds)
                if (locatorProgress <= 0.0) {
                    // Some EPUBs report 0 progression for long-form HTML chapters.
                    // Persist a minimal started progress so cards can reflect active reading state.
                    db.bookDao().updateProgress(bookId, 0.01)
                }
            }
        }

        locator ?: return
        viewModelScope.launch(Dispatchers.IO) {
            readerRepo.saveLocator(bookId, locator)
        }
    }

    fun getResumeLocator(): Locator? =
        _book.value?.let { readerRepo.loadLocator(it) }

    fun clearError() { _openError.value = null }

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            prefsStore.updateTheme(theme)
        }
    }

    fun updateFontSize(size: Float) {
        viewModelScope.launch {
            prefsStore.updateFontSize(size)
        }
    }

    fun updateLineHeight(height: Float) {
        viewModelScope.launch {
            prefsStore.updateLineHeight(height)
        }
    }

    fun resetForReopen() {
        // If publication is already loaded, do nothing.
        if (_publication.value != null) return
        // Otherwise, reload the last known book if available.
        _book.value?.id?.let { loadBook(it) }
    }

    override fun onCleared() {
        super.onCleared()
        // Avoid immediate close on clear to prevent stale-fragment blank states during quick reopen.
        viewModelScope.launch(Dispatchers.IO) {
            _publication.value?.close()
            _publication.value = null
        }
    }
}
