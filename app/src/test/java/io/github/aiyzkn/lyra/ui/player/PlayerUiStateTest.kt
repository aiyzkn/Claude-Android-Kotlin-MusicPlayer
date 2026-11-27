package io.github.aiyzkn.lyra.ui.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerUiStateTest {
    @Test
    fun repeatCyclesOffAllOne() {
        assertEquals(RepeatMode.All, RepeatMode.Off.next())
        assertEquals(RepeatMode.One, RepeatMode.All.next())
        assertEquals(RepeatMode.Off, RepeatMode.One.next())
    }

    @Test
    fun repeatMapsMedia3Constants() {
        assertEquals(RepeatMode.Off, RepeatMode.from(Player.REPEAT_MODE_OFF))
        assertEquals(RepeatMode.All, RepeatMode.from(Player.REPEAT_MODE_ALL))
        assertEquals(RepeatMode.One, RepeatMode.from(Player.REPEAT_MODE_ONE))
        assertEquals(RepeatMode.Off, RepeatMode.from(99))
    }

    @Test
    fun progressIsClampedAndSafeWithoutDuration() {
        assertEquals(0f, PlayerUiState(positionMs = 5_000, durationMs = 0).progress)
        assertEquals(0.5f, PlayerUiState(positionMs = 50_000, durationMs = 100_000).progress)
        assertEquals(1f, PlayerUiState(positionMs = 120_000, durationMs = 100_000).progress)
    }

    @Test
    fun hasMediaFollowsTitle() {
        assertFalse(PlayerUiState().hasMedia)
        assertTrue(PlayerUiState(title = "").hasMedia)
    }
}
