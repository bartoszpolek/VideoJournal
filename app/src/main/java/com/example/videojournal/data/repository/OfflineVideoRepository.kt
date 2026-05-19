package com.example.videojournal.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.videojournal.data.database.VideoJournalDatabase
import com.example.videojournal.data.mapper.toDomain
import com.example.videojournal.data.mapper.toEntity
import com.example.videojournal.domain.model.VideoEntry
import com.example.videojournal.domain.repository.VideoRepository
import com.example.videojournal.domain.util.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OfflineVideoRepository(
    database: VideoJournalDatabase,
    private val dispatcherProvider: DispatcherProvider,
) : VideoRepository {
    private val queries = database.videoEntryQueries

    override fun observeVideos(): Flow<List<VideoEntry>> =
        queries.selectAll()
            .asFlow()
            .mapToList(dispatcherProvider.io)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(video: VideoEntry) {
        val entity = video.toEntity()
        withContext(dispatcherProvider.io) {
            queries.insertOrReplace(
                id = entity.id,
                filePath = entity.filePath,
                thumbnailPath = entity.thumbnailPath,
                description = entity.description,
                durationMs = entity.durationMs,
                createdAtMillis = entity.createdAtMillis,
            )
        }
    }

    override suspend fun deleteById(id: String) {
        withContext(dispatcherProvider.io) {
            queries.deleteById(id)
        }
    }
}
