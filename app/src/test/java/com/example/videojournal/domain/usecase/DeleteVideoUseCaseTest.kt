package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.testing.FakeVideoRepository
import com.example.videojournal.domain.testing.FakeVideoStorage
import com.example.videojournal.domain.testing.TestDispatcherProvider
import com.example.videojournal.domain.testing.videoEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteVideoUseCaseTest {
    @Test
    fun `delete removes metadata and files`() = runTest {
        val video = videoEntry(thumbnailPath = "/files/thumbnails/thumb.jpg")
        val repository = FakeVideoRepository(initialVideos = listOf(video))
        val storage = FakeVideoStorage()
        val useCase = DeleteVideoUseCase(
            repository = repository,
            storage = storage,
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

        useCase(video)

        assertEquals(listOf(video.id), repository.deletedIds)
        assertEquals(listOf(video.filePath, "/files/thumbnails/thumb.jpg"), storage.deleteAttempts)
    }

    @Test
    fun `delete ignores file delete failures after metadata delete`() = runTest {
        val video = videoEntry()
        val repository = FakeVideoRepository(initialVideos = listOf(video))
        val storage = FakeVideoStorage().apply {
            deleteException = IllegalStateException("file delete failed")
        }
        val useCase = DeleteVideoUseCase(
            repository = repository,
            storage = storage,
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        )

        useCase(video)

        assertEquals(listOf(video.id), repository.deletedIds)
        assertEquals(listOf(video.filePath, video.thumbnailPath!!), storage.deleteAttempts)
    }
}
