package io.github.aiyzkn.lyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.ui.library.formatDuration
import io.github.aiyzkn.lyra.ui.theme.LyraTheme

/** 一覧の下に出す、小さな再生の操作。押すと再生画面を開く。 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onOpen: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column(Modifier.navigationBarsPadding()) {
            // Holo の一覧の下端の区切りに合わせた、アクセントの色の線
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtPlaceholder(Modifier.size(48.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(state.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        state.artist.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ControlButton(R.drawable.ic_skip_previous, R.string.action_previous, onPrevious)
                ControlButton(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (state.isPlaying) R.string.action_pause else R.string.action_play,
                    onPlayPause,
                )
                ControlButton(R.drawable.ic_skip_next, R.string.action_next, onNext, enabled = state.hasNext)
            }
        }
    }
}

/** 再生画面。大きなジャケット・曲名・シークバー・操作。 */
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArtPlaceholder(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                state.title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                listOfNotNull(state.artist, state.album).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))

            // つまみを動かしている間は、つまみの位置を出し、離したときにシークする
            var dragging by remember { mutableStateOf<Float?>(null) }
            Slider(
                value = dragging ?: state.progress,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let(onSeek)
                    dragging = null
                },
                colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val shownPosition = dragging?.let { (state.durationMs * it).toLong() } ?: state.positionMs
                TimeText(formatDuration(shownPosition))
                TimeText(formatDuration(state.durationMs))
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(R.drawable.ic_shuffle, R.string.action_shuffle, onToggleShuffle, active = state.shuffle)
                ControlButton(R.drawable.ic_skip_previous, R.string.action_previous, onPrevious, size = 40)
                ControlButton(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (state.isPlaying) R.string.action_pause else R.string.action_play,
                    onPlayPause,
                    size = 56,
                )
                ControlButton(R.drawable.ic_skip_next, R.string.action_next, onNext, enabled = state.hasNext, size = 40)
                ControlButton(
                    if (state.repeat == RepeatMode.One) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                    R.string.action_repeat,
                    onCycleRepeat,
                    active = state.repeat != RepeatMode.Off,
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: Int,
    label: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean? = null,
    size: Int = 24,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size((size + 24).dp)) {
        Icon(
            painterResource(icon),
            contentDescription = stringResource(label),
            modifier = Modifier.size(size.dp),
            // 切り替えの操作は、入っているときだけアクセントの色にする
            tint = when (active) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.onSurfaceVariant
                null -> LocalContentColor.current
            },
        )
    }
}

@Composable
private fun TimeText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** ジャケットの代わり（画像は後の PR で出す）。 */
@Composable
private fun ArtPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(
            painterResource(R.drawable.ic_play),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxSize(0.4f),
        )
    }
}

@Preview
@Composable
private fun MiniPlayerPreview() {
    LyraTheme {
        MiniPlayer(
            PlayerUiState(title = "Blue Sky", artist = "Artist A", isPlaying = true, hasNext = true),
            {}, {}, {}, {},
        )
    }
}

@Preview(heightDp = 720)
@Composable
private fun NowPlayingPreview() {
    LyraTheme {
        NowPlayingScreen(
            PlayerUiState(
                title = "Blue Sky", artist = "Artist A", album = "Album X",
                isPlaying = false, positionMs = 61_000, durationMs = 215_000, shuffle = true, repeat = RepeatMode.One,
            ),
            {}, {}, {}, {}, {}, {},
        )
    }
}
