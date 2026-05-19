package com.example.videojournal.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.videojournal.presentation.design.VideoJournalTheme

sealed interface AppRoute {
    val route: String
}

data object FeedRoute : AppRoute {
    override val route: String = "feed"
}

data object RecordRoute : AppRoute {
    override val route: String = "record"
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Video Journal",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppNavigationPreview() {
    VideoJournalTheme {
        AppNavigation()
    }
}
