package com.example.videojournal.app

import android.app.Application
import com.example.videojournal.app.di.appModules
import com.example.videojournal.domain.media.VideoStorage
import com.example.videojournal.domain.util.runCatchingNonCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VideoJournalApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val koinApplication = startKoin {
            androidContext(this@VideoJournalApplication)
            modules(appModules)
        }

        val videoStorage = koinApplication.koin.get<VideoStorage>()
        applicationScope.launch {
            runCatchingNonCancellation {
                videoStorage.cleanupTempRecordings()
            }
        }
    }
}
