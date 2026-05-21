package com.example.videojournal.presentation.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.example.videojournal.R
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.presentation.design.JournalMediaBackground
import com.example.videojournal.presentation.design.JournalMediaControlContainer
import com.example.videojournal.presentation.design.JournalMediaShape
import com.example.videojournal.presentation.design.JournalOnMedia
import com.example.videojournal.presentation.design.JournalSpacing
import com.example.videojournal.presentation.media.VideoPlayer
import java.io.File

private val MediaActionsReservedWidth = 96.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FeedPage(
    item: VideoEntry,
    exoPlayer: ExoPlayer,
    isActivePage: Boolean,
    isPlaying: Boolean,
    onVideoTapped: () -> Unit,
    onDeleteRequested: () -> Unit,
    onShareRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasPlayedWhileActive by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(isActivePage, isPlaying) {
        hasPlayedWhileActive = when {
            !isActivePage -> false
            isPlaying -> true
            else -> hasPlayedWhileActive
        }
    }
    val showPlayer = isActivePage && (isPlaying || hasPlayedWhileActive)

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
                .padding(horizontal = JournalSpacing.space6, vertical = JournalSpacing.space10)
                .clip(JournalMediaShape)
                .background(JournalMediaBackground),
        ) {
            if (showPlayer) {
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    videoPath = item.filePath,
                    playWhenReady = isPlaying,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (!showPlayer) {
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

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(JournalSpacing.space16),
                verticalArrangement = Arrangement.spacedBy(JournalSpacing.space8),
            ) {
                IconButton(onClick = onShareRequested) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.feed_share_video_content_description),
                        tint = JournalOnMedia,
                    )
                }

                IconButton(onClick = onDeleteRequested) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.feed_delete_video_content_description),
                        tint = JournalOnMedia,
                    )
                }
            }

            FeedMetadata(
                item = item,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = JournalSpacing.space20,
                        end = MediaActionsReservedWidth,
                        bottom = JournalSpacing.space20,
                    ),
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
                color = JournalMediaControlContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = JournalOnMedia,
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
        modifier = modifier.background(JournalMediaBackground),
        contentAlignment = Alignment.Center,
    ) {
        item.thumbnailPath?.let { thumbnailPath ->
            AsyncImage(
                model = File(thumbnailPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        PlayIndicator()
    }
}

@Composable
private fun FeedMetadata(
    item: VideoEntry,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(JournalSpacing.space8),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(JournalSpacing.space10),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = JournalOnMedia,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = formatDuration(item.durationMs),
                color = JournalOnMedia,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item.description
            ?.takeIf { it.isNotBlank() }
            ?.let { description ->
                Text(
                    text = description,
                    color = JournalOnMedia,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
