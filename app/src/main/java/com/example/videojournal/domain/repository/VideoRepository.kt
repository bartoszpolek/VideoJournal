package com.example.videojournal.domain.repository

import com.example.videojournal.domain.model.VideoEntry
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun observeVideos(): Flow<List<VideoEntry>>

    suspend fun save(video: VideoEntry)

    suspend fun deleteById(id: String)
}
