package com.example.videojournal.domain.media

interface VideoStorage {
    /**
     * Creates a writable temp file path for an in-progress recording.
     */
    suspend fun createTempRecordingFile(): String

    /**
     * Moves or copies [tempFilePath] into permanent app storage and returns the final video path.
     */
    suspend fun promoteTempRecording(tempFilePath: String): String

    /**
     * Deletes the file at [path]. Implementations should treat missing files as success.
     */
    suspend fun deleteFile(path: String)

    /**
     * Removes abandoned temp recordings that are not represented by saved journal entries.
     */
    suspend fun cleanupTempRecordings()
}
