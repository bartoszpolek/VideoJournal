package com.example.videojournal.domain.media

interface ThumbnailGenerator {
    /**
     * Creates a thumbnail for [videoPath] and returns its file path.
     *
     * Returns null when thumbnail generation fails or is intentionally skipped.
     */
    suspend fun generate(videoPath: String): String?
}
