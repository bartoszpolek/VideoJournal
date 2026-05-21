package com.example.videojournal.data.media

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidVideoStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `create temp recording file creates mp4 inside cache recording directory`() = runTest {
        val storage = createStorage(idProvider = { "temp-id" })

        val path = storage.createTempRecordingFile()

        val file = File(path)
        assertTrue(file.exists())
        assertTrue(file.isFile)
        assertEquals("temp-id.mp4", file.name)
        assertEquals(
            AndroidVideoStorage.TEMP_RECORDINGS_DIRECTORY,
            file.parentFile?.name,
        )
        assertEquals("cache", file.parentFile?.parentFile?.name)
    }

    @Test
    fun `promote temp recording moves file into videos directory`() = runTest {
        val storage = createStorage(idProvider = { "final-id" })
        val tempFile = temporaryFolder.newFile("recording.mp4").apply {
            writeText("video bytes")
        }

        val finalPath = storage.promoteTempRecording(tempFile.absolutePath)

        val finalFile = File(finalPath)
        assertTrue(finalFile.exists())
        assertEquals("video bytes", finalFile.readText())
        assertEquals("final-id.mp4", finalFile.name)
        assertEquals(AndroidVideoStorage.VIDEOS_DIRECTORY, finalFile.parentFile?.name)
        assertEquals("files", finalFile.parentFile?.parentFile?.name)
        assertFalse(tempFile.exists())
    }

    @Test
    fun `delete file removes existing file and ignores missing file`() = runTest {
        val storage = createStorage()
        val file = temporaryFolder.newFile("saved.mp4")

        storage.deleteFile(file.absolutePath)
        storage.deleteFile(file.absolutePath)

        assertFalse(file.exists())
    }

    @Test
    fun `cleanup temp recordings removes temp recording directory`() = runTest {
        val storage = createStorage()
        val tempDirectory =
            File(temporaryFolder.root, "cache/${AndroidVideoStorage.TEMP_RECORDINGS_DIRECTORY}")
        assertTrue(tempDirectory.mkdirs())
        val abandonedRecording = File(tempDirectory, "abandoned.mp4").apply {
            writeText("video bytes")
        }
        val nonRecordingFile = File(tempDirectory, "keep.txt").apply {
            writeText("metadata")
        }

        storage.cleanupTempRecordings()

        assertFalse(abandonedRecording.exists())
        assertFalse(nonRecordingFile.exists())
        assertFalse(tempDirectory.exists())
    }

    private fun createStorage(
        idProvider: () -> String = { "id" },
    ): AndroidVideoStorage =
        AndroidVideoStorage(
            filesDir = File(temporaryFolder.root, "files"),
            cacheDir = File(temporaryFolder.root, "cache"),
            idProvider = idProvider,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
}
