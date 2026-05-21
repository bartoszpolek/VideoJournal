package com.example.videojournal.app.startup

import com.example.videojournal.domain.media.VideoStorage

class CleanupTempRecordingsStartupTask(
    private val videoStorage: VideoStorage,
) : StartupTask {
    override suspend fun run() {
        videoStorage.cleanupTempRecordings()
    }
}
