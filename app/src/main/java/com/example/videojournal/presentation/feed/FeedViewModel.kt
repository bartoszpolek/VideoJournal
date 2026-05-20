package com.example.videojournal.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videojournal.R
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.usecase.DeleteVideoUseCase
import com.example.videojournal.domain.usecase.ObserveVideosUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FeedViewModel(
    observeVideosUseCase: ObserveVideosUseCase,
    private val deleteVideoUseCase: DeleteVideoUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        observeVideosUseCase()
            .onEach { videos ->
                _uiState.value = videos.toUiState()
            }
            .catch {
                _uiState.value = FeedUiState.Error(
                    messageResId = R.string.feed_error_load,
                )
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: FeedIntent) {
        when (intent) {
            is FeedIntent.DeleteClicked -> onDeleteClicked(intent.id)
            is FeedIntent.PageChanged -> onPageChanged(intent.id)
            FeedIntent.UserMessageShown -> onUserMessageShown()
            is FeedIntent.VideoTapped -> onVideoTapped(intent.id)
        }
    }

    private fun onVideoTapped(id: String) {
        updateContent { state ->
            if (state.items.none { it.id == id }) return@updateContent state

            state.copy(
                playingId = if (state.playingId == id) null else id,
            )
        }
    }

    private fun onPageChanged(id: String) {
        updateContent { state ->
            if (state.items.none { it.id == id }) state else state.copy(playingId = null)
        }
    }

    private fun onDeleteClicked(id: String) {
        val video = currentVideo(id) ?: return

        viewModelScope.launch {
            try {
                deleteVideoUseCase(video)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                updateContent { state ->
                    state.copy(userMessageResId = R.string.feed_error_delete)
                }
            }
        }
    }

    private fun onUserMessageShown() {
        updateContent { state -> state.copy(userMessageResId = null) }
    }

    private fun List<VideoEntry>.toUiState(): FeedUiState {
        if (isEmpty()) return FeedUiState.Empty

        val previousState = _uiState.value as? FeedUiState.Content
        val currentPlayingId = previousState
            ?.playingId
            ?.takeIf { playingId -> any { it.id == playingId } }

        return FeedUiState.Content(
            items = this,
            playingId = currentPlayingId,
            userMessageResId = previousState?.userMessageResId,
        )
    }

    private fun currentVideo(id: String): VideoEntry? =
        currentContent()?.items?.firstOrNull { it.id == id }

    private fun currentContent(): FeedUiState.Content? =
        _uiState.value as? FeedUiState.Content

    private inline fun updateContent(
        transform: (FeedUiState.Content) -> FeedUiState.Content,
    ) {
        val state = currentContent() ?: return
        _uiState.value = transform(state)
    }
}
