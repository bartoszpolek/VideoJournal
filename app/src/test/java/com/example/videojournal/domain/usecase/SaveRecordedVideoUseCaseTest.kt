package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.model.PendingRecording
import com.example.videojournal.domain.testing.FakeThumbnailGenerator
import com.example.videojournal.domain.testing.FakeVideoRepository
import com.example.videojournal.domain.testing.FakeVideoStorage
import com.example.videojournal.domain.testing.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveRecordedVideoUseCaseTest {
    @Test
    fun savePromotesFileGeneratesThumbnailAndStoresMetadata() = runTest {
        val repository = FakeVideoRepository()
        val storage = FakeVideoStorage()
        val thumbnailGenerator = FakeThumbnailGenerator().apply {
            result = "/files/thumbnails/thumb.jpg"
        }
        val useCase = createUseCase(repository, storage, thumbnailGenerator)

        val video = useCase(
            recording = PendingRecording(filePath = "/cache/video_recording/temp.mp4", durationMs = 5_000L),
            description = "  first clip  ",
        )

        assertNotNull(video.id)
        assertTrue(video.id.isNotBlank())
        assertEquals("/files/videos/final.mp4", video.filePath)
        assertEquals("/files/thumbnails/thumb.jpg", video.thumbnailPath)
        assertEquals("first clip", video.description)
        assertEquals(5_000L, video.durationMs)
        assertEquals(listOf("/cache/video_recording/temp.mp4"), storage.promotedTempPaths)
        assertEquals(listOf("/files/videos/final.mp4"), thumbnailGenerator.requestedVideoPaths)
        assertEquals(listOf(video), repository.savedVideos)
        assertTrue(storage.deletedPaths.isEmpty())
    }

    @Test
    fun saveStoresVideoWhenThumbnailGenerationFails() = runTest {
        val repository = FakeVideoRepository()
        val storage = FakeVideoStorage()
        val thumbnailGenerator = FakeThumbnailGenerator().apply {
            exception = IllegalStateException("thumbnail failed")
        }
        val useCase = createUseCase(repository, storage, thumbnailGenerator)

        val video = useCase(
            recording = PendingRecording(filePath = "/cache/video_recording/temp.mp4", durationMs = 5_000L),
            description = "",
        )

        assertEquals(null, video.thumbnailPath)
        assertEquals(null, video.description)
        assertEquals(listOf(video), repository.savedVideos)
        assertTrue(storage.deletedPaths.isEmpty())
    }

    @Test
    fun saveCleansPromotedFilesWhenRepositorySaveFails() = runTest {
        val repository = FakeVideoRepository().apply {
            saveException = IllegalStateException("db failed")
        }
        val storage = FakeVideoStorage()
        val thumbnailGenerator = FakeThumbnailGenerator().apply {
            result = "/files/thumbnails/thumb.jpg"
        }
        val useCase = createUseCase(repository, storage, thumbnailGenerator)

        val thrown = runCatching {
            useCase(
                recording = PendingRecording(filePath = "/cache/video_recording/temp.mp4", durationMs = 5_000L),
                description = "first clip",
            )
        }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertEquals(
            listOf("/files/videos/final.mp4", "/files/thumbnails/thumb.jpg"),
            storage.deletedPaths,
        )
    }

    private fun createUseCase(
        repository: FakeVideoRepository,
        storage: FakeVideoStorage,
        thumbnailGenerator: FakeThumbnailGenerator,
    ): SaveRecordedVideoUseCase =
        SaveRecordedVideoUseCase(
            repository = repository,
            storage = storage,
            thumbnailGenerator = thumbnailGenerator,
            dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher()),
        )
}
