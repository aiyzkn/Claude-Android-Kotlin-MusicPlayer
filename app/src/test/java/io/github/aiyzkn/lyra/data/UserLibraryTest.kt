package io.github.aiyzkn.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserLibraryTest {
    private val day = 24L * 60 * 60
    private val now = 1_800_000_000L

    private fun song(id: Long, title: String, addedSec: Long = 0) =
        Song(id, title, "A", "B", 1, null, 0, 0, null, addedSec)

    @Test
    fun recentlyAddedKeepsTheLastFourWeeksNewestFirst() {
        val songs = listOf(
            song(1, "old", now - 29 * day),
            song(2, "week", now - 7 * day),
            song(3, "today", now - 60),
            song(4, "edge", now - 28 * day),
            song(5, "unknown", 0),
            song(6, "future", now + day),
        )
        assertEquals(listOf("today", "week", "edge"), recentlyAdded(songs, now).map { it.title })
    }

    @Test
    fun recentlyAddedBreaksTiesByTitle() {
        val songs = listOf(song(1, "beta", now), song(2, "Alpha", now))
        assertEquals(listOf("Alpha", "beta"), recentlyAdded(songs, now).map { it.title })
    }

    @Test
    fun resolveSongsKeepsOrderDuplicatesAndSkipsMissing() {
        val byId = listOf(song(1, "one"), song(2, "two")).associateBy { it.id }
        assertEquals(listOf("two", "one", "two"), resolveSongs(listOf(2, 9, 1, 2), byId).map { it.title })
    }

    @Test
    fun playlistNamesAreTrimmedAndCollapsed() {
        assertEquals("Road trip", normalizePlaylistName("  Road \t  trip \n"))
        assertNull(normalizePlaylistName("   "))
        assertEquals(PLAYLIST_NAME_MAX, normalizePlaylistName("x".repeat(500))!!.length)
    }

    @Test
    fun libraryIndexesSongsById() {
        val library = Library.from(listOf(song(7, "seven"), song(3, "three")))
        assertEquals("seven", library.byId.getValue(7).title)
        assertEquals(setOf(3L, 7L), library.byId.keys)
    }
}
