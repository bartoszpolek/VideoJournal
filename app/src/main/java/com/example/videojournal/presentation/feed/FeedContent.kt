package com.example.videojournal.presentation.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.videojournal.R
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.presentation.media.rememberVideoExoPlayer
import com.example.videojournal.presentation.util.findActivity
import com.example.videojournal.presentation.util.shareVideoFile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.mapNotNull
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FeedContent(
    state: FeedUiState.Content,
    onIntent: (FeedIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { state.items.size })
    val exoPlayer = rememberVideoExoPlayer()
    val activity = LocalContext.current.findActivity()
    val shareChooserTitle = stringResource(R.string.feed_share_video_chooser_title)
    val itemIds = remember(state.items) { state.items.map { it.id } }
    var previousItemIds by rememberSaveable { mutableStateOf(itemIds) }
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
            onShareRequested = {
                activity?.shareVideoFile(
                    file = File(item.filePath),
                    chooserTitle = shareChooserTitle,
                )
            },
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
