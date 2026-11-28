package io.github.aiyzkn.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
    @Test
    fun accentRoundTripsByKey() {
        AccentColor.entries.forEach { assertEquals(it, AccentColor.fromKey(it.key)) }
    }

    @Test
    fun unknownOrMissingAccentFallsBackToBlue() {
        assertEquals(AccentColor.Blue, AccentColor.fromKey(null))
        assertEquals(AccentColor.Blue, AccentColor.fromKey("pink"))
    }

    @Test
    fun albumLayoutRoundTripsAndDefaultsToGrid() {
        AlbumLayout.entries.forEach { assertEquals(it, AlbumLayout.fromKey(it.key)) }
        assertEquals(AlbumLayout.Grid, AlbumLayout.fromKey(null))
        assertEquals(AlbumLayout.Grid, AlbumLayout.fromKey("table"))
    }

    @Test
    fun keysAreUnique() {
        assertEquals(AccentColor.entries.size, AccentColor.entries.map { it.key }.toSet().size)
        assertEquals(AlbumLayout.entries.size, AlbumLayout.entries.map { it.key }.toSet().size)
    }
}
