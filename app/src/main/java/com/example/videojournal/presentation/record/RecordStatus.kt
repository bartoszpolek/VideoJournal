package com.example.videojournal.presentation.record

import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.videojournal.R
import com.example.videojournal.presentation.design.VideoJournalTheme

@Composable
internal fun RecordPermissionDenied(
    permanentlyDenied: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecordStatus(
        title = stringResource(R.string.record_camera_permission_title),
        body = stringResource(R.string.record_camera_permission_body),
        modifier = modifier,
        action = {
            Button(
                onClick = if (permanentlyDenied) onOpenSettings else onRequestPermissions,
            ) {
                Text(
                    text = stringResource(
                        if (permanentlyDenied) {
                            R.string.record_camera_permission_settings
                        } else {
                            R.string.record_camera_permission_button
                        },
                    ),
                )
            }
        },
    )
}

@Composable
internal fun RecordStatus(
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        body?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        action?.let {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(width = 220.dp, height = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                it()
            }
        }
    }
}

@Composable
private fun rememberPreviewCameraController(): LifecycleCameraController {
    val context = LocalContext.current
    return remember(context) { LifecycleCameraController(context) }
}

@Preview(showBackground = true)
@Composable
private fun RecordPermissionPreview() {
    VideoJournalTheme {
        RecordScreen(
            uiState = RecordUiState.PermissionDenied(permanentlyDenied = false),
            cameraController = rememberPreviewCameraController(),
            onIntent = {},
            onRequestPermissions = {},
            onOpenSettings = {},
            onStopRecording = {},
        )
    }
}
