package io.github.aiyzkn.lyra.data

import android.Manifest
import io.github.aiyzkn.lyra.ui.library.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTest {
    private fun song(id: Long, title: String, artist: String, album: String, albumId: Long, genre: String? = null) =
        Song(id, title, artist, album, albumId, genre, 0, 0, null)

    private val songs = listOf(
        song(1, "b-side", "Zed", "Second", 20, "Rock"),
        song(2, "Alpha", "anna", "First", 10, "Pop"),
        song(3, "Charlie", "Zed", "Second", 20, " Rock "),
        song(4, "delta", "Bob", "First", 10, null),
        song(5, "Echo", "Zed", "Third", 30, ""),
    )

    @Test
    fun songsAreSortedByTitleIgnoringCase() {
        assertEquals(listOf("Alpha", "b-side", "Charlie", "delta", "Echo"), Library.from(songs).songs.map { it.title })
    }

    @Test
    fun albumsAreGroupedById() {
        val albums = albumsOf(songs)
        assertEquals(listOf("First", "Second", "Third"), albums.map { it.title })
        assertEquals(listOf(2, 2, 1), albums.map { it.songCount })
        assertEquals("Zed", albums[1].artist)
    }

    @Test
    fun artistsCountAlbumsAndSongs() {
        val artists = artistsOf(songs)
        assertEquals(listOf("anna", "Bob", "Zed"), artists.map { it.name })
        val zed = artists.last()
        assertEquals(2, zed.albumCount)
        assertEquals(3, zed.songCount)
    }

    @Test
    fun genresSkipBlankAndTrim() {
        val genres = genresOf(songs)
        assertEquals(listOf(Genre("Pop", 1), Genre("Rock", 2)), genres)
    }

    @Test
    fun permissionDependsOnSdk() {
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, audioPermission(32))
        assertEquals(Manifest.permission.READ_MEDIA_AUDIO, audioPermission(33))
    }

    @Test
    fun durationIsFormatted() {
        assertEquals("0:00", formatDuration(-5))
        assertEquals("3:35", formatDuration(215_000))
        assertEquals("1:02:05", formatDuration(3_725_000))
    }
}
