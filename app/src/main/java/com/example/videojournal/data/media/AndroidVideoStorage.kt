package com.example.videojournal.data.media

import com.example.videojournal.domain.media.VideoStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

class AndroidVideoStorage(
    private val filesDir: File,
    private val cacheDir: File,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
    private val ioDispatcher: CoroutineDispatcher,
) : VideoStorage {
    override suspend fun createTempRecordingFile(): String = withContext(ioDispatcher) {
        val tempFile = createEmptyFile(
            directory = tempRecordingsDirectory,
            extension = VIDEO_EXTENSION,
        )
        tempFile.absolutePath
    }

    override suspend fun promoteTempRecording(tempFilePath: String): String =
        withContext(ioDispatcher) {
            val source = File(tempFilePath)
            if (!source.isFile) {
                throw IOException("Temp recording does not exist: $tempFilePath")
            }

            val destination = nextUnusedFile(
                directory = videosDirectory,
                extension = VIDEO_EXTENSION,
            )

            moveFile(source = source, destination = destination)
            destination.absolutePath
        }

    override suspend fun deleteFile(path: String) {
        withContext(ioDispatcher) {
            val file = File(path)
            if (!file.exists()) return@withContext
            if (!file.isFile) {
                throw IOException("Expected a file path, got: $path")
            }
            if (!file.delete()) {
                throw IOException("Could not delete file: $path")
            }
        }
    }

    override suspend fun cleanupTempRecordings() {
        withContext(ioDispatcher) {
            val tempDirectory = tempRecordingsDirectory
            if (!tempDirectory.exists()) return@withContext

            if (!tempDirectory.deleteRecursively()) {
                throw IOException("Could not delete temp recording directory: ${tempDirectory.absolutePath}")
            }
        }
    }

    private val videosDirectory: File
        get() = File(filesDir, VIDEOS_DIRECTORY)

    private val tempRecordingsDirectory: File
        get() = File(cacheDir, TEMP_RECORDINGS_DIRECTORY)

    private fun createEmptyFile(directory: File, extension: String): File {
        directory.ensureDirectory()
        repeat(MAX_FILE_NAME_ATTEMPTS) {
            val file = nextFileCandidate(directory, extension)
            if (file.createNewFile()) {
                return file
            }
        }
        throw IOException("Could not create a unique file in ${directory.absolutePath}")
    }

    private fun nextUnusedFile(directory: File, extension: String): File {
        directory.ensureDirectory()
        repeat(MAX_FILE_NAME_ATTEMPTS) {
            val file = nextFileCandidate(directory, extension)
            if (!file.exists()) {
                return file
            }
        }
        throw IOException("Could not find a unique file name in ${directory.absolutePath}")
    }

    private fun nextFileCandidate(directory: File, extension: String): File =
        File(directory, "${idProvider()}$extension")

    private fun moveFile(source: File, destination: File) {
        if (source.renameTo(destination)) return

        try {
            source.copyTo(destination, overwrite = false)
            if (!source.delete()) {
                destination.delete()
                throw IOException("Could not delete temp recording after copy: ${source.absolutePath}")
            }
        } catch (throwable: Throwable) {
            destination.delete()
            throw throwable
        }
    }

    companion object {
        const val VIDEOS_DIRECTORY = "videos"
        const val TEMP_RECORDINGS_DIRECTORY = "video_recording"

        private const val VIDEO_EXTENSION = ".mp4"
        private const val MAX_FILE_NAME_ATTEMPTS = 10
    }
}
