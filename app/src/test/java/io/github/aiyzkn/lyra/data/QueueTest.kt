package io.github.aiyzkn.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueTest {
    private fun song(id: Long) = Song(id, "s$id", "A", "B", 1, null, 0, 0, null)
    private fun library(vararg ids: Long) = ids.associate { it to song(it) }

    @Test
    fun restoresInOrderAndKeepsThePosition() {
        val r = restoreQueue(listOf(3, 1, 2), 1, 42_000, library(1, 2, 3))!!
        assertEquals(listOf(3L, 1L, 2L), r.songs.map { it.id })
        assertEquals(1, r.currentIndex)
        assertEquals(42_000L, r.positionMs)
    }

    @Test
    fun skipsMissingSongsAndShiftsTheCurrentIndex() {
        // 0 番目（id 9）が端末から消えた → 聴いていた id 2 は 1 番目から 0 番目へ
        val r = restoreQueue(listOf(9, 2, 3), 1, 5_000, library(2, 3))!!
        assertEquals(listOf(2L, 3L), r.songs.map { it.id })
        assertEquals(0, r.currentIndex)
        assertEquals(5_000L, r.positionMs)
    }

    @Test
    fun missingCurrentSongMovesToTheNextFromTheStart() {
        val r = restoreQueue(listOf(1, 9, 3), 1, 5_000, library(1, 3))!!
        assertEquals(1, r.currentIndex)
        assertEquals(3L, r.songs[r.currentIndex].id)
        assertEquals(0L, r.positionMs)
    }

    @Test
    fun missingLastSongFallsBackToTheLastRemaining() {
        val r = restoreQueue(listOf(1, 2, 9), 2, 5_000, library(1, 2))!!
        assertEquals(1, r.currentIndex)
        assertEquals(0L, r.positionMs)
    }

    @Test
    fun outOfRangeIndexIsClamped() {
        assertEquals(0, restoreQueue(listOf(1, 2), -1, 0, library(1, 2))!!.currentIndex)
        val r = restoreQueue(listOf(1, 2), 7, 9_000, library(1, 2))!!
        assertEquals(1, r.currentIndex)
        assertEquals(0L, r.positionMs)
    }

    @Test
    fun nothingLeftIsNull() {
        assertNull(restoreQueue(listOf(8, 9), 0, 0, library(1)))
        assertNull(restoreQueue(emptyList(), 0, 0, library(1)))
    }

    @Test
    fun dragTargetMovesOneRowPerItemHeight() {
        assertEquals(2, dragTarget(2, 10f, 100f, 5))
        assertEquals(3, dragTarget(2, 60f, 100f, 5))
        assertEquals(4, dragTarget(2, 180f, 100f, 5))
        assertEquals(1, dragTarget(2, -60f, 100f, 5))
    }

    @Test
    fun dragTargetStaysInsideTheList() {
        assertEquals(4, dragTarget(2, 10_000f, 100f, 5))
        assertEquals(0, dragTarget(2, -10_000f, 100f, 5))
        assertEquals(2, dragTarget(2, 500f, 0f, 5))
        assertEquals(0, dragTarget(0, 0f, 100f, 0))
    }
}
