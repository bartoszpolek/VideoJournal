package com.example.videojournal.presentation.record

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreview(
    cameraController: LifecycleCameraController,
    onBindFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(cameraController, lifecycleOwner) {
        runCatching {
            cameraController.bindToLifecycle(lifecycleOwner)
        }.onFailure {
            onBindFailed()
        }

        onDispose {
            cameraController.unbind()
        }
    }

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                controller = cameraController
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            previewView.controller = cameraController
        },
        modifier = modifier,
    )
}
