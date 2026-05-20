package com.example.videojournal.presentation.record

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
internal fun LockOrientationWhenRecording(
    enabled: Boolean,
    activity: Activity?,
) {
    DisposableEffect(enabled, activity) {
        if (enabled && activity != null) {
            val previousOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

            onDispose {
                activity.requestedOrientation = previousOrientation
            }
        } else {
            onDispose { }
        }
    }
}
