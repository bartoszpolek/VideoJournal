package com.example.videojournal.presentation.record

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.util.concurrent.TimeUnit

private const val RecordingDurationLimitMs = 60_000L

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

    LockOrientationWhenRecording(
        enabled = uiState is RecordUiState.StartingRecording || uiState is RecordUiState.Recording,
        activity = activity,
    )

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissionResults ->
        val cameraGranted = permissionResults[Manifest.permission.CAMERA] == true
        val audioGranted = permissionResults[Manifest.permission.RECORD_AUDIO] == true
        val deniedPermissions = permissionResults
            .filterValues { granted -> !granted }
            .keys
        val permanentlyDenied = activity.hasPermanentlyDeniedAny(deniedPermissions)

        viewModel.onIntent(
            RecordIntent.PermissionsResolved(
                cameraGranted = cameraGranted,
                audioGranted = audioGranted,
                permanentlyDenied = permanentlyDenied,
            ),
        )
    }

    fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    }

    LaunchedEffect(uiState, context) {
        if (uiState != RecordUiState.CheckingPermissions) return@LaunchedEffect

        val cameraGranted = context.hasPermission(Manifest.permission.CAMERA)
        val audioGranted = context.hasPermission(Manifest.permission.RECORD_AUDIO)

        if (cameraGranted && audioGranted) {
            viewModel.onIntent(
                RecordIntent.PermissionsResolved(
                    cameraGranted = true,
                    audioGranted = true,
                    permanentlyDenied = false,
                ),
            )
        } else {
            requestPermissions()
        }
    }

    LaunchedEffect(uiState, cameraController, context) {
        val state = uiState as? RecordUiState.StartingRecording ?: return@LaunchedEffect

        activeRecording?.stop()
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
        onOpenSettings = { context.openApplicationSettings() },
        onStopRecording = {
            activeRecording?.stop()
            activeRecording = null
        },
        modifier = modifier,
    )
}

@Composable
private fun LockOrientationWhenRecording(
    enabled: Boolean,
    activity: Activity?,
) {
    DisposableEffect(enabled, activity) {
        if (enabled && activity != null) {
            val previousOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

            onDispose {
                activity.requestedOrientation = previousOrientation
            }
        } else {
            onDispose { }
        }
    }
}

@SuppressLint("MissingPermission")
private fun startCameraRecording(
    context: Context,
    cameraController: LifecycleCameraController,
    tempFilePath: String,
    onDurationChanged: (Long) -> Unit,
    onFinalized: (String, Long) -> Unit,
    onFailure: (String) -> Unit,
): Recording? =
    runCatching {
        val outputOptions = FileOutputOptions.Builder(File(tempFilePath))
            .setDurationLimitMillis(RecordingDurationLimitMs)
            .build()
        val audioConfig = AudioConfig.create(true)

        cameraController.startRecording(
            outputOptions,
            audioConfig,
            ContextCompat.getMainExecutor(context),
        ) { event ->
            val durationMs = event.recordingStats.recordedDurationNanos.toMillis()
            when (event) {
                is VideoRecordEvent.Status -> onDurationChanged(durationMs)
                is VideoRecordEvent.Finalize -> {
                    if (event.isSuccessfulFinalize()) {
                        onFinalized(tempFilePath, durationMs)
                    } else {
                        onFailure(tempFilePath)
                    }
                }
                else -> Unit
            }
        }
    }.getOrElse {
        onFailure(tempFilePath)
        null
    }

private fun VideoRecordEvent.Finalize.isSuccessfulFinalize(): Boolean =
    !hasError() || error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED

private fun Long.toMillis(): Long =
    TimeUnit.NANOSECONDS.toMillis(this)

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Activity?.hasPermanentlyDeniedAny(permissions: Collection<String>): Boolean =
    this != null && permissions.any { permission ->
        !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }

private fun Context.openApplicationSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    startActivity(intent)
}
