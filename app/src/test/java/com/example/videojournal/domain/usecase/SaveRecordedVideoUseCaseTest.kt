package com.example.videojournal.domain.usecase

import com.example.videojournal.domain.model.PendingRecording
import com.example.videojournal.domain.testing.FakeThumbnailGenerator
import com.example.videojournal.domain.testing.FakeVideoRepository
import com.example.videojournal.domain.testing.FakeVideoStorage
import com.example.videojournal.domain.testing.TestDispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveRecordedVideoUseCaseTest {
    @Test
    fun `save promotes file, generates thumbnail, stores metadata`() = runTest {
        val repository = FakeVideoRepository()
        val storage = FakeVideoStorage()
        val thumbnailGenerator = FakeThumbnailGenerator().apply {
            result = "/files/thumbnails/thumb.jpg"
        }
        val useCase = createUseCase(
            repository,
            storage,
            thumbnailGenerator,
            StandardTestDispatcher(testScheduler)
        )

        val video = useCase(
            recording = PendingRecording(
                tempFilePath = "/cache/video_recording/temp.mp4",
                durationMs = 5_000L
            ),
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
        assertTrue(storage.deleteAttempts.isEmpty())
    }

    @Test
    fun `save stores video when thumbnail generation fails`() = runTest {
        val repository = FakeVideoRepository()
        val storage = FakeVideoStorage()
        val thumbnailGenerator = FakeThumbnailGenerator().apply {
            exception = IllegalStateException("thumbnail failed")
        }
        val useCase = createUseCase(
            repository,
            storage,
            thumbnailGenerator,
            StandardTestDispatcher(testScheduler)
        )

        val video = useCase(
            recording = PendingRecording(
                tempFilePath = "/cache/video_recording/temp.mp4",
                durationMs = 5_000L
            ),
            description = "",
        )

        assertNull(video.thumbnailPath)
        assertNull(video.description)
        assertEquals(listOf(video), repository.savedVideos)
        assertTrue(storage.deleteAttempts.isEmpty())
    }

    @Test
    fun `save cleans promoted files when repository save fails`() = runTest {
        val repository = FakeVideoRepository().apply {
            saveException = IllegalStateException("db failed")
        }
        val storage = FakeVideoStorage()
        val thumbnailGenerator = FakeThumbnailGenerator().apply {
            result = "/files/thumbnails/thumb.jpg"
        }
        val useCase = createUseCase(
            repository,
            storage,
            thumbnailGenerator,
            StandardTestDispatcher(testScheduler)
        )

        val thrown = runCatching {
            useCase(
                recording = PendingRecording(
                    tempFilePath = "/cache/video_recording/temp.mp4",
                    durationMs = 5_000L
                ),
                description = "first clip",
            )
        }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertEquals(
            listOf("/files/videos/final.mp4", "/files/thumbnails/thumb.jpg"),
            storage.deleteAttempts,
        )
    }

    @Test
    fun `save deletes temp recording when promote fails`() = runTest {
        val repository = FakeVideoRepository()
        val storage = FakeVideoStorage().apply {
            promoteException = IllegalStateException("promote failed")
        }
        val thumbnailGenerator = FakeThumbnailGenerator()
        val useCase = createUseCase(
            repository,
            storage,
            thumbnailGenerator,
            StandardTestDispatcher(testScheduler)
        )

        val thrown = runCatching {
            useCase(
                recording = PendingRecording(
                    tempFilePath = "/cache/video_recording/temp.mp4",
                    durationMs = 5_000L
                ),
                description = "first clip",
            )
        }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertEquals(listOf("/cache/video_recording/temp.mp4"), storage.deleteAttempts)
        assertTrue(repository.savedVideos.isEmpty())
        assertTrue(thumbnailGenerator.requestedVideoPaths.isEmpty())
    }

    @Test
    fun `save ignores cleanup failures after repository save fails`() = runTest {
        val repository = FakeVideoRepository().apply {
            saveException = IllegalStateException("db failed")
        }
        val storage = FakeVideoStorage().apply {
            deleteException = IllegalStateException("delete failed")
        }
        val thumbnailGenerator = FakeThumbnailGenerator().apply {
            result = "/files/thumbnails/thumb.jpg"
        }
        val useCase = createUseCase(
            repository,
            storage,
            thumbnailGenerator,
            StandardTestDispatcher(testScheduler)
        )

        val thrown = runCatching {
            useCase(
                recording = PendingRecording(
                    tempFilePath = "/cache/video_recording/temp.mp4",
                    durationMs = 5_000L
                ),
                description = "first clip",
            )
        }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
        assertEquals(
            listOf("/files/videos/final.mp4", "/files/thumbnails/thumb.jpg"),
            storage.deleteAttempts,
        )
    }

    private fun createUseCase(
        repository: FakeVideoRepository,
        storage: FakeVideoStorage,
        thumbnailGenerator: FakeThumbnailGenerator,
        dispatcher: CoroutineDispatcher,
    ): SaveRecordedVideoUseCase =
        SaveRecordedVideoUseCase(
            repository = repository,
            storage = storage,
            thumbnailGenerator = thumbnailGenerator,
            dispatcherProvider = TestDispatcherProvider(dispatcher),
        )
}
