package com.example.videojournal.app.di

import com.example.videojournal.app.startup.CleanupTempRecordingsStartupTask
import com.example.videojournal.app.startup.StartupTask
import org.koin.dsl.module

val startupModule = module {
    single<StartupTask> {
        CleanupTempRecordingsStartupTask(
            videoStorage = get(),
        )
    }
}
