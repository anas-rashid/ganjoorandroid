package com.ganjoor.android.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    // Above the NavHost, so the large-screen columns survive moving between destinations.
    val browser = remember { BrowserState() }

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
        // Tablets and unfolded foldables browse in columns (ColumnBrowser); phones, and a
        // foldable when folded, keep one screen at a time. Same routes either way, so unfolding
        // in the middle of a poem keeps the poem.
        BoxWithConstraints(Modifier.fillMaxSize()) {
        // Read through state everywhere below: the transition and destination lambdas belong to
        // the nav graph, which outlives this composition, and folding or unfolding must reach them.
        val wide by rememberUpdatedState(maxWidth >= 600.dp)
        // Up to three list columns beside the poets; below this, only the newest one.
        val expanded by rememberUpdatedState(maxWidth >= 840.dp)
        // On a large screen reading settings open in a panel on the left, where the dictionary
        // opens, so the page stays in view and shows each change as it is made.
        Row(Modifier.fillMaxSize()) {
        // The columns fold away while the settings panel is open, so the page doesn't end up
        // squeezed between them and the panel.
        Box(Modifier.weight(1f)) {
        CompositionLocalProvider(LocalSidePanelOpen provides (wide && settingsOpen)) {
        NavHost(
            navController = nav,
            startDestination = PoetsRoute,
            // Start/End rather than Left/Right, so going deeper always moves against the reading
            // direction — leftwards here, since the app lays out right-to-left. In columns the
            // page changes in place: sliding the whole screen would drag the columns with it.
            enterTransition = {
                if (wide) EnterTransition.None else slideIntoContainer(SlideDirection.Start)
            },
            exitTransition = {
                if (wide) ExitTransition.None else slideOutOfContainer(SlideDirection.Start)
            },
            popEnterTransition = {
                if (wide) EnterTransition.None else slideIntoContainer(SlideDirection.End)
            },
            popExitTransition = {
                if (wide) ExitTransition.None else slideOutOfContainer(SlideDirection.End)
            },
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
                if (wide) {
                    ColumnBrowser(
                        state = browser,
                        url = url,
                        isPoem = false,
                        expanded = expanded,
                        onPoet = { nav.open(CategoryRoute(it)) },
                        onCategory = { nav.open(CategoryRoute(it)) },
                        onPoem = { nav.open(PoemRoute(it)) },
                    ) { toggle ->
                        CategoryOverview(
                            state = browser,
                            fullUrl = url,
                            toggle = toggle,
                            onUp = { nav.goUp(url) },
                            onHome = { nav.goHome() },
                            onCategory = { nav.open(CategoryRoute(it)) },
                            onPoem = { nav.open(PoemRoute(it)) },
                        )
                    }
                } else {
                    CategoryScreen(
                        fullUrl = url,
                        onUp = { nav.goUp(url) },
                        onHome = { nav.goHome() },
                        onCategory = { nav.open(CategoryRoute(it)) },
                        onPoem = { nav.open(PoemRoute(it)) },
                    )
                }
            }
            composable<PoemRoute> { entry ->
                val route = entry.toRoute<PoemRoute>()
                val poem: @Composable (Boolean, (@Composable () -> Unit)?) -> Unit = { wideText, toggle ->
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
                        wide = wideText,
                        navigationToggle = toggle,
                    )
                }
                if (wide) {
                    ColumnBrowser(
                        state = browser,
                        url = route.url,
                        isPoem = true,
                        expanded = expanded,
                        onPoet = { nav.open(CategoryRoute(it)) },
                        onCategory = { nav.open(CategoryRoute(it)) },
                        onPoem = { nav.open(PoemRoute(it)) },
                    ) { toggle -> poem(true, toggle) }
                } else {
                    poem(false, null)
                }
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
        }
        }
        if (wide) {
            AnimatedVisibility(
                visible = settingsOpen,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                ReadingSettingsPanel(onDismiss = { settingsOpen = false })
            }
        }
        }

        // Inside the provider: the sheet reads LocalOpenAbout, so it has to be in scope.
        if (settingsOpen && !wide) ReadingSettingsSheet(onDismiss = { settingsOpen = false })
        }
    }
}
