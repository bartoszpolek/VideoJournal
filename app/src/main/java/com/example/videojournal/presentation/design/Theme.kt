package com.example.videojournal.presentation.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val JournalColorScheme = lightColorScheme(
    primary = JournalPrimary,
    onPrimary = JournalOnPrimary,
    primaryContainer = JournalPrimaryContainer,
    onPrimaryContainer = JournalOnPrimaryContainer,
    secondary = JournalSecondary,
    background = JournalBackground,
    surface = JournalSurface,
    onSurface = JournalOnSurface,
    onSurfaceVariant = JournalOnSurfaceVariant,
    outline = JournalOutline,
)

@Composable
fun VideoJournalTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JournalColorScheme,
        typography = Typography,
        content = content,
    )
}
