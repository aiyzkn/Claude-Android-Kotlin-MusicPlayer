package io.github.aiyzkn.lyra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.aiyzkn.lyra.data.AccentColor

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

/** アクセントの色だけを替えた配色。地・文字の色は Holo の暗いテーマのまま。 */
fun lyraColorScheme(accent: AccentColor): ColorScheme =
    if (accent == AccentColor.DEFAULT) {
        LyraColorScheme
    } else {
        LyraColorScheme.copy(
            primary = Color(accent.light),
            primaryContainer = Color(accent.dark),
            secondary = Color(accent.dark),
        )
    }

@Composable
fun LyraTheme(accent: AccentColor = AccentColor.DEFAULT, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lyraColorScheme(accent),
        content = content,
    )
}
