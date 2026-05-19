package com.example.videojournal.data.mapper

import com.example.videojournal.domain.model.VideoEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoEntryMapperTest {
    @Test
    fun `entity maps to domain preserving values`() {
        val expected = VideoEntry(
            id = "video-id",
            filePath = "/files/videos/video.mp4",
            thumbnailPath = "/files/thumbnails/video.jpg",
            description = "description",
            durationMs = 5_000L,
            createdAtMillis = 123L,
        )
        val entity = com.example.videojournal.data.database.VideoEntryEntity(
            id = expected.id,
            filePath = expected.filePath,
            thumbnailPath = expected.thumbnailPath,
            description = expected.description,
            durationMs = expected.durationMs,
            createdAtMillis = expected.createdAtMillis,
        )

        assertEquals(expected, entity.toDomain())
    }
}
