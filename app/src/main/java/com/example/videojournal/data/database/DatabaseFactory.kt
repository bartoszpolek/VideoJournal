package com.example.videojournal.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class DatabaseFactory(
    private val context: Context,
) {
    fun create(): VideoJournalDatabase =
        VideoJournalDatabase(createDriver())

    private fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = VideoJournalDatabase.Schema,
            context = context,
            name = DATABASE_NAME,
        )

    private companion object {
        const val DATABASE_NAME = "video-journal.db"
    }
}
