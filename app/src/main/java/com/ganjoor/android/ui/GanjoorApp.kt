package com.ganjoor.android.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

// Routes carry Ganjoor URLs (/hafez/ghazal/sh1) rather than numeric ids: the data set is laid
// out by URL, so no id index lookup is needed. Nav percent-encodes the slashes.
@Serializable
object PoetsRoute

@Serializable
data class CategoryRoute(val url: String)

@Serializable
data class PoemRoute(val url: String)

@Serializable
object BookmarksRoute

@Serializable
object DownloadsRoute

@Composable
fun GanjoorApp() {
    val nav = rememberNavController()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(LocalOpenReadingSettings provides { settingsOpen = true }) {
        NavHost(
            navController = nav,
            startDestination = PoetsRoute,
            // Start/End rather than Left/Right, so going deeper always moves against the reading
            // direction — leftwards here, since the app lays out right-to-left.
            enterTransition = { slideIntoContainer(SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(SlideDirection.Start) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End) },
        ) {
            composable<PoetsRoute> {
                PoetsScreen(
                    onPoet = { nav.navigate(CategoryRoute(it)) },
                    onBookmarks = { nav.navigate(BookmarksRoute) },
                    onDownloads = { nav.navigate(DownloadsRoute) },
                )
            }
            composable<CategoryRoute> { entry ->
                CategoryScreen(
                    fullUrl = entry.toRoute<CategoryRoute>().url,
                    onBack = { nav.navigateUp() },
                    onCategory = { nav.navigate(CategoryRoute(it)) },
                    onPoem = { nav.navigate(PoemRoute(it)) },
                )
            }
            composable<PoemRoute> { entry ->
                PoemScreen(
                    fullUrl = entry.toRoute<PoemRoute>().url,
                    onBack = { nav.navigateUp() },
                    // Reading on through a divan replaces the current poem, so Back returns to
                    // the list instead of unwinding every poem read along the way.
                    onPoem = { url ->
                        nav.navigate(PoemRoute(url)) {
                            popUpTo<PoemRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<BookmarksRoute> {
                BookmarksScreen(
                    onBack = { nav.navigateUp() },
                    onPoem = { nav.navigate(PoemRoute(it)) },
                )
            }
            composable<DownloadsRoute> {
                DownloadsScreen(onBack = { nav.navigateUp() })
            }
        }
    }

    if (settingsOpen) ReadingSettingsSheet(onDismiss = { settingsOpen = false })
}
