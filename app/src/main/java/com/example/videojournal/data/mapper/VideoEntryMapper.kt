package com.example.videojournal.data.mapper

import com.example.videojournal.data.database.VideoEntryEntity
import com.example.videojournal.domain.model.VideoEntry

fun VideoEntryEntity.toDomain(): VideoEntry =
    VideoEntry(
        id = id,
        filePath = filePath,
        thumbnailPath = thumbnailPath,
        description = description,
        durationMs = durationMs,
        createdAtMillis = createdAtMillis,
    )

fun VideoEntry.toEntity(): VideoEntryEntity =
    VideoEntryEntity(
        id = id,
        filePath = filePath,
        thumbnailPath = thumbnailPath,
        description = description,
        durationMs = durationMs,
        createdAtMillis = createdAtMillis,
    )
