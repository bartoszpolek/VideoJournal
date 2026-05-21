package com.example.videojournal.presentation.record

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.TimeUnit

private const val RecordingDurationLimitMs = 60_000L

@SuppressLint("MissingPermission")
internal fun startCameraRecording(
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
