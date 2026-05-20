package com.example.videojournal.presentation.feed

import androidx.annotation.StringRes
import com.example.videojournal.domain.model.VideoEntry

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState

    data class Content(
        val items: List<VideoEntry>,
        val playingId: String? = null,
        @param:StringRes val userMessageResId: Int? = null,
    ) : FeedUiState

    data class Error(@param:StringRes val messageResId: Int) : FeedUiState
}

sealed interface FeedIntent {
    data class VideoTapped(val id: String) : FeedIntent
    data class PageChanged(val id: String) : FeedIntent
    data class DeleteClicked(val id: String) : FeedIntent
    data object UserMessageShown : FeedIntent
}
