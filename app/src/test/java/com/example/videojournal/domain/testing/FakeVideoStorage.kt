package com.example.videojournal.domain.testing

import com.example.videojournal.domain.media.VideoStorage

class FakeVideoStorage : VideoStorage {
    var tempRecordingPath = "/cache/video_recording/temp.mp4"
    var promotedPath = "/files/videos/final.mp4"
    var promoteException: Throwable? = null
    var deleteException: Throwable? = null

    val createdTempFiles = mutableListOf<String>()
    val promotedTempPaths = mutableListOf<String>()
    val deleteAttempts = mutableListOf<String>()
    var cleanupCalls = 0

    override suspend fun createTempRecordingFile(): String {
        createdTempFiles += tempRecordingPath
        return tempRecordingPath
    }

    override suspend fun promoteTempRecording(tempFilePath: String): String {
        promotedTempPaths += tempFilePath
        promoteException?.let { throw it }
        return promotedPath
    }

    override suspend fun deleteFile(path: String) {
        deleteAttempts += path
        deleteException?.let { throw it }
    }

    override suspend fun cleanupTempRecordings() {
        cleanupCalls += 1
    }
}
