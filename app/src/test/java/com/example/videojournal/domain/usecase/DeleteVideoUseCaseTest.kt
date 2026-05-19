package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.testing.FakeVideoRepository
import com.example.videojournal.domain.testing.FakeVideoStorage
import com.example.videojournal.domain.testing.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteVideoUseCaseTest {
    @Test
    fun deleteRemovesMetadataAndFiles() = runTest {
        val video = videoEntry(thumbnailPath = "/files/thumbnails/thumb.jpg")
        val repository = FakeVideoRepository(initialVideos = listOf(video))
        val storage = FakeVideoStorage()
        val useCase = DeleteVideoUseCase(
            repository = repository,
            storage = storage,
            dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler)),
        )

        useCase(video)

        assertEquals(listOf(video.id), repository.deletedIds)
        assertEquals(listOf(video.filePath, "/files/thumbnails/thumb.jpg"), storage.deletedPaths)
    }

    @Test
    fun deleteIgnoresFileDeleteFailuresAfterMetadataDelete() = runTest {
        val video = videoEntry()
        val repository = FakeVideoRepository(initialVideos = listOf(video))
        val storage = FakeVideoStorage().apply {
            deleteException = IllegalStateException("file delete failed")
        }
        val useCase = DeleteVideoUseCase(
            repository = repository,
            storage = storage,
            dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler)),
        )

        useCase(video)

        assertEquals(listOf(video.id), repository.deletedIds)
        assertEquals(listOf(video.filePath), storage.deletedPaths)
    }

    private fun videoEntry(thumbnailPath: String? = null): VideoEntry =
        VideoEntry(
            id = "video-id",
            filePath = "/files/videos/video.mp4",
            thumbnailPath = thumbnailPath,
            description = "description",
            durationMs = 1_000L,
            createdAtMillis = 123L,
        )
}
