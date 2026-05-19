package com.example.videojournal.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.example.videojournal.data.database.VideoJournalDatabase
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.testing.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineVideoRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: VideoJournalDatabase

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VideoJournalDatabase.Schema.create(driver)
        database = VideoJournalDatabase(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun observeVideosEmitsLatestFirstAndUpdates() = runTest {
        val repository = createRepository()
        val older = videoEntry(id = "older", createdAtMillis = 1_000L)
        val newer = videoEntry(id = "newer", createdAtMillis = 2_000L)

        repository.observeVideos().test {
            assertEquals(emptyList<VideoEntry>(), awaitItem())

            repository.save(older)
            assertEquals(listOf(older), awaitItem())

            repository.save(newer)
            assertEquals(listOf(newer, older), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByIdRemovesPersistedVideo() = runTest {
        val repository = createRepository()
        val video = videoEntry(id = "video-id")

        repository.observeVideos().test {
            assertEquals(emptyList<VideoEntry>(), awaitItem())

            repository.save(video)
            assertEquals(listOf(video), awaitItem())

            repository.deleteById(video.id)
            assertEquals(emptyList<VideoEntry>(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createRepository(): OfflineVideoRepository =
        OfflineVideoRepository(
            database = database,
            dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher()),
        )

    private fun videoEntry(
        id: String,
        createdAtMillis: Long = 1_000L,
    ): VideoEntry =
        VideoEntry(
            id = id,
            filePath = "/files/videos/$id.mp4",
            thumbnailPath = "/files/thumbnails/$id.jpg",
            description = "description $id",
            durationMs = 5_000L,
            createdAtMillis = createdAtMillis,
        )
}
