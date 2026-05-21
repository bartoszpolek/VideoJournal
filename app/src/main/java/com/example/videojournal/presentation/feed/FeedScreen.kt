package com.example.videojournal.presentation.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.videojournal.R
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.presentation.design.JournalSpacing
import com.example.videojournal.presentation.design.VideoJournalTheme

@Composable
fun FeedScreen(
    uiState: FeedUiState,
    onIntent: (FeedIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        FeedUiState.Empty -> EmptyFeed(modifier)
        is FeedUiState.Error -> FeedMessage(
            title = stringResource(R.string.feed_error_unavailable),
            body = stringResource(uiState.messageResId),
            modifier = modifier,
        )
        FeedUiState.Loading -> LoadingFeed(modifier)
        is FeedUiState.Content -> FeedContent(
            state = uiState,
            onIntent = onIntent,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoadingFeed(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyFeed(modifier: Modifier = Modifier) {
    FeedMessage(
        title = stringResource(R.string.feed_empty_title),
        body = stringResource(R.string.feed_empty_body),
        modifier = modifier,
    )
}

@Composable
private fun FeedMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(JournalSpacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(JournalSpacing.space8))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedScreenPreview() {
    VideoJournalTheme {
        FeedScreen(
            uiState = FeedUiState.Content(
                items = listOf(
                    VideoEntry(
                        id = "preview",
                        filePath = "/files/videos/preview.mp4",
                        thumbnailPath = null,
                        description = "Morning check-in",
                        durationMs = 12_000,
                        createdAtMillis = 1_000,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}
