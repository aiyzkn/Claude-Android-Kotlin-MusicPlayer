package io.github.aiyzkn.lyra.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.Album
import io.github.aiyzkn.lyra.data.Library
import io.github.aiyzkn.lyra.data.Song
import io.github.aiyzkn.lyra.data.search

/**
 * 曲・アーティスト・アルバムの横断検索。入力のたびに絞り込む（端末の中だけを探すので、送信は要らない）。
 * 結果は アーティスト → アルバム → 曲 の順に見出しを付けて並べる。曲を押すと、検索に当たった曲を再生待ちにして再生する。
 */
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    library: Library,
    onPlay: (songs: List<Song>, index: Int) -> Unit,
    actions: SongActions,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (Album) -> Unit,
) {
    val result = remember(library, query) { library.search(query) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.search_clear))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .focusRequester(focus),
        )
        when {
            query.isBlank() -> Unit
            result.isEmpty -> Empty(text = stringResource(R.string.search_empty))
            else -> LazyColumn(Modifier.fillMaxSize()) {
                if (result.artists.isNotEmpty()) {
                    item(key = "h:artists") { SectionHeader(stringResource(R.string.tab_artists)) }
                    items(result.artists, key = { "artist:${it.name}" }) { artist ->
                        DetailRow(
                            headline = artist.name,
                            supporting = pluralStringResource(R.plurals.album_count, artist.albumCount, artist.albumCount) +
                                " · " + pluralStringResource(R.plurals.song_count, artist.songCount, artist.songCount),
                            onClick = { onOpenArtist(artist.name) },
                        )
                    }
                }
                if (result.albums.isNotEmpty()) {
                    item(key = "h:albums") { SectionHeader(stringResource(R.string.tab_albums)) }
                    items(result.albums, key = { "album:${it.id}" }) { album ->
                        DetailRow(
                            headline = album.title,
                            supporting = album.artist,
                            onClick = { onOpenAlbum(album) },
                        )
                    }
                }
                if (result.songs.isNotEmpty()) {
                    item(key = "h:songs") { SectionHeader(stringResource(R.string.tab_songs)) }
                    itemsIndexed(result.songs, key = { _, song -> "song:${song.id}" }) { index, song ->
                        SongRow(song = song, onClick = { onPlay(result.songs, index) }, actions = actions, onRemove = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
