package io.github.aiyzkn.lyra.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.aiyzkn.lyra.data.AccentColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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

    @Test
    fun defaultAccentKeepsHoloScheme() {
        assertSame(LyraColorScheme, lyraColorScheme(AccentColor.Blue))
    }

    @Test
    fun otherAccentChangesOnlyAccentColors() {
        val scheme = lyraColorScheme(AccentColor.Orange)
        assertEquals(Color(AccentColor.Orange.light), scheme.primary)
        assertEquals(Color(AccentColor.Orange.dark), scheme.primaryContainer)
        assertEquals(HoloBackground, scheme.background)
        assertEquals(HoloTextPrimary, scheme.onSurface)
    }
}
