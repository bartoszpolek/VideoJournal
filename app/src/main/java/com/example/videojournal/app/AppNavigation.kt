package com.example.videojournal.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.videojournal.presentation.feed.FeedRoute
import com.example.videojournal.presentation.feed.FeedViewModel
import com.example.videojournal.presentation.record.RecordRoute
import com.example.videojournal.presentation.record.RecordViewModel
import com.example.videojournal.presentation.util.findActivity
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
private sealed interface AppDestination : NavKey {
    @Serializable
    data object Feed : AppDestination

    @Serializable
    data object Record : AppDestination
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(AppDestination.Feed)
    val activity = LocalContext.current.findActivity()

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else {
                activity?.finish()
            }
        },
        entryProvider = entryProvider {
            entry<AppDestination.Feed> {
                val viewModel = koinViewModel<FeedViewModel>()

                FeedRoute(
                    viewModel = viewModel,
                    onRecordClick = {
                        if (backStack.lastOrNull() != AppDestination.Record) {
                            backStack.add(AppDestination.Record)
                        }
                    },
                )
            }
            entry<AppDestination.Record> {
                val viewModel = koinViewModel<RecordViewModel>()

                RecordRoute(
                    viewModel = viewModel,
                    onDone = {
                        if (backStack.lastOrNull() == AppDestination.Record) {
                            backStack.removeLastOrNull()
                        }
                    },
                )
            }
        },
    )
}
