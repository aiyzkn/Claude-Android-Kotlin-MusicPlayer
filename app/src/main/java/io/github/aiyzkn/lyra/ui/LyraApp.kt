package io.github.aiyzkn.lyra.ui

import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.Album
import io.github.aiyzkn.lyra.data.Library
import io.github.aiyzkn.lyra.data.PlaylistRef
import io.github.aiyzkn.lyra.data.Song
import io.github.aiyzkn.lyra.data.audioPermission
import io.github.aiyzkn.lyra.ui.library.AlbumDetail
import io.github.aiyzkn.lyra.ui.library.ArtistDetail
import io.github.aiyzkn.lyra.ui.library.ConfirmDeletePlaylistDialog
import io.github.aiyzkn.lyra.ui.library.GenreDetail
import io.github.aiyzkn.lyra.ui.library.LibraryContent
import io.github.aiyzkn.lyra.ui.library.LibraryTabs
import io.github.aiyzkn.lyra.ui.library.LibraryUiState
import io.github.aiyzkn.lyra.ui.library.LibraryViewModel
import io.github.aiyzkn.lyra.ui.library.PlaylistContent
import io.github.aiyzkn.lyra.ui.library.PlaylistNameDialog
import io.github.aiyzkn.lyra.ui.library.PlaylistPickerDialog
import io.github.aiyzkn.lyra.ui.library.SearchScreen
import io.github.aiyzkn.lyra.ui.library.SongActions
import io.github.aiyzkn.lyra.ui.library.rememberLibraryPagerState
import io.github.aiyzkn.lyra.ui.player.MiniPlayer
import io.github.aiyzkn.lyra.ui.player.NowPlayingScreen
import io.github.aiyzkn.lyra.ui.player.PlayerViewModel
import io.github.aiyzkn.lyra.ui.player.QueueScreen
import kotlinx.coroutines.flow.flowOf

/** ライブラリのタブの上に重ねて開く画面。 */
private sealed interface Destination {
    data class Playlist(val ref: PlaylistRef) : Destination
    data class Artist(val name: String) : Destination
    data class Album(val id: Long) : Destination
    data class Genre(val name: String) : Destination
    data object Search : Destination
}

/** プレイリストの名前のダイアログで、何をするか。 */
private sealed interface NameRequest {
    /** 新しく作る。[songIds] があれば、作ったプレイリストに入れる。 */
    data class Create(val songIds: List<Long>) : NameRequest
    data class Rename(val id: Long, val current: String) : NameRequest
}

