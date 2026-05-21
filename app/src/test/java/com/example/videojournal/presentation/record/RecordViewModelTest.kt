package com.example.videojournal.presentation.record

import com.example.videojournal.R
import com.example.videojournal.domain.testing.FakeThumbnailGenerator
import com.example.videojournal.domain.testing.FakeVideoRepository
import com.example.videojournal.domain.testing.FakeVideoStorage
import com.example.videojournal.domain.testing.TestDispatcherProvider
import com.example.videojournal.domain.usecase.SaveRecordedVideoUseCase
import com.example.videojournal.presentation.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `reviewed recording can be saved`() = runTest(mainDispatcherRule.testDispatcher) {
        val storage = FakeVideoStorage()
        val viewModel = createViewModel(
            storage = storage,
        )

        viewModel.onIntent(permissionsGranted())
        viewModel.onIntent(RecordIntent.StartClicked)
        advanceUntilIdle()

        assertEquals(
            RecordUiState.StartingRecording(tempFilePath = storage.tempRecordingPath),
            viewModel.uiState.value,
        )

        viewModel.onIntent(RecordIntent.CameraRecordingStarted(storage.tempRecordingPath))
        viewModel.onIntent(
            RecordIntent.CameraRecordingFinalized(
                tempFilePath = storage.tempRecordingPath,
                durationMs = 2_500L,
            ),
        )
        viewModel.onIntent(RecordIntent.DescriptionChanged("  first clip  "))

        assertEquals(
            RecordUiState.Reviewing(
                tempFilePath = storage.tempRecordingPath,
                durationMs = 2_500L,
                description = "  first clip  ",
            ),
            viewModel.uiState.value,
        )

        viewModel.onIntent(RecordIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(RecordUiState.Done, viewModel.uiState.value)
    }

    @Test
    fun `microphone denial blocks recording flow`() = runTest(mainDispatcherRule.testDispatcher) {
        val storage = FakeVideoStorage()
        val viewModel = createViewModel(storage = storage)

        viewModel.onIntent(
            RecordIntent.PermissionsResolved(
                cameraGranted = true,
                audioGranted = false,
                permanentlyDenied = false,
            ),
        )
        viewModel.onIntent(RecordIntent.StartClicked)
        advanceUntilIdle()

        assertEquals(
            RecordUiState.PermissionDenied(permanentlyDenied = false),
            viewModel.uiState.value,
        )
        assertTrue(storage.createdTempFiles.isEmpty())
    }

    @Test
    fun `save failure maps to error state`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeVideoRepository().apply {
            saveException = IllegalStateException("database unavailable")
        }
        val storage = FakeVideoStorage()
        val viewModel = createViewModel(
            repository = repository,
            storage = storage,
        )

        viewModel.onIntent(permissionsGranted())
        viewModel.onIntent(RecordIntent.StartClicked)
        advanceUntilIdle()
        viewModel.onIntent(RecordIntent.CameraRecordingStarted(storage.tempRecordingPath))

        viewModel.onIntent(
            RecordIntent.CameraRecordingFinalized(
                tempFilePath = storage.tempRecordingPath,
                durationMs = 2_500L,
            ),
        )
        viewModel.onIntent(RecordIntent.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            RecordUiState.Error(
                messageResId = R.string.record_error_save,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `discard deletes temp recording and returns to ready`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val storage = FakeVideoStorage()
            val viewModel = createViewModel(storage = storage)

            viewModel.onIntent(permissionsGranted())
            viewModel.onIntent(RecordIntent.StartClicked)
            advanceUntilIdle()
            viewModel.onIntent(RecordIntent.CameraRecordingStarted(storage.tempRecordingPath))

            viewModel.onIntent(
                RecordIntent.CameraRecordingFinalized(
                    tempFilePath = storage.tempRecordingPath,
                    durationMs = 2_500L,
                ),
            )
            viewModel.onIntent(RecordIntent.DiscardClicked)
            advanceUntilIdle()

            assertEquals(RecordUiState.Ready, viewModel.uiState.value)
            assertEquals(listOf(storage.tempRecordingPath), storage.deleteAttempts)
        }

    @Test
    fun `camera denial maps to permission denied`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(
            RecordIntent.PermissionsResolved(
                cameraGranted = false,
                audioGranted = false,
                permanentlyDenied = true,
            ),
        )

        assertEquals(
            RecordUiState.PermissionDenied(permanentlyDenied = true),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `recording failure deletes temp recording`() = runTest(mainDispatcherRule.testDispatcher) {
        val storage = FakeVideoStorage()
        val viewModel = createViewModel(storage = storage)

        viewModel.onIntent(permissionsGranted())
        viewModel.onIntent(RecordIntent.StartClicked)
        advanceUntilIdle()

        viewModel.onIntent(RecordIntent.CameraRecordingFailed(storage.tempRecordingPath))
        advanceUntilIdle()

        assertEquals(
            RecordUiState.Error(
                messageResId = R.string.record_error_start,
            ),
            viewModel.uiState.value,
        )
        assertTrue(storage.deleteAttempts.contains(storage.tempRecordingPath))
    }

    @Test
    fun `retry returns to permission checking`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(RecordIntent.CameraRecordingFailed(tempFilePath = null))
        advanceUntilIdle()

        viewModel.onIntent(RecordIntent.RetryClicked)

        assertEquals(RecordUiState.CheckingPermissions, viewModel.uiState.value)
    }

    private fun createViewModel(
        repository: FakeVideoRepository = FakeVideoRepository(),
        storage: FakeVideoStorage = FakeVideoStorage(),
        thumbnailGenerator: FakeThumbnailGenerator = FakeThumbnailGenerator(),
    ): RecordViewModel =
        RecordViewModel(
            saveRecordedVideoUseCase = SaveRecordedVideoUseCase(
                repository = repository,
                storage = storage,
                thumbnailGenerator = thumbnailGenerator,
                dispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher),
            ),
            videoStorage = storage,
        )

    private fun permissionsGranted(): RecordIntent.PermissionsResolved =
        RecordIntent.PermissionsResolved(
            cameraGranted = true,
            audioGranted = true,
            permanentlyDenied = false,
        )
}
