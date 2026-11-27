package io.github.aiyzkn.lyra.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.Album
import io.github.aiyzkn.lyra.data.Artist
import io.github.aiyzkn.lyra.data.Genre
import io.github.aiyzkn.lyra.data.Library
import io.github.aiyzkn.lyra.data.Song
import io.github.aiyzkn.lyra.ui.theme.LyraTheme
import kotlinx.coroutines.launch

/** ライブラリのタブ。Apollo の並び（アーティスト → アルバム → 曲 → ジャンル）に合わせる。 */
enum class LibraryTab(val titleRes: Int) {
    Artists(R.string.tab_artists),
    Albums(R.string.tab_albums),
    Songs(R.string.tab_songs),
    Genres(R.string.tab_genres),
}

@Composable
fun LibraryContent(
    state: LibraryUiState,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        LibraryUiState.NeedsPermission -> Message(
            text = stringResource(R.string.permission_rationale),
            action = stringResource(R.string.permission_grant),
            onAction = onRequestPermission,
            modifier = modifier,
        )
        LibraryUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is LibraryUiState.Failed -> Message(
            text = stringResource(R.string.library_failed, state.message),
            action = stringResource(R.string.retry),
            onAction = onRetry,
            modifier = modifier,
        )
        is LibraryUiState.Loaded -> LibraryTabs(state.library, onPlay, modifier)
    }
}

@Composable
fun LibraryTabs(
    library: Library,
    onPlay: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = LibraryTab.entries
    val pagerState = rememberPagerState(initialPage = LibraryTab.Songs.ordinal) { tabs.size }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 0.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stringResource(tab.titleRes).uppercase()) },
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (tabs[page]) {
                LibraryTab.Artists -> ArtistList(library.artists)
                LibraryTab.Albums -> AlbumGrid(library.albums)
                LibraryTab.Songs -> SongList(library.songs, onPlay)
                LibraryTab.Genres -> GenreList(library.genres)
            }
        }
    }
}

@Composable
private fun SongList(songs: List<Song>, onPlay: (songs: List<Song>, index: Int) -> Unit) {
    if (songs.isEmpty()) return Empty()
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            // 曲を押すと、曲の一覧全体を再生待ちにして、その曲から再生する
            Row(
                headline = song.title,
                supporting = "${song.artist} · ${formatDuration(song.durationMs)}",
                onClick = { onPlay(songs, index) },
            )
        }
    }
}

@Composable
private fun ArtistList(artists: List<Artist>) {
    if (artists.isEmpty()) return Empty()
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.name }) { artist ->
            Row(
                headline = artist.name,
                supporting = pluralStringResource(R.plurals.album_count, artist.albumCount, artist.albumCount) +
                    " · " + pluralStringResource(R.plurals.song_count, artist.songCount, artist.songCount),
            )
        }
    }
}

@Composable
private fun GenreList(genres: List<Genre>) {
    if (genres.isEmpty()) return Empty()
    LazyColumn(Modifier.fillMaxSize()) {
        items(genres, key = { it.name }) { genre ->
            Row(
                headline = genre.name,
                supporting = pluralStringResource(R.plurals.song_count, genre.songCount, genre.songCount),
            )
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>) {
    if (albums.isEmpty()) return Empty()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            // ジャケットは次の PR（Coil）で出す。いまは正方形の地に名前を重ねる
            Box(
                Modifier
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                        .padding(8.dp),
                ) {
                    Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(
                        album.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Row(headline: String, supporting: String, onClick: (() -> Unit)? = null) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        headlineContent = { Text(headline, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(supporting, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun Empty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.library_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Message(text: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) { Text(action) }
    }
}

/** 再生時間を「m:ss」（1 時間以上は「h:mm:ss」）にする。 */
internal fun formatDuration(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Preview
@Composable
private fun LibraryTabsPreview() {
    val songs = listOf(
        Song(1, "Blue", "Artist A", "Album X", 10, "Pop", 215_000, 1, null),
        Song(2, "Green", "Artist B", "Album Y", 11, "Rock", 3_725_000, 1, null),
    )
    LyraTheme { LibraryTabs(Library.from(songs), onPlay = { _, _ -> }) }
}
