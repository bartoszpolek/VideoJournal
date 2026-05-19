package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.testing.FakeVideoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveVideosUseCaseTest {
    @Test
    fun observeVideosReturnsRepositoryFlow() = runTest {
        val video = VideoEntry(
            id = "video-id",
            filePath = "/files/videos/video.mp4",
            thumbnailPath = null,
            description = "description",
            durationMs = 1_000L,
            createdAtMillis = 123L,
        )
        val repository = FakeVideoRepository(initialVideos = listOf(video))
        val useCase = ObserveVideosUseCase(repository)

        assertEquals(listOf(video), useCase().first())
    }
}
