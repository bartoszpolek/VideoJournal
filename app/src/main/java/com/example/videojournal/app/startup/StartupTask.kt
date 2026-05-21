package com.example.videojournal.app.startup

interface StartupTask {
    suspend fun run()
}
