package com.example.videojournal.domain.testing

import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVideoRepository(
    initialVideos: List<VideoEntry> = emptyList(),
) : VideoRepository {
    private val videos = MutableStateFlow(initialVideos)

    var saveException: Throwable? = null
    val savedVideos = mutableListOf<VideoEntry>()
    val deletedIds = mutableListOf<String>()

    override fun observeVideos(): Flow<List<VideoEntry>> = videos

    override suspend fun save(video: VideoEntry) {
        saveException?.let { throw it }
        savedVideos += video
        videos.value = (videos.value + video).sortedByDescending { it.createdAtMillis }
    }

    override suspend fun deleteById(id: String) {
        deletedIds += id
        videos.value = videos.value.filterNot { it.id == id }
    }
}
