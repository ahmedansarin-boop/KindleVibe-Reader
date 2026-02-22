package com.kindlevibe.reader.data.repo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.kindlevibe.reader.core.Result
import com.kindlevibe.reader.data.db.BookDao
import com.kindlevibe.reader.data.db.entities.BookEntity
import com.kindlevibe.reader.reader.ReadiumInit
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.toUrl

class LibraryRepository(
    private val bookDao: BookDao,
    private val context: Context
) {
    fun observeBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    suspend fun importBook(uri: Uri): Result<BookEntity> {
        return try {
            // Persist SAF permission across reboots
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // Check for duplicate
            val uriStr = uri.toString()
            val existing = bookDao.getByUri(uriStr)
            if (existing != null) return Result.Success(existing)

            val mimeType = context.contentResolver.getType(uri)
            val fileType = if (mimeType == "application/pdf") "pdf" else "epub"

            if (fileType == "pdf") {
                // PDF import: extract metadata, no cover
                val fileName = resolveDisplayName(uri) ?: "document.pdf"
                var pdfTitle = fileName.removeSuffix(".pdf")
                var pdfAuthor: String? = null
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        PDDocument.load(stream).use { doc ->
                            val info = doc.documentInformation
                            info?.title?.takeIf { it.isNotBlank() }?.let { pdfTitle = it }
                            pdfAuthor = info?.author?.takeIf { it.isNotBlank() }
                        }
                    }
                } catch (_: Exception) { /* use filename as title */ }

                val book = BookEntity(
                    id = UUID.randomUUID().toString(),
                    uri = uriStr,
                    title = pdfTitle,
                    author = pdfAuthor,
                    lastReadAt = 0L,
                    addedAt = System.currentTimeMillis(),
                    progress = 0.0,
                    readingTimeSeconds = 0L,
                    lastOpenedAt = null,
                    lastLocatorJson = null,
                    lastProgress = null,
                    fileType = "pdf"
                )
                bookDao.insert(book)
                return Result.Success(book)
            }

            // EPUB import
            val title = resolveDisplayName(uri)
            val book = BookEntity(
                id = UUID.randomUUID().toString(),
                uri = uriStr,
                title = title,
                author = null,
                lastReadAt = 0L,
                addedAt = System.currentTimeMillis(),
                progress = 0.0,
                readingTimeSeconds = 0L,
                lastOpenedAt = null,
                lastLocatorJson = null,
                lastProgress = null,
                fileType = "epub"
            )
            bookDao.insert(book)
            book.coverPath = extractAndSaveCover(uri = uri, bookId = book.id)
            Result.Success(book)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun removeBook(id: String) {
        bookDao.deleteById(id)
    }

    suspend fun markOpened(id: String) {
        bookDao.updateLastRead(id, System.currentTimeMillis())
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private suspend fun extractAndSaveCover(uri: Uri, bookId: String): String? {
        return try {
            val absoluteUrl = uri.toUrl() as? AbsoluteUrl ?: return null

            val assetTry = ReadiumInit.assetRetriever.retrieve(absoluteUrl)
            if (assetTry is Try.Failure) return null
            val asset = (assetTry as Try.Success).value

            val pubTry = ReadiumInit.publicationOpener.open(asset, allowUserInteraction = false)
            if (pubTry is Try.Failure) return null
            val publication = (pubTry as Try.Success).value

            try {
                val bitmap = publication.cover() ?: return null
                val file = File(context.filesDir, "covers/$bookId.jpg")
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
                file.absolutePath
            } finally {
                publication.close()
            }
        } catch (_: Exception) {
            null
        }
    }
}
