package com.example.videojournal.domain.testing

import com.example.videojournal.domain.media.ThumbnailGenerator

class FakeThumbnailGenerator : ThumbnailGenerator {
    var result: String? = null
    var exception: Throwable? = null
    val requestedVideoPaths = mutableListOf<String>()

    override suspend fun generate(videoPath: String): String? {
        requestedVideoPaths += videoPath
        exception?.let { throw it }
        return result
    }
}
