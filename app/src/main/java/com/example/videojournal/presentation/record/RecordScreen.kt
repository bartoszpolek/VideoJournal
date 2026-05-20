package com.example.videojournal.presentation.record

import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.videojournal.R

private val RecordPreviewFallbackColor = Color(0xFF101114)
private val RecordOverlayColor = Color.Black.copy(alpha = 0.48f)
private val RecordOnMediaColor = Color.White

@Composable
fun RecordScreen(
    uiState: RecordUiState,
    cameraController: LifecycleCameraController,
    onIntent: (RecordIntent) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        RecordUiState.CheckingPermissions -> RecordStatus(
            title = stringResource(R.string.record_camera_permission_title),
            body = stringResource(R.string.record_camera_permission_body),
            modifier = modifier,
            action = { CircularProgressIndicator() },
        )
        is RecordUiState.PermissionDenied -> RecordPermissionDenied(
            permanentlyDenied = uiState.permanentlyDenied,
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
        RecordUiState.Ready -> RecordCameraContent(
            cameraController = cameraController,
            topLabel = stringResource(R.string.record_ready_title),
            bottomContent = {
                Button(onClick = { onIntent(RecordIntent.StartClicked) }) {
                    Text(stringResource(R.string.record_start))
                }
            },
            onCameraUnavailable = {
                onIntent(RecordIntent.CameraRecordingFailed(tempFilePath = null))
            },
            modifier = modifier,
        )
        is RecordUiState.StartingRecording -> RecordCameraContent(
            cameraController = cameraController,
            topLabel = stringResource(R.string.record_starting),
            bottomContent = {
                CircularProgressIndicator(color = RecordOnMediaColor)
            },
            onCameraUnavailable = {
                onIntent(RecordIntent.CameraRecordingFailed(uiState.tempFilePath))
            },
            modifier = modifier,
        )
        is RecordUiState.Recording -> RecordCameraContent(
            cameraController = cameraController,
            topLabel = formatRecordDuration(uiState.elapsedMs),
            bottomContent = {
                Button(onClick = onStopRecording) {
                    Text(stringResource(R.string.record_stop))
                }
            },
            onCameraUnavailable = {
                onIntent(RecordIntent.CameraRecordingFailed(uiState.tempFilePath))
            },
            modifier = modifier,
        )
        is RecordUiState.Reviewing -> RecordReview(
            state = uiState,
            onDescriptionChanged = { description ->
                onIntent(RecordIntent.DescriptionChanged(description))
            },
            onSave = { onIntent(RecordIntent.SaveClicked) },
            onDiscard = { onIntent(RecordIntent.DiscardClicked) },
            modifier = modifier,
        )
        RecordUiState.Saving -> RecordStatus(
            title = stringResource(R.string.record_saving),
            body = null,
            modifier = modifier,
            action = { CircularProgressIndicator() },
        )
        RecordUiState.Done -> RecordStatus(
            title = stringResource(R.string.record_done),
            body = null,
            modifier = modifier,
        )
        is RecordUiState.Error -> RecordStatus(
            title = stringResource(uiState.messageResId),
            body = null,
            modifier = modifier,
            action = {
                Button(onClick = { onIntent(RecordIntent.RetryClicked) }) {
                    Text(stringResource(R.string.record_try_again))
                }
            },
        )
    }
}

@Composable
private fun RecordCameraContent(
    cameraController: LifecycleCameraController,
    topLabel: String,
    bottomContent: @Composable () -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RecordPreviewFallbackColor),
    ) {
        CameraPreview(
            cameraController = cameraController,
            onBindFailed = onCameraUnavailable,
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            color = RecordOverlayColor,
            contentColor = RecordOnMediaColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = topLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Surface(
            color = RecordOverlayColor,
            contentColor = RecordOnMediaColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                bottomContent()
            }
        }
    }
}
