package com.example.videojournal.app

import android.app.Application
import com.example.videojournal.app.di.appModules
import com.example.videojournal.app.startup.StartupTask
import com.example.videojournal.domain.util.DispatcherProvider
import com.example.videojournal.domain.util.runCatchingNonCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VideoJournalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val koinApplication = startKoin {
            androidContext(this@VideoJournalApplication)
            modules(appModules)
        }

        val koin = koinApplication.koin
        val dispatcherProvider = koin.get<DispatcherProvider>()
        val startupTasks = koin.getAll<StartupTask>()
        val applicationScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
        applicationScope.launch {
            startupTasks.forEach { task ->
                runCatchingNonCancellation {
                    task.run()
                }
            }
        }
    }
}
