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
import io.github.aiyzkn.lyra.data.PlaylistRef
import io.github.aiyzkn.lyra.data.Song
import io.github.aiyzkn.lyra.data.audioPermission
import io.github.aiyzkn.lyra.ui.library.ConfirmDeletePlaylistDialog
import io.github.aiyzkn.lyra.ui.library.LibraryContent
import io.github.aiyzkn.lyra.ui.library.LibraryTabs
import io.github.aiyzkn.lyra.ui.library.LibraryViewModel
import io.github.aiyzkn.lyra.ui.library.PlaylistContent
import io.github.aiyzkn.lyra.ui.library.PlaylistNameDialog
import io.github.aiyzkn.lyra.ui.library.PlaylistPickerDialog
import io.github.aiyzkn.lyra.ui.library.SongActions
import io.github.aiyzkn.lyra.ui.library.rememberLibraryPagerState
import io.github.aiyzkn.lyra.ui.player.MiniPlayer
import io.github.aiyzkn.lyra.ui.player.NowPlayingScreen
import io.github.aiyzkn.lyra.ui.player.PlayerViewModel
import kotlinx.coroutines.flow.flowOf

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
    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    // 画面の行き来は、詳細の画面を足す PR で Navigation にする。それまでは、開いているプレイリストを状態で持つ
    var openPlaylist by remember { mutableStateOf<PlaylistRef?>(null) }
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
        )
    }

    BackHandler(enabled = shownPlaylist != null && !nowPlayingOpen) { openPlaylist = null }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            shownPlaylist?.let { playlistTitle(it) } ?: stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (shownPlaylist != null) {
                            IconButton(onClick = { openPlaylist = null }) {
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
            val playlist = shownPlaylist
            if (playlist == null) {
                LibraryTabs(
                    library = library,
                    user = user,
                    onPlay = playerViewModel::play,
                    actions = actions,
                    onOpenPlaylist = { openPlaylist = it },
                    onNewPlaylist = { nameRequest = NameRequest.Create(emptyList()) },
                    pagerState = libraryPagerState,
                )
            } else {
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
                if ((openPlaylist as? PlaylistRef.User)?.id == target.id) openPlaylist = null
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
        )
    }
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
