package com.example.videojournal.domain.util

import kotlinx.coroutines.CancellationException

inline fun <T> runCatchingNonCancellation(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        Result.failure(throwable)
    }
