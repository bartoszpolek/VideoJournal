package com.example.videojournal.presentation.feed

import com.example.videojournal.R
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.repository.VideoRepository
import com.example.videojournal.domain.testing.FakeVideoRepository
import com.example.videojournal.domain.testing.FakeVideoStorage
import com.example.videojournal.domain.testing.TestDispatcherProvider
import com.example.videojournal.domain.testing.videoEntry
import com.example.videojournal.domain.usecase.DeleteVideoUseCase
import com.example.videojournal.domain.usecase.ObserveVideosUseCase
import com.example.videojournal.presentation.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts loading then maps videos to content`() = runTest(mainDispatcherRule.testDispatcher) {
        val video = videoEntry(id = "video-1")
        val repository = ControlledVideoRepository()
        val viewModel = createViewModel(repository = repository)

        assertEquals(FeedUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()
        repository.emit(listOf(video))
        advanceUntilIdle()

        assertEquals(
            FeedUiState.Content(items = listOf(video)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `empty video list maps to empty state`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(videos = emptyList())
        advanceUntilIdle()

        assertEquals(FeedUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `observe failure maps to error state`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(repository = FailingVideoRepository())
        advanceUntilIdle()

        assertEquals(
            FeedUiState.Error(R.string.feed_error_load),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `video tap toggles playing id`() = runTest(mainDispatcherRule.testDispatcher) {
        val video = videoEntry(id = "video-1")
        val viewModel = createViewModel(videos = listOf(video))
        advanceUntilIdle()

        viewModel.onIntent(FeedIntent.VideoTapped(video.id))

        assertEquals(video.id, viewModel.contentState().playingId)

        viewModel.onIntent(FeedIntent.VideoTapped(video.id))

        assertNull(viewModel.contentState().playingId)
    }

    @Test
    fun `page change clears playing id`() = runTest(mainDispatcherRule.testDispatcher) {
        val first = videoEntry(id = "video-1")
        val second = videoEntry(id = "video-2")
        val viewModel = createViewModel(videos = listOf(first, second))
        advanceUntilIdle()

        viewModel.onIntent(FeedIntent.VideoTapped(first.id))
        viewModel.onIntent(FeedIntent.PageChanged(second.id))

        assertNull(viewModel.contentState().playingId)
    }

    @Test
    fun `delete click invokes delete use case`() = runTest(mainDispatcherRule.testDispatcher) {
        val video = videoEntry(id = "video-1")
        val storage = FakeVideoStorage()
        val repository = FakeVideoRepository(initialVideos = listOf(video))
        val viewModel = createViewModel(
            repository = repository,
            storage = storage,
        )
        advanceUntilIdle()

        viewModel.onIntent(FeedIntent.DeleteClicked(video.id))
        advanceUntilIdle()

        assertEquals(listOf(video.id), repository.deletedIds)
        assertTrue(storage.deleteAttempts.contains(video.filePath))
        assertTrue(storage.deleteAttempts.contains(video.thumbnailPath!!))
        assertEquals(FeedUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `delete failure keeps content and exposes user message`() = runTest(mainDispatcherRule.testDispatcher) {
        val video = videoEntry(id = "video-1")
        val repository = DeleteFailingVideoRepository(initialVideos = listOf(video))
        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.onIntent(FeedIntent.DeleteClicked(video.id))
        advanceUntilIdle()

        assertEquals(
            FeedUiState.Content(
                items = listOf(video),
                userMessageResId = R.string.feed_error_delete,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `user message shown clears user message`() = runTest(mainDispatcherRule.testDispatcher) {
        val video = videoEntry(id = "video-1")
        val repository = DeleteFailingVideoRepository(initialVideos = listOf(video))
        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.onIntent(FeedIntent.DeleteClicked(video.id))
        advanceUntilIdle()

        viewModel.onIntent(FeedIntent.UserMessageShown)

        assertEquals(FeedUiState.Content(items = listOf(video)), viewModel.uiState.value)
    }

    @Test
    fun `unknown video tap keeps state unchanged`() = runTest(mainDispatcherRule.testDispatcher) {
        val video = videoEntry(id = "video-1")
        val viewModel = createViewModel(videos = listOf(video))
        advanceUntilIdle()
        val initialState = viewModel.uiState.value

        viewModel.onIntent(FeedIntent.VideoTapped("missing-video"))

        assertEquals(initialState, viewModel.uiState.value)
    }

    @Test
    fun `page change outside content state is ignored`() = runTest(mainDispatcherRule.testDispatcher) {
        val loadingRepository = ControlledVideoRepository()
        val loadingViewModel = createViewModel(repository = loadingRepository)
        val emptyViewModel = createViewModel(videos = emptyList())
        advanceUntilIdle()

        loadingViewModel.onIntent(FeedIntent.PageChanged("video-1"))
        emptyViewModel.onIntent(FeedIntent.PageChanged("video-1"))

        assertEquals(FeedUiState.Loading, loadingViewModel.uiState.value)
        assertEquals(FeedUiState.Empty, emptyViewModel.uiState.value)
    }

    @Test
    fun `playing id is kept when observed video still exists`() = runTest(mainDispatcherRule.testDispatcher) {
        val first = videoEntry(id = "video-1", createdAtMillis = 1_000L)
        val second = videoEntry(id = "video-2", createdAtMillis = 2_000L)
        val repository = FakeVideoRepository(initialVideos = listOf(first))
        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.onIntent(FeedIntent.VideoTapped(first.id))
        repository.save(second)
        advanceUntilIdle()

        assertEquals(first.id, viewModel.contentState().playingId)
    }

    private fun createViewModel(
        videos: List<VideoEntry> = emptyList(),
        repository: VideoRepository = FakeVideoRepository(initialVideos = videos),
        storage: FakeVideoStorage = FakeVideoStorage(),
    ): FeedViewModel =
        FeedViewModel(
            observeVideosUseCase = ObserveVideosUseCase(repository),
            deleteVideoUseCase = DeleteVideoUseCase(
                repository = repository,
                storage = storage,
                dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
            ),
        )

    private fun FeedViewModel.contentState(): FeedUiState.Content =
        uiState.value as FeedUiState.Content

    private class FailingVideoRepository : VideoRepository {
        override fun observeVideos(): Flow<List<VideoEntry>> = flow {
            throw IllegalStateException("database unavailable")
        }

        override suspend fun save(video: VideoEntry) = Unit

        override suspend fun deleteById(id: String) = Unit
    }

    private class DeleteFailingVideoRepository(
        initialVideos: List<VideoEntry>,
    ) : VideoRepository {
        private val videos = MutableStateFlow(initialVideos)

        override fun observeVideos(): Flow<List<VideoEntry>> = videos

        override suspend fun save(video: VideoEntry) = Unit

        override suspend fun deleteById(id: String) {
            throw IllegalStateException("delete failed")
        }
    }

    private class ControlledVideoRepository : VideoRepository {
        private val videos = MutableSharedFlow<List<VideoEntry>>(extraBufferCapacity = 1)

        override fun observeVideos(): Flow<List<VideoEntry>> = videos

        override suspend fun save(video: VideoEntry) = Unit

        override suspend fun deleteById(id: String) = Unit

        suspend fun emit(items: List<VideoEntry>) {
            videos.emit(items)
        }
    }
}
