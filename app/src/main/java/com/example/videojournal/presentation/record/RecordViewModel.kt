package com.example.videojournal.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videojournal.R
import com.example.videojournal.domain.media.VideoStorage
import com.example.videojournal.domain.model.PendingRecording
import com.example.videojournal.domain.usecase.SaveRecordedVideoUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordViewModel(
    private val saveRecordedVideoUseCase: SaveRecordedVideoUseCase,
    private val videoStorage: VideoStorage,
) : ViewModel() {
    private val _uiState = MutableStateFlow<RecordUiState>(RecordUiState.CheckingPermissions)
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    fun onIntent(intent: RecordIntent) {
        when (intent) {
            is RecordIntent.DescriptionChanged -> onDescriptionChanged(intent.description)
            RecordIntent.DiscardClicked -> onDiscardClicked()
            is RecordIntent.PermissionsResolved -> onPermissionsResolved(intent)
            is RecordIntent.CameraRecordingDurationChanged -> onRecordingDurationChanged(intent.elapsedMs)
            is RecordIntent.CameraRecordingFailed -> onRecordingFailed(intent.tempFilePath)
            is RecordIntent.CameraRecordingFinalized -> onRecordingFinalized(
                tempFilePath = intent.tempFilePath,
                durationMs = intent.durationMs,
            )
            is RecordIntent.CameraRecordingStarted -> onRecordingStarted(intent.tempFilePath)
            RecordIntent.RetryClicked -> _uiState.value = RecordUiState.CheckingPermissions
            RecordIntent.SaveClicked -> onSaveClicked()
            RecordIntent.StartClicked -> onStartClicked()
        }
    }

    private fun onPermissionsResolved(intent: RecordIntent.PermissionsResolved) {
        if (!intent.cameraGranted || !intent.audioGranted) {
            _uiState.value = RecordUiState.PermissionDenied(
                permanentlyDenied = intent.permanentlyDenied,
            )
            return
        }

        _uiState.value = RecordUiState.Ready
    }

    private fun onStartClicked() {
        if (_uiState.value != RecordUiState.Ready) return

        viewModelScope.launch {
            try {
                val tempFilePath = videoStorage.createTempRecordingFile()
                _uiState.value = RecordUiState.StartingRecording(
                    tempFilePath = tempFilePath,
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.value = RecordUiState.Error(
                    messageResId = R.string.record_error_start,
                )
            }
        }
    }

    private fun onRecordingStarted(tempFilePath: String) {
        val state = _uiState.value as? RecordUiState.StartingRecording ?: return
        if (state.tempFilePath != tempFilePath) return

        _uiState.value = RecordUiState.Recording(
            tempFilePath = tempFilePath,
            elapsedMs = 0L,
        )
    }

    private fun onRecordingDurationChanged(elapsedMs: Long) {
        updateRecording { state ->
            state.copy(elapsedMs = elapsedMs.coerceAtLeast(0L))
        }
    }

    private fun onRecordingFinalized(tempFilePath: String, durationMs: Long) {
        val currentState = _uiState.value
        val elapsedMs = when (currentState) {
            is RecordUiState.Recording -> {
                if (currentState.tempFilePath != tempFilePath) return
                currentState.elapsedMs
            }
            is RecordUiState.StartingRecording -> {
                if (currentState.tempFilePath != tempFilePath) return
                0L
            }
            else -> return
        }

        _uiState.value = RecordUiState.Reviewing(
            tempFilePath = tempFilePath,
            durationMs = durationMs.coerceAtLeast(elapsedMs),
        )
    }

    private fun onRecordingFailed(tempFilePath: String?) {
        viewModelScope.launch {
            tempFilePath?.let { deleteTempRecordingBestEffort(it) }
            _uiState.value = RecordUiState.Error(
                messageResId = R.string.record_error_start,
            )
        }
    }

    private fun onDescriptionChanged(description: String) {
        updateReviewing { state -> state.copy(description = description) }
    }

    private fun onSaveClicked() {
        val state = _uiState.value as? RecordUiState.Reviewing ?: return

        viewModelScope.launch {
            _uiState.value = RecordUiState.Saving
            try {
                saveRecordedVideoUseCase(
                    recording = PendingRecording(
                        tempFilePath = state.tempFilePath,
                        durationMs = state.durationMs,
                    ),
                    description = state.description,
                )
                _uiState.value = RecordUiState.Done
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _uiState.value = RecordUiState.Error(
                    messageResId = R.string.record_error_save,
                )
            }
        }
    }

    private fun onDiscardClicked() {
        val state = _uiState.value
        val tempFilePath = when (state) {
            is RecordUiState.Recording -> state.tempFilePath
            is RecordUiState.Reviewing -> state.tempFilePath
            is RecordUiState.StartingRecording -> state.tempFilePath
            else -> return
        }

        viewModelScope.launch {
            deleteTempRecordingBestEffort(tempFilePath)
            _uiState.value = RecordUiState.Ready
        }
    }

    private suspend fun deleteTempRecordingBestEffort(tempFilePath: String) {
        try {
            videoStorage.deleteFile(tempFilePath)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
        }
    }

    private inline fun updateRecording(
        transform: (RecordUiState.Recording) -> RecordUiState.Recording,
    ) {
        val state = _uiState.value as? RecordUiState.Recording ?: return
        _uiState.value = transform(state)
    }

    private inline fun updateReviewing(
        transform: (RecordUiState.Reviewing) -> RecordUiState.Reviewing,
    ) {
        val state = _uiState.value as? RecordUiState.Reviewing ?: return
        _uiState.value = transform(state)
    }
}
