package com.example.videojournal.presentation.record

import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.videojournal.R
import com.example.videojournal.presentation.design.JournalMediaBackground
import com.example.videojournal.presentation.design.JournalMediaShape
import com.example.videojournal.presentation.design.JournalOnMedia
import com.example.videojournal.presentation.design.JournalRecordCapture
import com.example.videojournal.presentation.design.JournalRecordTimerShadow
import com.example.videojournal.presentation.design.JournalSpacing

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
        )
        is RecordUiState.PermissionDenied -> RecordPermissionDenied(
            permanentlyDenied = uiState.permanentlyDenied,
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
        RecordUiState.Ready,
        is RecordUiState.StartingRecording,
        is RecordUiState.Recording,
        -> {
            val cameraContentState = uiState.toRecordCameraContentState()

            RecordCameraContent(
                cameraController = cameraController,
                topLabel = cameraContentState.topLabel,
                controlsState = cameraContentState.controlsState,
                onStart = { onIntent(RecordIntent.StartClicked) },
                onStop = onStopRecording,
                onCameraUnavailable = {
                    onIntent(RecordIntent.CameraRecordingFailed(cameraContentState.tempFilePath))
                },
                modifier = modifier,
            )
        }
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

private enum class RecordCameraControlsState {
    Start,
    Starting,
    Stop,
}

private data class RecordCameraContentState(
    val topLabel: String,
    val tempFilePath: String?,
    val controlsState: RecordCameraControlsState,
)

@Composable
private fun RecordUiState.toRecordCameraContentState(): RecordCameraContentState =
    when (this) {
        RecordUiState.Ready -> RecordCameraContentState(
            topLabel = stringResource(R.string.record_ready_title),
            tempFilePath = null,
            controlsState = RecordCameraControlsState.Start,
        )
        is RecordUiState.StartingRecording -> RecordCameraContentState(
            topLabel = stringResource(R.string.record_starting),
            tempFilePath = tempFilePath,
            controlsState = RecordCameraControlsState.Starting,
        )
        is RecordUiState.Recording -> RecordCameraContentState(
            topLabel = formatRecordDuration(elapsedMs),
            tempFilePath = tempFilePath,
            controlsState = RecordCameraControlsState.Stop,
        )
        else -> error("State does not render camera content: $this")
    }

@Composable
private fun RecordCameraControls(
    controlsState: RecordCameraControlsState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    when (controlsState) {
        RecordCameraControlsState.Start -> {
            RecordCaptureButton(
                isRecording = false,
                contentDescription = stringResource(R.string.record_start),
                onClick = onStart,
            )
        }
        RecordCameraControlsState.Starting -> {
            RecordCaptureButton(
                isRecording = true,
                contentDescription = stringResource(R.string.record_stop),
                onClick = onStop,
            )
        }
        RecordCameraControlsState.Stop -> {
            RecordCaptureButton(
                isRecording = true,
                contentDescription = stringResource(R.string.record_stop),
                onClick = onStop,
            )
        }
    }
}

@Composable
private fun RecordCameraContent(
    cameraController: LifecycleCameraController,
    topLabel: String,
    controlsState: RecordCameraControlsState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = JournalSpacing.space16)
            .padding(top = 22.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JournalSpacing.space24),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(JournalMediaShape)
                .background(JournalMediaBackground),
        ) {
            CameraPreview(
                cameraController = cameraController,
                onBindFailed = onCameraUnavailable,
                modifier = Modifier.fillMaxSize(),
            )

            Text(
                text = topLabel,
                color = JournalOnMedia,
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(
                        color = JournalRecordTimerShadow,
                        offset = Offset(x = 0f, y = 2f),
                        blurRadius = 8f,
                    ),
                ),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = JournalSpacing.space24),
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            RecordCameraControls(
                controlsState = controlsState,
                onStart = onStart,
                onStop = onStop,
            )
        }
    }
}

@Composable
private fun RecordCaptureButton(
    isRecording: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ringColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)

    Box(
        modifier = modifier
            .size(96.dp)
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
            }
            .border(width = 4.dp, color = ringColor, shape = CircleShape)
            .padding(JournalSpacing.space8),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 38.dp else 68.dp)
                .background(
                    color = JournalRecordCapture,
                    shape = if (isRecording) MaterialTheme.shapes.medium else CircleShape,
                ),
        )
    }
}
