package io.github.aiyzkn.lyra.widget

import io.github.aiyzkn.lyra.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateTest {
    private fun song(id: Long) = Song(id, "t$id", "A", "B", 1, null, 0, 0, null)

    @Test
    fun nowPlayingKeepsTitleAndArtist() {
        val np = nowPlayingOf("Song", " Artist ", isPlaying = true)
        assertEquals(NowPlaying("Song", "Artist", true), np)
        assertTrue(np.hasSong)
    }

    @Test
    fun blankTitleMeansNothingIsPlaying() {
        assertEquals(NowPlaying.NONE, nowPlayingOf("  ", "Artist", isPlaying = true))
        assertEquals(NowPlaying.NONE, nowPlayingOf(null, null, isPlaying = false))
        assertFalse(NowPlaying.NONE.hasSong)
    }

    @Test
    fun blankArtistIsDropped() {
        assertNull(nowPlayingOf("Song", "", isPlaying = false).artist)
    }

    @Test
    fun recentSongsSkipMissingAndKeepOrder() {
        val byId = listOf(1L, 2L, 3L).associateWith(::song)
        assertEquals(listOf(3L, 1L), recentSongsForWidget(listOf(3L, 9L, 1L, 3L), byId).map { it.id })
    }

    @Test
    fun recentSongsAreLimited() {
        val byId = (1L..20L).associateWith(::song)
        assertEquals(RECENT_WIDGET_LIMIT, recentSongsForWidget((1L..20L).toList(), byId).size)
        assertEquals(listOf(1L, 2L), recentSongsForWidget((1L..20L).toList(), byId, limit = 2).map { it.id })
    }
}
