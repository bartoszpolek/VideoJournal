package com.example.videojournal.app.di

import com.example.videojournal.domain.usecase.DeleteVideoUseCase
import com.example.videojournal.domain.usecase.ObserveVideosUseCase
import com.example.videojournal.domain.usecase.SaveRecordedVideoUseCase
import com.example.videojournal.domain.util.DefaultDispatcherProvider
import com.example.videojournal.domain.util.DispatcherProvider
import org.koin.dsl.module

val domainModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider }
    single { ObserveVideosUseCase(repository = get()) }
    single {
        SaveRecordedVideoUseCase(
            repository = get(),
            storage = get(),
            thumbnailGenerator = get(),
            dispatcherProvider = get(),
        )
    }
    single {
        DeleteVideoUseCase(
            repository = get(),
            storage = get(),
            dispatcherProvider = get(),
        )
    }
}
