package io.github.aiyzkn.lyra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Holo 風の暗い配色（黒地・水色のアクセント）。端末の壁紙の色（ダイナミック カラー）は使わない。 */
val LyraColorScheme: ColorScheme = darkColorScheme(
    primary = HoloBlueLight,
    onPrimary = HoloBackground,
    primaryContainer = HoloBlueDark,
    onPrimaryContainer = HoloTextPrimary,
    secondary = HoloBlueDark,
    onSecondary = HoloTextPrimary,
    background = HoloBackground,
    onBackground = HoloTextPrimary,
    surface = HoloSurface,
    onSurface = HoloTextPrimary,
    surfaceVariant = HoloSurfaceVariant,
    onSurfaceVariant = HoloTextSecondary,
)

@Composable
fun LyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LyraColorScheme,
        content = content,
    )
}
