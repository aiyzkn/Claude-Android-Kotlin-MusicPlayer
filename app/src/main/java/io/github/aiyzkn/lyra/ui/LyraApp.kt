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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.audioPermission
import io.github.aiyzkn.lyra.ui.library.LibraryContent
import io.github.aiyzkn.lyra.ui.library.LibraryViewModel
import io.github.aiyzkn.lyra.ui.player.MiniPlayer
import io.github.aiyzkn.lyra.ui.player.NowPlayingScreen
import io.github.aiyzkn.lyra.ui.player.PlayerViewModel

/**
 * アプリの最上位の画面。ライブラリのタブ、下端のミニプレーヤー、重ねて開く再生画面。
 * `viewModel()` は、`@AndroidEntryPoint` の Activity の中では Hilt の生成器を使う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyraApp(
    viewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.state.collectAsStateWithLifecycle()
    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.app_name)) })
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
        )
    }

    // 再生画面は一覧の上に重ねる（画面の行き来は、詳細の画面を足す PR で Navigation にする）
    AnimatedVisibility(
        visible = nowPlayingOpen && player.hasMedia,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        BackHandler { nowPlayingOpen = false }
        NowPlayingScreen(
            state = player,
            onPrevious = playerViewModel::previous,
            onPlayPause = playerViewModel::togglePlayPause,
            onNext = playerViewModel::next,
            onSeek = playerViewModel::seekTo,
            onToggleShuffle = playerViewModel::toggleShuffle,
            onCycleRepeat = playerViewModel::cycleRepeat,
        )
    }
}
