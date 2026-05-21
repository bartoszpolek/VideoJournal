package com.example.videojournal.app.di

import com.example.videojournal.data.database.createVideoJournalDatabase
import com.example.videojournal.data.media.AndroidThumbnailGenerator
import com.example.videojournal.data.media.AndroidVideoStorage
import com.example.videojournal.data.repository.OfflineVideoRepository
import com.example.videojournal.domain.media.ThumbnailGenerator
import com.example.videojournal.domain.media.VideoStorage
import com.example.videojournal.domain.repository.VideoRepository
import com.example.videojournal.domain.util.DispatcherProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { createVideoJournalDatabase(androidContext()) }
    single<VideoStorage> {
        AndroidVideoStorage(
            filesDir = androidContext().filesDir,
            cacheDir = androidContext().cacheDir,
            ioDispatcher = get<DispatcherProvider>().io,
        )
    }
    single<ThumbnailGenerator> {
        AndroidThumbnailGenerator(
            filesDir = androidContext().filesDir,
            ioDispatcher = get<DispatcherProvider>().io,
        )
    }
    single<VideoRepository> {
        OfflineVideoRepository(
            database = get(),
            dispatcherProvider = get(),
        )
    }
}
