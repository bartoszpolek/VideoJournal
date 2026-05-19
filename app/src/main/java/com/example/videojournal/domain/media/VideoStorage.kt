package com.example.videojournal.domain.media

interface VideoStorage {
    suspend fun createTempRecordingFile(): String

    suspend fun promoteTempRecording(tempPath: String): String

    suspend fun deleteFile(path: String)

    suspend fun cleanupTempRecordings()
}
