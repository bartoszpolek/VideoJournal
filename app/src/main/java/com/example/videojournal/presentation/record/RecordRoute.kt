package com.example.videojournal.presentation.record

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.Recording
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videojournal.presentation.util.findActivity

@Composable
fun RecordRoute(
    viewModel: RecordViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        }
    }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    val currentActiveRecording by rememberUpdatedState(activeRecording)

    LockOrientationWhenRecording(
        enabled = uiState is RecordUiState.StartingRecording || uiState is RecordUiState.Recording,
        activity = activity,
    )

    fun stopActiveRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    DisposableEffect(Unit) {
        onDispose {
            currentActiveRecording?.stop()
            activeRecording = null
        }
    }

    fun resolvePermissions() {
        viewModel.onIntent(context.currentRecordPermissions(activity))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        resolvePermissions()
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        resolvePermissions()
    }

    fun requestPermissions() {
        permissionLauncher.launch(RecordPermissions)
    }

    LaunchedEffect(uiState, context, activity) {
        if (uiState != RecordUiState.CheckingPermissions) return@LaunchedEffect

        val permissions = context.currentRecordPermissions(activity)

        if (permissions.cameraGranted && permissions.audioGranted) {
            viewModel.onIntent(permissions)
        } else {
            requestPermissions()
        }
    }

    LaunchedEffect(uiState, cameraController, context) {
        val state = uiState as? RecordUiState.StartingRecording ?: return@LaunchedEffect

        stopActiveRecording()
        activeRecording = startCameraRecording(
            context = context,
            cameraController = cameraController,
            tempFilePath = state.tempFilePath,
            onDurationChanged = { durationMs ->
                viewModel.onIntent(RecordIntent.CameraRecordingDurationChanged(durationMs))
            },
            onFinalized = { tempFilePath, durationMs ->
                activeRecording = null
                viewModel.onIntent(
                    RecordIntent.CameraRecordingFinalized(
                        tempFilePath = tempFilePath,
                        durationMs = durationMs,
                    ),
                )
            },
            onFailure = { tempFilePath ->
                activeRecording = null
                viewModel.onIntent(RecordIntent.CameraRecordingFailed(tempFilePath))
            },
        )
        if (activeRecording != null) {
            viewModel.onIntent(RecordIntent.CameraRecordingStarted(state.tempFilePath))
        }
    }

    LaunchedEffect(uiState) {
        if (uiState == RecordUiState.Done) {
            onDone()
        }
    }

    RecordScreen(
        uiState = uiState,
        cameraController = cameraController,
        onIntent = viewModel::onIntent,
        onRequestPermissions = ::requestPermissions,
        onOpenSettings = { settingsLauncher.launch(context.applicationSettingsIntent()) },
        onStopRecording = ::stopActiveRecording,
        modifier = modifier,
    )
}
