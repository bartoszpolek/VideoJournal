package com.example.videojournal.presentation.record

import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.videojournal.R
import com.example.videojournal.presentation.design.VideoJournalTheme

@Composable
internal fun RecordReview(
    state: RecordUiState.Reviewing,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.record_review_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatRecordDuration(state.durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChanged,
            label = { Text(stringResource(R.string.record_description_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            minLines = 3,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.record_discard))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(stringResource(R.string.record_save))
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
private fun RecordReviewPreview() {
    VideoJournalTheme {
        RecordScreen(
            uiState = RecordUiState.Reviewing(
                tempFilePath = "/cache/video_recording/temp.mp4",
                durationMs = 12_000L,
                description = "Morning note",
            ),
            cameraController = rememberPreviewCameraController(),
            onIntent = {},
            onRequestPermissions = {},
            onOpenSettings = {},
            onStopRecording = {},
        )
    }
}
