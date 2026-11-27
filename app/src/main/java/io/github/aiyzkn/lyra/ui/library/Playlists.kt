package io.github.aiyzkn.lyra.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.PLAYLIST_NAME_MAX
import io.github.aiyzkn.lyra.data.PlaylistRef
import io.github.aiyzkn.lyra.data.PlaylistSummary
import io.github.aiyzkn.lyra.data.Song
import io.github.aiyzkn.lyra.data.normalizePlaylistName

/** 曲の行の「︙」から行う操作。 */
@Stable
class SongActions(
    val isFavorite: (songId: Long) -> Boolean,
    val onToggleFavorite: (songId: Long) -> Unit,
    val onAddToPlaylist: (Song) -> Unit,
) {
    companion object {
        val NONE = SongActions(isFavorite = { false }, onToggleFavorite = {}, onAddToPlaylist = {})
    }
}

/**
 * 曲の一覧。押した曲から、一覧全体を再生待ちにして再生する。
 * プレイリストは同じ曲を何度も持てるので、行の鍵は位置と ID の組にする。
 */
@Composable
internal fun SongList(
    songs: List<Song>,
    onPlay: (songs: List<Song>, index: Int) -> Unit,
    actions: SongActions,
    modifier: Modifier = Modifier,
    onRemoveAt: ((index: Int) -> Unit)? = null,
    empty: String? = null,
) {
    if (songs.isEmpty()) return Empty(modifier, empty)
    LazyColumn(modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { index, song -> "$index:${song.id}" }) { index, song ->
            SongRow(
                song = song,
                onClick = { onPlay(songs, index) },
                actions = actions,
                onRemove = onRemoveAt?.let { remove -> { remove(index) } },
            )
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit, actions: SongActions, onRemove: (() -> Unit)?) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text("${song.artist} · ${formatDuration(song.durationMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    val favorite = actions.isFavorite(song.id)
                    DropdownMenuItem(
                        text = { Text(stringResource(if (favorite) R.string.action_unfavorite else R.string.action_favorite)) },
                        onClick = {
                            menuOpen = false
                            actions.onToggleFavorite(song.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_playlist)) },
                        onClick = {
                            menuOpen = false
                            actions.onAddToPlaylist(song)
                        },
                    )
                    if (onRemove != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            },
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

/** プレイリストのタブ。新しいプレイリスト → 自動のもの（お気に入り・最近追加）→ 利用者が作ったもの。 */
@Composable
internal fun PlaylistsTab(
    favoritesCount: Int,
    recentlyAddedCount: Int,
    playlists: List<PlaylistSummary>,
    onOpen: (PlaylistRef) -> Unit,
    onNew: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "new") {
            ListItem(
                modifier = Modifier.clickable(onClick = onNew),
                leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.playlist_new)) },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.background,
                    headlineColor = MaterialTheme.colorScheme.primary,
                    leadingIconColor = MaterialTheme.colorScheme.primary,
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
        item(key = "favorites") {
            PlaylistRow(stringResource(R.string.playlist_favorites), favoritesCount) { onOpen(PlaylistRef.Favorites) }
        }
        item(key = "recentlyAdded") {
            PlaylistRow(stringResource(R.string.playlist_recently_added), recentlyAddedCount) {
                onOpen(PlaylistRef.RecentlyAdded)
            }
        }
        items(playlists, key = { "user:${it.id}" }) { playlist ->
            PlaylistRow(playlist.name, playlist.songCount) { onOpen(PlaylistRef.User(playlist.id, playlist.name)) }
        }
    }
}

@Composable
private fun PlaylistRow(name: String, songCount: Int, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(pluralStringResource(R.plurals.song_count, songCount, songCount)) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

/** プレイリストの名前を入れるダイアログ（新規・名前の変更）。 */
@Composable
fun PlaylistNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // 名前の変更では、カーソルを末尾に置いて開く
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(initialName.length)))
    }
    val name = field.text
    val valid = normalizePlaylistName(name) != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { if (it.text.length <= PLAYLIST_NAME_MAX) field = it },
                singleLine = true,
                label = { Text(stringResource(R.string.playlist_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = valid) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** 曲を足すプレイリストを選ぶダイアログ。いちばん上は「新しいプレイリスト」。 */
@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistSummary>,
    onPick: (playlistId: Long) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_to_playlist)) },
        text = {
            // 行の地は、ダイアログの地に合わせて透明にする
            val colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            LazyColumn {
                item {
                    ListItem(
                        modifier = Modifier.clickable(onClick = onNew),
                        headlineContent = { Text(stringResource(R.string.playlist_new)) },
                        leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                        colors = colors,
                    )
                }
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        modifier = Modifier.clickable { onPick(playlist.id) },
                        headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = colors,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ConfirmDeletePlaylistDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_delete_title)) },
        text = { Text(stringResource(R.string.playlist_delete_message, name)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun Empty(modifier: Modifier = Modifier, text: String? = null) {
    Box(modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(
            text ?: stringResource(R.string.library_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
