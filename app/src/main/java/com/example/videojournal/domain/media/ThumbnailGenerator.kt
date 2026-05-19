package com.example.videojournal.domain.media

interface ThumbnailGenerator {
    suspend fun generate(videoPath: String): String?
}
