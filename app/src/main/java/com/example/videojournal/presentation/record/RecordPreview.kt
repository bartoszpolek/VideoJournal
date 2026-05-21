package com.example.videojournal.presentation.record

import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberPreviewCameraController(): LifecycleCameraController {
    val context = LocalContext.current
    return remember(context) { LifecycleCameraController(context) }
}
