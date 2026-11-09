package dev.anon.novatv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import dev.anon.novatv.core.player.PlaybackController
import dev.anon.novatv.feature.live.LiveScreen
import dev.anon.novatv.feature.live.LiveViewModel
import dev.anon.novatv.feature.player.ChannelStreamResolver
import dev.anon.novatv.feature.player.PlayerRoutes
import dev.anon.novatv.feature.player.PlayerScreen
import dev.anon.novatv.feature.player.PlayerViewModel
import dev.anon.novatv.feature.playlists.PlaylistFormScreen
import dev.anon.novatv.feature.playlists.PlaylistFormViewModel
import dev.anon.novatv.feature.playlists.PlaylistsRoutes
import dev.anon.novatv.feature.playlists.PlaylistsScreen
import dev.anon.novatv.feature.playlists.PlaylistsViewModel

object Routes {
    const val LIVE = "live"
}

@Composable
fun NovaNavHost(graph: NovaGraph) {
    val navController = rememberNavController()

    // Açılış hedefi: son izlenen kanal varsa oynatıcı, yoksa canlı TV.
    // Navigasyon yığınına sonradan eklemek yerine başlangıç hedefi seçilir;
    // böylece açılışta yaşam döngüsü yarışı oluşmaz ve Back temiz çalışır.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDestination = runCatching { PlayerRoutes.lastWatched(graph.channels) }
            .getOrNull()
            ?: Routes.LIVE
    }

    val start = startDestination ?: return

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LIVE) {
            val viewModel: LiveViewModel = viewModel(
                factory = LiveViewModel.factory(graph.channels, graph.preferences),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val channels = viewModel.channels.collectAsLazyPagingItems()
            LiveScreen(
                state = state,
                channels = channels,
                onSelectGroup = viewModel::selectGroup,
                onQueryChange = viewModel::setQuery,
                onToggleFavorites = viewModel::toggleFavoritesOnly,
                onToggleGrid = viewModel::toggleGrid,
                onChannelClick = { channel -> navController.navigate(PlayerRoutes.of(channel.id)) },
                onToggleFavorite = viewModel::toggleFavorite,
                onHideChannel = viewModel::hideChannel,
                onHideGroup = viewModel::hideGroup,
                onSetNumber = viewModel::setNumber,
                onChannelFocused = viewModel::onChannelFocused,
                onOpenPlaylists = { navController.navigate(PlaylistsRoutes.LIST) },
            )
        }

        composable(PlaylistsRoutes.LIST) {
            val viewModel: PlaylistsViewModel = viewModel(
                factory = PlaylistsViewModel.factory(
                    graph.playlists,
                    graph.syncEngine,
                    graph.syncScheduler,
                ),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlaylistsScreen(
                state = state,
                onAdd = { navController.navigate(PlaylistsRoutes.form()) },
                onEdit = { playlist -> navController.navigate(PlaylistsRoutes.form(playlist.id)) },
                onRefresh = viewModel::refresh,
                onToggleActive = { playlist -> viewModel.setActive(playlist, !playlist.isActive) },
                onMoveUp = { playlist -> viewModel.move(playlist, -1) },
                onMoveDown = { playlist -> viewModel.move(playlist, 1) },
                onDelete = viewModel::delete,
                onBackToLive = { navController.popBackStack(Routes.LIVE, false) },
            )
        }

        composable(
            route = PlaylistsRoutes.FORM,
            arguments = listOf(
                navArgument("playlistId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { entry ->
            val playlistId = entry.arguments?.getLong("playlistId")?.takeIf { it > 0 }
            val viewModel: PlaylistFormViewModel = viewModel(
                factory = PlaylistFormViewModel.factory(graph.playlists, playlistId, graph.registry),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlaylistFormScreen(
                state = state,
                onName = viewModel::setName,
                onType = viewModel::setType,
                onUrl = viewModel::setUrl,
                onUsername = viewModel::setUsername,
                onPassword = viewModel::setPassword,
                onEpgUrl = viewModel::setEpgUrl,
                onUserAgent = viewModel::setUserAgent,
                onSyncInterval = viewModel::setSyncInterval,
                onTest = viewModel::test,
                onSave = viewModel::save,
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    // Kaynak eklenince kanallar hemen gelsin; senkron ekran
                    // kapansa da uygulama kapsamında sürer.
                    graph.syncEngine.requestSync(id)
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = PlayerRoutes.PATTERN,
            arguments = listOf(
                navArgument(PlayerRoutes.ARG_CHANNEL_ID) { type = NavType.LongType },
            ),
        ) { entry ->
            val channelId = entry.arguments?.getLong(PlayerRoutes.ARG_CHANNEL_ID) ?: -1L
            val context = LocalContext.current
            val controller = remember { PlaybackController(context) }
            DisposableEffect(controller) {
                onDispose { controller.release() }
            }
            val viewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.factory(
                    playbackController = controller,
                    repository = graph.channels,
                    resolver = ChannelStreamResolver { graph.streamResolver.resolve(it) },
                    preferenceStore = graph.preferences,
                ),
            )
            LaunchedEffect(channelId) { viewModel.open(channelId) }
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlayerScreen(
                state = state,
                controller = controller,
                onToggleOsd = viewModel::toggleOsd,
                onHideOsd = viewModel::hideOsd,
                onRetry = viewModel::retry,
                onBackToChannels = {
                    // Oynatıcı açılış hedefiyse geri yığını boştur; canlı TV'ye geç.
                    if (!navController.popBackStack()) navController.navigate(Routes.LIVE)
                },
                onSeekBack = { viewModel.seekBack() },
                onCycleAspect = viewModel::cycleAspect,
                onCycleBuffer = viewModel::cycleBuffer,
                onSelectAudio = viewModel::selectAudio,
                onSelectSubtitle = viewModel::selectSubtitle,
                onMenuOpenChange = viewModel::keepOsdVisible,
            )
        }
    }
}
