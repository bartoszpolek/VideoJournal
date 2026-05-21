package com.example.videojournal.data.media

import java.io.File
import java.io.IOException

internal fun File.ensureDirectory() {
    if (exists()) {
        if (!isDirectory) {
            throw IOException("Expected directory, got file: $absolutePath")
        }
        return
    }
    if (!mkdirs()) {
        throw IOException("Could not create directory: $absolutePath")
    }
}
