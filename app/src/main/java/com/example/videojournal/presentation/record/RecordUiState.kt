package com.example.videojournal.presentation.record

import androidx.annotation.StringRes

sealed interface RecordUiState {
    data object CheckingPermissions : RecordUiState

    data class PermissionDenied(
        val permanentlyDenied: Boolean,
    ) : RecordUiState

    data object Ready : RecordUiState

    data class StartingRecording(
        val tempFilePath: String,
    ) : RecordUiState

    data class Recording(
        val tempFilePath: String,
        val elapsedMs: Long,
    ) : RecordUiState

    data class Reviewing(
        val tempFilePath: String,
        val durationMs: Long,
        val description: String = "",
    ) : RecordUiState

    data object Saving : RecordUiState
    data object Done : RecordUiState

    data class Error(
        @param:StringRes val messageResId: Int,
    ) : RecordUiState
}

sealed interface RecordIntent {
    data class PermissionsResolved(
        val cameraGranted: Boolean,
        val audioGranted: Boolean,
        val permanentlyDenied: Boolean,
    ) : RecordIntent

    data object StartClicked : RecordIntent
    data class CameraRecordingStarted(val tempFilePath: String) : RecordIntent
    data class CameraRecordingDurationChanged(val elapsedMs: Long) : RecordIntent
    data class CameraRecordingFinalized(val tempFilePath: String, val durationMs: Long) :
        RecordIntent

    data class CameraRecordingFailed(val tempFilePath: String?) : RecordIntent
    data class DescriptionChanged(val description: String) : RecordIntent
    data object SaveClicked : RecordIntent
    data object DiscardClicked : RecordIntent
    data object RetryClicked : RecordIntent
}
