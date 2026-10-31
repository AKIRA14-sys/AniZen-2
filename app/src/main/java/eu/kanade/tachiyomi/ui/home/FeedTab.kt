package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.IconButton

import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.PanoramaMode
import eu.kanade.presentation.components.PanoramaModeToggle
import tachiyomi.presentation.core.util.collectAsState as collectAsStatePref
import androidx.compose.runtime.getValue

fun feedTab(): Tab = FeedTab

data object FeedTab : Tab {

    override fun isEnabled(): Boolean {
        return true
    }

    override val options: TabOptions
        @Composable
        get() {
            val title = SYMR.strings.feed
            return TabOptions(
                index = 1u,
                title = stringResource(title),
                icon = painterResource(R.drawable.ic_dynamic_feed_24dp),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val globalPanorama by uiPreferences.panoramaCover().collectAsStatePref() as androidx.compose.runtime.State<Boolean>
        val feedMode by uiPreferences.feedPanoramaMode().collectAsStatePref() as androidx.compose.runtime.State<PanoramaMode>
        val effectivePanorama = remember(globalPanorama, feedMode) { feedMode.resolve(globalPanorama) }

        val context = androidx.compose.ui.platform.LocalContext.current
        val feedScreenModel = rememberScreenModel { FeedScreenModel() }
        val coroutineScope = rememberCoroutineScope()

        Scaffold(
            topBar = {
                eu.kanade.presentation.components.AppBar(
                    title = stringResource(SYMR.strings.feed),
                    actions = {
                        // ANZ -->
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val random = feedScreenModel.getRandomAnime()
                                if (random != null) {
                                    navigator.push(eu.kanade.tachiyomi.ui.home.anilist.AniListDetailsScreen(random))
                                } else {
                                    android.widget.Toast.makeText(context, "No anime found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Casino,
                                contentDescription = "Random Anime",
                            )
                        }
                        IconButton(onClick = {
                            navigator.push(eu.kanade.tachiyomi.ui.home.anilist.AniListSearchScreen())
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search Anime",
                            )
                        }
                        // ANZ <--
                        PanoramaModeToggle(
                            panoramaMode = feedMode,
                            globalPanorama = globalPanorama,
                            onPanoramaModeChange = { next ->
                                uiPreferences.feedPanoramaMode().set(next)
                            },
                        )
                        IconButton(onClick = { navigator.push(FeedManageScreen()) }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Edit Feed",
                            )
                        }
                    }
                )
            }
        ) { contentPadding ->
            Content(contentPadding, effectivePanorama, feedScreenModel)
        }
    }

    @Composable
    fun Content(
        contentPadding: PaddingValues,
        usePanorama: Boolean,
        screenModel: FeedScreenModel = rememberScreenModel { FeedScreenModel() },
    ) {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = LocalTabNavigator.current
        val scope = rememberCoroutineScope()
        
        FeedScreen(
            screenModel = screenModel,
            onAnimeClick = { anime, feedId -> 
                screenModel.onAnimeClicked(anime, feedId)
                navigator.push(AnimeScreen(anime.id)) 
            },
            // ANZ -->
            onAniListClick = { media ->
                navigator.push(eu.kanade.tachiyomi.ui.home.anilist.AniListDetailsScreen(media))
            },
            // ANZ <--
            onAddSourceClick = { 
                scope.launch {
                    tabNavigator.current = BrowseTab
                    // BrowseTab is already at index 0 (sourcesTab)
                }
            },
            onSeeAllClick = { item ->
                val type = tachiyomi.domain.source.model.FeedSavedSearch.Type.from(item.feed.type)
                val screen = when (type) {
                    tachiyomi.domain.source.model.FeedSavedSearch.Type.Latest ->
                        eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen(
                            sourceId = item.source.id,
                            listingQuery = tachiyomi.domain.source.interactor.GetRemoteAnime.QUERY_LATEST,
                        )
                    tachiyomi.domain.source.model.FeedSavedSearch.Type.Popular ->
                        eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen(
                            sourceId = item.source.id,
                            listingQuery = tachiyomi.domain.source.interactor.GetRemoteAnime.QUERY_POPULAR,
                        )
                    tachiyomi.domain.source.model.FeedSavedSearch.Type.SavedSearch ->
                        eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen(
                            sourceId = item.source.id,
                            savedSearchId = item.savedSearch?.id,
                        )
                }
                navigator.push(screen)
            },
            contentPadding = contentPadding,
            usePanorama = usePanorama,
        )
    }
}
