package com.example.videojournal.data.media

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.util.Size
import com.example.videojournal.domain.media.ThumbnailGenerator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidThumbnailGenerator(
    private val filesDir: File,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ThumbnailGenerator {
    override suspend fun generate(videoPath: String): String? = withContext(ioDispatcher) {
        try {
            generateThumbnail(videoPath)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            null
        }
    }

    private fun generateThumbnail(videoPath: String): String? {
        val thumbnail = ThumbnailUtils.createVideoThumbnail(
            File(videoPath),
            THUMBNAIL_SIZE,
            null,
        )
        val destination = nextThumbnailFile()

        return try {
            FileOutputStream(destination).use { output ->
                if (!thumbnail.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    throw IOException("Could not encode thumbnail")
                }
            }
            destination.absolutePath
        } catch (throwable: Throwable) {
            destination.delete()
            throw throwable
        } finally {
            thumbnail.recycle()
        }
    }

    private fun nextThumbnailFile(): File {
        val directory = File(filesDir, THUMBNAILS_DIRECTORY)
        directory.ensureDirectory()

        repeat(MAX_FILE_NAME_ATTEMPTS) {
            val file = File(directory, "${idProvider()}$THUMBNAIL_EXTENSION")
            if (!file.exists()) {
                return file
            }
        }
        throw IOException("Could not find a unique thumbnail name in ${directory.absolutePath}")
    }

    private fun File.ensureDirectory() {
        if (exists()) {
            if (!isDirectory) {
                throw IOException("Expected directory, got file: $absolutePath")
            }
            return
        }
        if (!mkdirs()) {
            throw IOException("Could not create directory: $absolutePath")
        }
    }

    companion object {
        const val THUMBNAILS_DIRECTORY = "thumbnails"

        private val THUMBNAIL_SIZE = Size(480, 480)
        private const val JPEG_QUALITY = 80
        private const val THUMBNAIL_EXTENSION = ".jpg"
        private const val MAX_FILE_NAME_ATTEMPTS = 10
    }
}
