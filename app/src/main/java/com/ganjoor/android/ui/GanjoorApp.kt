package com.ganjoor.android.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ganjoor.android.data.parentUrl
import kotlinx.serialization.Serializable

// Routes carry Ganjoor URLs (/hafez/ghazal/sh1) rather than numeric ids: the data set is laid
// out by URL, so no id index lookup is needed. Nav percent-encodes the slashes.
@Serializable
object PoetsRoute

@Serializable
data class CategoryRoute(val url: String)

/**
 * [fromBookmarks] marks a poem opened from the saved list. Browsing is a tree and Back climbs
 * it, but a saved poem was reached from a list, not a shelf — so Back returns to that list.
 */
@Serializable
data class PoemRoute(val url: String, val fromBookmarks: Boolean = false)

@Serializable
data class SearchRoute(val term: String = "")

@Serializable
object BookmarksRoute

@Serializable
object DownloadsRoute

@Serializable
object AboutRoute

@Serializable
object AssistantRoute

/**
 * Opens a page with the poet list as the only thing beneath it.
 *
 * Browsing is a tree, so Back should climb it — poem to section to book to poet to home —
 * rather than retrace however you arrived. Each screen works out its own parent from its URL,
 * so the trail is identical whether you drilled down, followed a bookmark, tapped a breadcrumb
 * or read on from the previous poem. Keeping the stack flat is what stops Back from walking
 * you forward again into the page you just came from.
 */
private fun NavController.open(route: Any) = navigate(route) {
    popUpTo<PoetsRoute> { inclusive = false }
    launchSingleTop = true
}

private fun NavController.goHome() = open(PoetsRoute)

/** One level up the tree; from a poet's root that means home. */
private fun NavController.goUp(fromUrl: String) {
    val parent = parentUrl(fromUrl)
    if (parent == null) goHome() else open(CategoryRoute(parent))
}

@Composable
fun GanjoorApp() {
    val nav = rememberNavController()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalOpenReadingSettings provides { settingsOpen = true },
        LocalOpenAbout provides {
            settingsOpen = false
            nav.navigate(AboutRoute)
        },
        LocalOpenAssistant provides {
            settingsOpen = false
            nav.navigate(AssistantRoute)
        },
    ) {
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
                    onPoet = { nav.open(CategoryRoute(it)) },
                    onSearchPoems = { nav.navigate(SearchRoute(it)) },
                    onBookmarks = { nav.navigate(BookmarksRoute) },
                    onDownloads = { nav.navigate(DownloadsRoute) },
                )
            }
            composable<CategoryRoute> { entry ->
                val url = entry.toRoute<CategoryRoute>().url
                CategoryScreen(
                    fullUrl = url,
                    onUp = { nav.goUp(url) },
                    onHome = { nav.goHome() },
                    onCategory = { nav.open(CategoryRoute(it)) },
                    onPoem = { nav.open(PoemRoute(it)) },
                )
            }
            composable<PoemRoute> { entry ->
                val route = entry.toRoute<PoemRoute>()
                PoemScreen(
                    fullUrl = route.url,
                    // Reading on through a divan keeps the origin, so Back still lands where
                    // you started rather than in whichever section you drifted into.
                    onUp = { if (route.fromBookmarks) nav.navigateUp() else nav.goUp(route.url) },
                    onHome = { nav.goHome() },
                    onPoem = { url ->
                        if (route.fromBookmarks) {
                            nav.navigate(PoemRoute(url, fromBookmarks = true)) {
                                popUpTo<PoemRoute> { inclusive = true }
                            }
                        } else {
                            nav.open(PoemRoute(url))
                        }
                    },
                    // Tapping a breadcrumb leaves the saved list behind and starts browsing.
                    onCategory = { nav.open(CategoryRoute(it)) },
                )
            }
            composable<SearchRoute> { entry ->
                SearchScreen(
                    initialTerm = entry.toRoute<SearchRoute>().term,
                    onUp = { nav.navigateUp() },
                    onHome = { nav.goHome() },
                    // Searching is a list, like the saved poems, so Back returns to the results.
                    onPoem = { nav.navigate(PoemRoute(it, fromBookmarks = true)) },
                )
            }
            composable<BookmarksRoute> {
                BookmarksScreen(
                    onUp = { nav.goHome() },
                    // Plain navigate, not open(): this keeps the saved list on the stack.
                    onPoem = { nav.navigate(PoemRoute(it, fromBookmarks = true)) },
                )
            }
            composable<DownloadsRoute> {
                DownloadsScreen(onUp = { nav.goHome() })
            }
            composable<AssistantRoute> {
                AssistantScreen(onUp = { nav.navigateUp() }, onHome = { nav.goHome() })
            }
            composable<AboutRoute> {
                AboutScreen(onUp = { nav.navigateUp() }, onHome = { nav.goHome() })
            }
        }

        // Inside the provider: the sheet reads LocalOpenAbout, so it has to be in scope.
        if (settingsOpen) ReadingSettingsSheet(onDismiss = { settingsOpen = false })
    }
}
