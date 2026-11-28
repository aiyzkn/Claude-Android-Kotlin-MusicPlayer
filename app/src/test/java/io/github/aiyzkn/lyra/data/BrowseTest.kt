package io.github.aiyzkn.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseTest {
    private fun song(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumId: Long,
        track: Int = 0,
        genre: String? = null,
    ) = Song(id, title, artist, album, albumId, genre, 0, track, null)

    private val library = Library.from(
        listOf(
            song(1, "Third", "Zed", "Second", 20, track = 3, genre = "Rock"),
            song(2, "First", "Zed", "Second", 20, track = 1, genre = " Rock "),
            song(3, "Alpha", "Anna", "Mix", 30, track = 1, genre = "Pop"),
            song(4, "Beta", "Zed", "Mix", 30, track = 2),
            song(5, "Ｐｉａｎｏ　Ｍａｎ", "Bob", "Live", 40, track = 0),
            song(6, "Another", "Zed", "Second", 20, track = 0),
        ),
    )

    @Test
    fun albumSongsFollowTrackNumbers() {
        assertEquals(listOf("Another", "First", "Third"), library.songsOfAlbum(20).map { it.title })
    }

    @Test
    fun artistSongsAreGroupedByAlbum() {
        assertEquals(listOf("Beta", "Another", "First", "Third"), library.songsOfArtist("Zed").map { it.title })
    }

    @Test
    fun artistAlbumsIncludeCompilationsWithFullCounts() {
        val albums = library.albumsOfArtist("Zed")
        assertEquals(listOf("Mix", "Second"), albums.map { it.title })
        assertEquals(listOf(2, 3), albums.map { it.songCount })
    }

    @Test
    fun genreSongsIgnoreSurroundingSpaces() {
        assertEquals(listOf("First", "Third"), library.songsOfGenre("Rock").map { it.title })
    }

    @Test
    fun searchMatchesAllTermsAcrossFields() {
        val result = library.search("zed second")
        assertEquals(listOf("Another", "First", "Third"), result.songs.map { it.title })
        assertEquals(listOf("Second"), result.albums.map { it.title })
        assertTrue(result.artists.isEmpty())
        assertEquals(listOf("Beta"), library.search("zed beta").songs.map { it.title })
    }

    @Test
    fun searchIgnoresCaseAndWidth() {
        assertEquals(listOf("Ｐｉａｎｏ　Ｍａｎ"), library.search("piano  MAN").songs.map { it.title })
        assertEquals(listOf("Zed"), library.search("ＺＥＤ").artists.map { it.name })
    }

    @Test
    fun blankQueryFindsNothing() {
        assertTrue(library.search("   ").isEmpty)
    }
}
