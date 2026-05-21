package com.example.videojournal.presentation.util

internal fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
