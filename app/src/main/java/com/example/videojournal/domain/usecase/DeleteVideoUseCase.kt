package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.media.VideoStorage
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.repository.VideoRepository
import com.example.videojournal.domain.util.DispatcherProvider
import com.example.videojournal.domain.util.runCatchingNonCancellation
import kotlinx.coroutines.withContext

class DeleteVideoUseCase(
    private val repository: VideoRepository,
    private val storage: VideoStorage,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(video: VideoEntry) = withContext(dispatcherProvider.io) {
        repository.deleteById(video.id)
        runCatchingNonCancellation { storage.deleteFile(video.filePath) }
        video.thumbnailPath?.let { thumbnailPath ->
            runCatchingNonCancellation { storage.deleteFile(thumbnailPath) }
        }
    }
}
