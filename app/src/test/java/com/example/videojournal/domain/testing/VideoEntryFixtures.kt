package com.example.videojournal.domain.testing

import com.example.videojournal.domain.model.VideoEntry

fun videoEntry(
    id: String = "video-id",
    filePath: String = "/files/videos/$id.mp4",
    thumbnailPath: String? = "/files/thumbnails/$id.jpg",
    description: String? = "description $id",
    durationMs: Long = 5_000L,
    createdAtMillis: Long = 1_000L,
): VideoEntry =
    VideoEntry(
        id = id,
        filePath = filePath,
        thumbnailPath = thumbnailPath,
        description = description,
        durationMs = durationMs,
        createdAtMillis = createdAtMillis,
    )
