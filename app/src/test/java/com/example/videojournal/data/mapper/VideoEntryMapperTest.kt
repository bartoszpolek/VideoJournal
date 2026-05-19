package com.example.videojournal.data.mapper

import com.example.videojournal.domain.model.VideoEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoEntryMapperTest {
    @Test
    fun domainEntityRoundTripPreservesValues() {
        val video = VideoEntry(
            id = "video-id",
            filePath = "/files/videos/video.mp4",
            thumbnailPath = "/files/thumbnails/video.jpg",
            description = "description",
            durationMs = 5_000L,
            createdAtMillis = 123L,
        )

        assertEquals(video, video.toEntity().toDomain())
    }
}
