package com.example.videojournal.data.database

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun createVideoJournalDatabase(context: Context): VideoJournalDatabase =
    VideoJournalDatabase(
        AndroidSqliteDriver(
            schema = VideoJournalDatabase.Schema,
            context = context,
            name = "video-journal.db",
        ),
    )
