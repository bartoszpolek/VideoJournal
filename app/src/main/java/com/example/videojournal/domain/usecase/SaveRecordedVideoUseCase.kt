package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.media.ThumbnailGenerator
import com.example.videojournal.domain.media.VideoStorage
import com.example.videojournal.domain.model.PendingRecording
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.repository.VideoRepository
import com.example.videojournal.domain.util.DispatcherProvider
import com.example.videojournal.domain.util.runCatchingNonCancellation
import kotlinx.coroutines.withContext
import java.util.UUID

class SaveRecordedVideoUseCase(
    private val repository: VideoRepository,
    private val storage: VideoStorage,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(
        recording: PendingRecording,
        description: String?,
    ): VideoEntry = withContext(dispatcherProvider.io) {
        val tempVideoPath = recording.tempFilePath
        var finalVideoPath: String? = null
        var thumbnailPath: String? = null

        try {
            finalVideoPath = storage.promoteTempRecording(tempVideoPath)
            thumbnailPath = runCatchingNonCancellation {
                thumbnailGenerator.generate(finalVideoPath)
            }.getOrNull()

            val video = VideoEntry(
                id = UUID.randomUUID().toString(),
                filePath = finalVideoPath,
                thumbnailPath = thumbnailPath,
                description = description.normalizeDescription(),
                durationMs = recording.durationMs,
                createdAtMillis = System.currentTimeMillis(),
            )

            repository.save(video)
            video
        } catch (throwable: Throwable) {
            val videoPathToDelete = finalVideoPath ?: tempVideoPath
            runCatchingNonCancellation { storage.deleteFile(videoPathToDelete) }
            thumbnailPath?.let { runCatchingNonCancellation { storage.deleteFile(it) } }
            throw throwable
        }
    }

    private fun String?.normalizeDescription(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
