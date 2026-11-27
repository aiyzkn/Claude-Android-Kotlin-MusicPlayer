package io.github.aiyzkn.lyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.dragTarget
import io.github.aiyzkn.lyra.ui.theme.LyraTheme

/**
 * 再生待ちの画面。行を押すとその曲へ飛ぶ。左の「＝」をドラッグして並べ替え、右の「×」で外す。
 * 並べ替えは、離したときに 1 回だけ再生待ちに渡す（動かしている間は、見た目だけをずらす）。
 */
@Composable
fun QueueScreen(
    queue: QueueUiState,
    onClose: () -> Unit,
    onPlay: (index: Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(stringResource(R.string.queue_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        pluralStringResource(R.plurals.song_count, queue.items.size, queue.items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Holo の操作バーの下にあった、アクセントの色の細い線
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            if (queue.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.queue_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                QueueList(queue, onPlay, onMove, onRemove)
            }
        }
    }
}

@Composable
private fun QueueList(
    queue: QueueUiState,
    onPlay: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    // 開いたときは、いま鳴っている曲が見えるところから
    LaunchedEffect(Unit) {
        if (queue.currentIndex > 0) listState.scrollToItem(queue.currentIndex)
    }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var itemHeight by remember { mutableFloatStateOf(0f) }
    val size = queue.items.size
    val currentOnMove by rememberUpdatedState(onMove)

    val from = draggingIndex
    val target = from?.let { dragTarget(it, dragOffset, itemHeight, size) }

    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        itemsIndexed(queue.items, key = { index, entry -> "$index:${entry.songId}" }) { index, entry ->
            // 動かしている行の行き先に合わせて、間の行を 1 行ぶんずらす
            val shift = when {
                from == null || target == null || index == from -> 0f
                index in (from + 1)..target -> -itemHeight
                index in target until from -> itemHeight
                else -> 0f
            }
            val dragging = index == from
            Column(
                Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else shift }
                    .onSizeChanged { itemHeight = it.height.toFloat() },
            ) {
                QueueRow(
                    entry = entry,
                    current = index == queue.currentIndex,
                    dragging = dragging,
                    onClick = { onPlay(index) },
                    onRemove = { onRemove(index) },
                    handleModifier = Modifier.pointerInput(index, size) {
                        detectDragGestures(
                            onDragStart = {
                                draggingIndex = index
                                dragOffset = 0f
                            },
                            onDragEnd = {
                                val to = dragTarget(index, dragOffset, itemHeight, size)
                                draggingIndex = null
                                dragOffset = 0f
                                if (to != index) currentOnMove(index, to)
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                            },
                        )
                    },
                )
            }
        }
    }
}

// handleModifier は行全体ではなく、つまみ（＝）だけに付けるドラッグの受け口なので、modifier の名前にしない
@Suppress("ModifierParameter")
@Composable
private fun QueueRow(
    entry: QueueEntry,
    current: Boolean,
    dragging: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    handleModifier: Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.action_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = handleModifier.padding(8.dp),
            )
        },
        headlineContent = {
            Text(
                entry.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (current) accent else MaterialTheme.colorScheme.onBackground,
            )
        },
        supportingContent = {
            Text(
                if (current) "${stringResource(R.string.now_playing)} · ${entry.artist.orEmpty()}" else entry.artist.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_remove_from_queue))
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (dragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
        ),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Preview(heightDp = 480)
@Composable
private fun QueueScreenPreview() {
    LyraTheme {
        QueueScreen(
            QueueUiState(
                items = listOf(
                    QueueEntry(1, "Blue Sky", "Artist A"),
                    QueueEntry(2, "Green Field", "Artist B"),
                    QueueEntry(1, "Blue Sky", "Artist A"),
                ),
                currentIndex = 1,
            ),
            {}, {}, { _, _ -> }, {},
        )
    }
}
