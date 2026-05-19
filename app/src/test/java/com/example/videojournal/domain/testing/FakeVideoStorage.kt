package com.example.videojournal.domain.testing

import com.example.videojournal.domain.media.VideoStorage

class FakeVideoStorage : VideoStorage {
    var tempRecordingPath = "/cache/video_recording/temp.mp4"
    var promotedPath = "/files/videos/final.mp4"
    var deleteException: Throwable? = null

    val createdTempFiles = mutableListOf<String>()
    val promotedTempPaths = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()
    var cleanupCalls = 0

    override suspend fun createTempRecordingFile(): String {
        createdTempFiles += tempRecordingPath
        return tempRecordingPath
    }

    override suspend fun promoteTempRecording(tempPath: String): String {
        promotedTempPaths += tempPath
        return promotedPath
    }

    override suspend fun deleteFile(path: String) {
        deletedPaths += path
        deleteException?.let { throw it }
    }

    override suspend fun cleanupTempRecordings() {
        cleanupCalls += 1
    }
}
