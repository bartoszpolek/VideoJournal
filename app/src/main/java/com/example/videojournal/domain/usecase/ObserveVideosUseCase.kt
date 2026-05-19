package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow

class ObserveVideosUseCase(
    private val repository: VideoRepository,
) {
    operator fun invoke(): Flow<List<VideoEntry>> = repository.observeVideos()
}
