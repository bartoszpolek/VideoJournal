package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.testing.FakeVideoRepository
import com.example.videojournal.domain.testing.videoEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveVideosUseCaseTest {
    @Test
    fun `observe videos returns repository flow`() = runTest {
        val video = videoEntry(thumbnailPath = null)
        val repository = FakeVideoRepository(initialVideos = listOf(video))
        val useCase = ObserveVideosUseCase(repository)

        assertEquals(listOf(video), useCase().first())
    }
}
