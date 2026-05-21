package com.example.videojournal.presentation.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.videojournal.R
import com.example.videojournal.presentation.design.JournalReviewMediaBackground
import com.example.videojournal.presentation.design.JournalSpacing
import com.example.videojournal.presentation.design.VideoJournalTheme
import com.example.videojournal.presentation.media.VideoPlayer
import com.example.videojournal.presentation.media.rememberVideoExoPlayer
import com.example.videojournal.presentation.util.formatDuration

@Composable
internal fun RecordReview(
    state: RecordUiState.Reviewing,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exoPlayer = rememberVideoExoPlayer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        val previewHeight = when {
            maxHeight < 480.dp -> 180.dp
            maxHeight < 680.dp -> 260.dp
            else -> 420.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(JournalSpacing.space24),
            verticalArrangement = Arrangement.spacedBy(JournalSpacing.space20),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(JournalSpacing.space4)) {
                Text(
                    text = stringResource(R.string.record_review_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatDuration(state.durationMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .clip(MaterialTheme.shapes.small)
                    .background(JournalReviewMediaBackground),
            ) {
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    videoPath = state.tempFilePath,
                    playWhenReady = true,
                    useController = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChanged,
                label = { Text(stringResource(R.string.record_description_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.record_discard))
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.padding(start = JournalSpacing.space12),
                ) {
                    Text(stringResource(R.string.record_save))
                }
            }
        }
    }
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
