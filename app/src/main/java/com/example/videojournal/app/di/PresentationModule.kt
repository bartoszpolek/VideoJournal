package com.example.videojournal.app.di

import com.example.videojournal.presentation.feed.FeedViewModel
import com.example.videojournal.presentation.record.RecordViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel {
        FeedViewModel(
            observeVideosUseCase = get(),
            deleteVideoUseCase = get(),
        )
    }
    viewModel {
        RecordViewModel(
            saveRecordedVideoUseCase = get(),
            videoStorage = get(),
        )
    }
}
