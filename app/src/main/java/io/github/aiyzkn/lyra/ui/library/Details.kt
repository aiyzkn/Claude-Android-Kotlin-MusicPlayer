package io.github.aiyzkn.lyra.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.Album
import io.github.aiyzkn.lyra.data.Library
import io.github.aiyzkn.lyra.data.Song
import io.github.aiyzkn.lyra.data.albumsOfArtist
import io.github.aiyzkn.lyra.data.songsOfAlbum
import io.github.aiyzkn.lyra.data.songsOfArtist
import io.github.aiyzkn.lyra.data.songsOfGenre
import io.github.aiyzkn.lyra.ui.theme.LyraTheme

/** アーティストの詳細のタブ。 */
private enum class ArtistTab(val titleRes: Int) {
    Songs(R.string.tab_songs),
    Albums(R.string.tab_albums),
}

/**
 * アーティストの詳細。上部の大きな見出し（スクロールで半分の速さで動く視差）と、曲・アルバムのタブ。
 * 端末の中の画像だけを使う決まり（Q3）なので、見出しはアクセントの色の地に名前を出す。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistDetail(
    name: String,
    library: Library,
    onPlay: (songs: List<Song>, index: Int) -> Unit,
    actions: SongActions,
    onOpenAlbum: (Album) -> Unit,
) {
    val songs = remember(library, name) { library.songsOfArtist(name) }
    val albums = remember(library, name) { library.albumsOfArtist(name) }
    if (songs.isEmpty()) return Empty()
    var tab by rememberSaveable(name) { mutableIntStateOf(ArtistTab.Songs.ordinal) }
    val listState = rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        item(key = "header") {
            ParallaxHeader(
                title = name,
                subtitle = pluralStringResource(R.plurals.album_count, albums.size, albums.size) +
                    " · " + pluralStringResource(R.plurals.song_count, songs.size, songs.size),
                listState = listState,
            )
        }
        stickyHeader(key = "tabs") {
            PrimaryTabRow(selectedTabIndex = tab) {
                ArtistTab.entries.forEach { entry ->
                    Tab(
                        selected = tab == entry.ordinal,
                        onClick = { tab = entry.ordinal },
                        text = { Text(stringResource(entry.titleRes).uppercase()) },
                    )
                }
            }
        }
        when (ArtistTab.entries[tab]) {
            ArtistTab.Songs -> itemsIndexed(songs, key = { _, song -> "song:${song.id}" }) { index, song ->
                SongRow(song = song, onClick = { onPlay(songs, index) }, actions = actions, onRemove = null)
            }
            ArtistTab.Albums -> items(albums, key = { "album:${it.id}" }) { album ->
                DetailRow(
                    headline = album.title,
                    supporting = album.artist + " · " +
                        pluralStringResource(R.plurals.song_count, album.songCount, album.songCount),
                    onClick = { onOpenAlbum(album) },
                )
            }
        }
    }
}

/** アルバムの詳細。見出しにアルバム名・アーティスト・曲数・合計の時間、下にトラック番号の順の曲。 */
@Composable
fun AlbumDetail(
    albumId: Long,
    library: Library,
    onPlay: (songs: List<Song>, index: Int) -> Unit,
    actions: SongActions,
    onOpenArtist: (String) -> Unit,
) {
    val album = remember(library, albumId) { library.albums.firstOrNull { it.id == albumId } }
    val songs = remember(library, albumId) { library.songsOfAlbum(albumId) }
    if (album == null || songs.isEmpty()) return Empty()
    val listState = rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize(), state = listState) {
        item(key = "header") {
            ParallaxHeader(
                title = album.title,
                subtitle = pluralStringResource(R.plurals.song_count, songs.size, songs.size) +
                    " · " + formatDuration(songs.sumOf { it.durationMs }),
                listState = listState,
            )
        }
        item(key = "artist") {
            DetailRow(
                headline = album.artist,
                supporting = stringResource(R.string.detail_open_artist),
                onClick = { onOpenArtist(album.artist) },
            )
        }
        itemsIndexed(songs, key = { _, song -> "song:${song.id}" }) { index, song ->
            SongRow(song = song, onClick = { onPlay(songs, index) }, actions = actions, onRemove = null)
        }
    }
}

/** ジャンルの曲。 */
@Composable
fun GenreDetail(
    name: String,
    library: Library,
    onPlay: (songs: List<Song>, index: Int) -> Unit,
    actions: SongActions,
) {
    val songs = remember(library, name) { library.songsOfGenre(name) }
    SongList(songs, onPlay, actions)
}

/** 一覧の先頭に置く大きな見出し。スクロールすると、一覧の半分の速さで上へずれる（視差）。 */
@Composable
private fun ParallaxHeader(title: String, subtitle: String, listState: LazyListState) {
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .graphicsLayer {
                // 見出しが一覧の先頭にある間だけ動かす（画面の外に出たら描かれない）
                translationY = if (listState.firstVisibleItemIndex == 0) {
                    listState.firstVisibleItemScrollOffset * PARALLAX
                } else {
                    0f
                }
            }
            .background(Brush.verticalGradient(listOf(primary.copy(alpha = 0.45f), background))),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun DetailRow(headline: String, supporting: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(headline, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(supporting, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

private val HEADER_HEIGHT = 200.dp
private const val PARALLAX = 0.5f

@Preview
@Composable
private fun ArtistDetailPreview() {
    val songs = listOf(
        Song(1, "Blue", "Artist A", "Album X", 10, "Pop", 215_000, 1, null),
        Song(2, "Green", "Artist A", "Album Y", 11, "Rock", 3_725_000, 1, null),
    )
    LyraTheme { ArtistDetail("Artist A", Library.from(songs), { _, _ -> }, SongActions.NONE, {}) }
}
