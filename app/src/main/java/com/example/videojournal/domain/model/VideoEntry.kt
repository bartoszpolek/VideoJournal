package com.example.videojournal.domain.model

data class VideoEntry(
    val id: String,
    val filePath: String,
    val thumbnailPath: String?,
    val description: String?,
    val durationMs: Long,
    val createdAtMillis: Long,
)