/**
 * アプリの最上位の画面。ライブラリのタブ（またはプレイリストの中身）、下端のミニプレーヤー、重ねて開く再生画面。
 * `viewModel()` は、`@AndroidEntryPoint` の Activity の中では Hilt の生成器を使う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyraApp(
    viewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val player by playerViewModel.state.collectAsStateWithLifecycle()
    val queue by playerViewModel.queue.collectAsStateWithLifecycle()
    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    // 画面の行き来は、開いた画面の積み重ねで持つ（開くと積み、戻ると 1 つ外す）。
    // 画面が少なく、引数が曲の一覧の中の値だけなので、Navigation は入れない
    val backStack = remember { mutableStateListOf<Destination>() }
    val top = backStack.lastOrNull()
    val openPlaylist = (top as? Destination.Playlist)?.ref
    var searchQuery by rememberSaveable { mutableStateOf("") }
    fun open(destination: Destination) {
        if (destination == Destination.Search) {
            // 検索は 1 つだけ。開き直したら、前の検索の上に積んだ画面ごと閉じて入力を消す
            val at = backStack.indexOf(Destination.Search)
            if (at >= 0) backStack.subList(at, backStack.size).clear()
            searchQuery = ""
        }
        if (backStack.lastOrNull() != destination) backStack.add(destination)
    }
    fun back() {
        backStack.removeLastOrNull()
    }
    var addingSong by remember { mutableStateOf<Song?>(null) }
    var nameRequest by remember { mutableStateOf<NameRequest?>(null) }
    var deleting by remember { mutableStateOf<PlaylistRef.User?>(null) }
    val libraryPagerState = rememberLibraryPagerState()

    val context = LocalContext.current
    val permission = audioPermission()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.onPermissionGranted()
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionGranted()
        } else {
            launcher.launch(permission)
        }
    }

    // 利用者のプレイリストは、名前の変更・削除を一覧の側から拾う
    val shownPlaylist: PlaylistRef? = when (val p = openPlaylist) {
        is PlaylistRef.User -> user.playlists.firstOrNull { it.id == p.id }?.let { PlaylistRef.User(it.id, it.name) }
        else -> p
    }
    val userPlaylistId = (shownPlaylist as? PlaylistRef.User)?.id
    val userSongIds by remember(userPlaylistId) {
        userPlaylistId?.let(viewModel::playlistSongIds) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val actions = remember(user) {
        SongActions(
            isFavorite = user::isFavorite,
            onToggleFavorite = viewModel::toggleFavorite,
            onAddToPlaylist = { addingSong = it },
            onPlayNext = playerViewModel::playNext,
            onAddToQueue = playerViewModel::addToQueue,
        )
    }

    val loadedLibrary = (state as? LibraryUiState.Loaded)?.library

    BackHandler(enabled = top != null && !nowPlayingOpen) { back() }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            top?.let { destinationTitle(it, shownPlaylist, loadedLibrary) } ?: stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (top != null) {
                            IconButton(onClick = { back() }) {
                                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                            }
                        }
                    },
                    actions = {
                        if (shownPlaylist is PlaylistRef.User) {
                            PlaylistMenu(
                                onRename = { nameRequest = NameRequest.Rename(shownPlaylist.id, shownPlaylist.name) },
                                onDelete = { deleting = shownPlaylist },
                            )
                        } else if (top != Destination.Search && loadedLibrary != null) {
                            IconButton(onClick = { open(Destination.Search) }) {
                                Icon(painterResource(R.drawable.ic_search), contentDescription = stringResource(R.string.action_search))
                            }
                        }
                    },
                )
                // Holo の操作バーの下にあった、アクセントの色の細い線
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        },
        bottomBar = {
            if (player.hasMedia) {
                MiniPlayer(
                    state = player,
                    onOpen = { nowPlayingOpen = true },
                    onPrevious = playerViewModel::previous,
                    onPlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next,
                )
            }
        },
    ) { innerPadding ->
        LibraryContent(
            state = state,
            onRequestPermission = { launcher.launch(permission) },
            onRetry = viewModel::refresh,
            onPlay = playerViewModel::play,
            modifier = Modifier.padding(innerPadding),
        ) { library ->
            val onOpenArtist: (String) -> Unit = { open(Destination.Artist(it)) }
            val onOpenAlbum: (Album) -> Unit = { open(Destination.Album(it.id)) }
            when (val destination = top) {
                null -> LibraryTabs(
                    library = library,
                    user = user,
                    onPlay = playerViewModel::play,
                    actions = actions,
                    onOpenPlaylist = { open(Destination.Playlist(it)) },
                    onNewPlaylist = { nameRequest = NameRequest.Create(emptyList()) },
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onOpenGenre = { open(Destination.Genre(it)) },
                    pagerState = libraryPagerState,
                )
                is Destination.Playlist -> shownPlaylist?.let { playlist ->
                    PlaylistContent(
                        playlist = playlist,
                        library = library,
                        user = user,
                        userSongIds = userSongIds,
                        onPlay = playerViewModel::play,
                        actions = actions,
                        onRemoveAt = { position -> userPlaylistId?.let { viewModel.removeFromPlaylist(it, position) } },
                    )
                }
                is Destination.Artist -> ArtistDetail(
                    name = destination.name,
                    library = library,
                    onPlay = playerViewModel::play,
                    actions = actions,
                    onOpenAlbum = onOpenAlbum,
                )
                is Destination.Album -> AlbumDetail(
                    albumId = destination.id,
                    library = library,
                    onPlay = playerViewModel::play,
                    actions = actions,
                    onOpenArtist = onOpenArtist,
                )
                is Destination.Genre -> GenreDetail(
                    name = destination.name,
                    library = library,
                    onPlay = playerViewModel::play,
                    actions = actions,
                )
                Destination.Search -> SearchScreen(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    library = library,
                    onPlay = playerViewModel::play,
                    actions = actions,
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                )
            }
        }
    }

    addingSong?.let { song ->
        PlaylistPickerDialog(
            playlists = user.playlists,
            onPick = { id ->
                viewModel.addToPlaylist(id, listOf(song.id))
                addingSong = null
            },
            onNew = {
                nameRequest = NameRequest.Create(listOf(song.id))
                addingSong = null
            },
            onDismiss = { addingSong = null },
        )
    }

    nameRequest?.let { request ->
        PlaylistNameDialog(
            title = stringResource(if (request is NameRequest.Rename) R.string.action_rename else R.string.playlist_new),
            initialName = (request as? NameRequest.Rename)?.current.orEmpty(),
            onConfirm = { name ->
                when (request) {
                    is NameRequest.Create -> viewModel.createPlaylist(name, request.songIds)
                    is NameRequest.Rename -> viewModel.renamePlaylist(request.id, name)
                }
                nameRequest = null
            },
            onDismiss = { nameRequest = null },
        )
    }

    deleting?.let { target ->
        ConfirmDeletePlaylistDialog(
            name = target.name,
            onConfirm = {
                viewModel.deletePlaylist(target.id)
                backStack.removeAll { ((it as? Destination.Playlist)?.ref as? PlaylistRef.User)?.id == target.id }
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }

    // 再生画面は一覧の上に重ねる
    AnimatedVisibility(
        visible = nowPlayingOpen && player.hasMedia,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        BackHandler { nowPlayingOpen = false }
        val songId = player.songId
        NowPlayingScreen(
            state = player,
            onPrevious = playerViewModel::previous,
            onPlayPause = playerViewModel::togglePlayPause,
            onNext = playerViewModel::next,
            onSeek = playerViewModel::seekTo,
            onToggleShuffle = playerViewModel::toggleShuffle,
            onCycleRepeat = playerViewModel::cycleRepeat,
            isFavorite = songId != null && user.isFavorite(songId),
            onToggleFavorite = { songId?.let(viewModel::toggleFavorite) },
            onOpenQueue = { queueOpen = true },
        )
    }

    // 再生待ちは再生画面の上に重ねる（再生画面を閉じたら、いっしょに閉じる）
    AnimatedVisibility(
        visible = queueOpen && nowPlayingOpen && player.hasMedia,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        BackHandler { queueOpen = false }
        QueueScreen(
            queue = queue,
            onClose = { queueOpen = false },
            onPlay = playerViewModel::playQueueItem,
            onMove = playerViewModel::moveQueueItem,
            onRemove = playerViewModel::removeQueueItem,
        )
    }
}

@Composable
private fun destinationTitle(destination: Destination, shownPlaylist: PlaylistRef?, library: Library?): String = when (destination) {
    is Destination.Playlist -> playlistTitle(shownPlaylist ?: destination.ref)
    is Destination.Artist -> destination.name
    is Destination.Album -> library?.albums?.firstOrNull { it.id == destination.id }?.title.orEmpty()
    is Destination.Genre -> destination.name
    Destination.Search -> stringResource(R.string.action_search)
}

@Composable
private fun playlistTitle(playlist: PlaylistRef): String = when (playlist) {
    PlaylistRef.Favorites -> stringResource(R.string.playlist_favorites)
    PlaylistRef.RecentlyAdded -> stringResource(R.string.playlist_recently_added)
    is PlaylistRef.User -> playlist.name
}

@Composable
private fun PlaylistMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.action_rename)) }, onClick = {
                open = false
                onRename()
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = {
                open = false
                onDelete()
            })
        }
    }
}
