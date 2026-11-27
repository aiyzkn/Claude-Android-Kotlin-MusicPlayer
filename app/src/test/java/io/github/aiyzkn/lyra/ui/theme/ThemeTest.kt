package io.github.aiyzkn.lyra.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {
    @Test
    fun accentIsHoloBlue() {
        assertEquals(HoloBlueLight, LyraColorScheme.primary)
    }

    @Test
    fun backgroundIsBlack() {
        assertEquals(HoloBackground, LyraColorScheme.background)
    }
}
