package com.example.videojournal.presentation.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import com.example.videojournal.R
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.presentation.design.VideoJournalTheme
import com.example.videojournal.presentation.media.VideoPlayer
import com.example.videojournal.presentation.media.rememberVideoExoPlayer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.mapNotNull

private val FeedPlaceholderColor = Color(0xFF101114)
private val FeedOnMediaColor = Color.White
private val FeedPlayIndicatorContainerColor = FeedOnMediaColor.copy(alpha = 0.18f)
private val FeedMutedTextColor = FeedOnMediaColor.copy(alpha = 0.4f)
private val FeedVideoShape = RoundedCornerShape(36.dp)

@Composable
fun FeedRoute(
    viewModel: FeedViewModel,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessageResId = (uiState as? FeedUiState.Content)?.userMessageResId
    val userMessage = userMessageResId?.let { stringResource(it) }

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage)
            viewModel.onIntent(FeedIntent.UserMessageShown)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        FeedScreen(
            uiState = uiState,
            onIntent = viewModel::onIntent,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        FloatingActionButton(
            onClick = onRecordClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.feed_record_video_content_description),
            )
        }
    }
}

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedContent(
    state: FeedUiState.Content,
    onIntent: (FeedIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { state.items.size })
    val exoPlayer = rememberVideoExoPlayer()
    val itemIds = remember(state.items) { state.items.map { it.id } }
    var previousItemIds by remember { mutableStateOf(itemIds) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDelete = pendingDeleteId?.let { id ->
        state.items.firstOrNull { it.id == id }
    }

    LaunchedEffect(itemIds) {
        val currentFirstItemId = itemIds.firstOrNull() ?: return@LaunchedEffect
        val previousIds = previousItemIds
        previousItemIds = itemIds

        if (previousIds.isNotEmpty() && currentFirstItemId !in previousIds) {
            pagerState.scrollToPage(0)
            onIntent(FeedIntent.PageChanged(currentFirstItemId))
        }
    }

    LaunchedEffect(pendingDelete, pendingDeleteId) {
        if (pendingDeleteId != null && pendingDelete == null) {
            pendingDeleteId = null
        }
    }

    LaunchedEffect(pagerState, state.items) {
        snapshotFlow { pagerState.currentPage }
            .mapNotNull { page -> state.items.getOrNull(page)?.id }
            .distinctUntilChanged()
            .drop(1)
            .collect { id -> onIntent(FeedIntent.PageChanged(id)) }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        key = { page -> state.items[page].id },
    ) { page ->
        val item = state.items[page]
        FeedPage(
            item = item,
            exoPlayer = exoPlayer,
            isActivePage = page == pagerState.currentPage,
            isPlaying = item.id == state.playingId,
            onVideoTapped = { onIntent(FeedIntent.VideoTapped(item.id)) },
            onDeleteRequested = { pendingDeleteId = item.id },
            modifier = Modifier.fillMaxSize(),
        )
    }

    pendingDelete?.let { video ->
        DeleteVideoDialog(
            video = video,
            onDismiss = { pendingDeleteId = null },
            onConfirm = {
                pendingDeleteId = null
                onIntent(FeedIntent.DeleteClicked(video.id))
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedPage(
    item: VideoEntry,
    exoPlayer: ExoPlayer,
    isActivePage: Boolean,
    isPlaying: Boolean,
    onVideoTapped: () -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 10.dp)
                .clip(FeedVideoShape)
                .background(FeedPlaceholderColor),
        ) {
            if (isActivePage) {
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    videoPath = item.filePath,
                    playWhenReady = isPlaying,
                    modifier = Modifier.fillMaxSize(),
                )
                if (!isPlaying) {
                    PlayIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                VideoPlaceholder(
                    item = item,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = onVideoTapped,
                        onLongClick = onDeleteRequested,
                    ),
            )

            IconButton(
                onClick = onDeleteRequested,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.feed_delete_video_content_description),
                    tint = FeedOnMediaColor,
                )
            }

            FeedMetadata(
                item = item,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 96.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun PlayIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .background(
                color = FeedPlayIndicatorContainerColor,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = FeedOnMediaColor,
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun VideoPlaceholder(
    item: VideoEntry,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(FeedPlaceholderColor),
        contentAlignment = Alignment.Center,
    ) {
        PlayIndicator()

        Text(
            text = item.description?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.feed_video_fallback_title),
            color = FeedMutedTextColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 96.dp),
        )
    }
}

@Composable
private fun FeedMetadata(
    item: VideoEntry,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = FeedOnMediaColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = formatDuration(item.durationMs),
                color = FeedOnMediaColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item.description
            ?.takeIf { it.isNotBlank() }
            ?.let { description ->
                Text(
                    text = description,
                    color = FeedOnMediaColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

@Composable
private fun DeleteVideoDialog(
    video: VideoEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feed_delete_dialog_title)) },
        text = {
            Text(
                text = video.description?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.feed_delete_dialog_body),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.feed_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feed_delete_dialog_dismiss))
            }
        },
    )
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
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
