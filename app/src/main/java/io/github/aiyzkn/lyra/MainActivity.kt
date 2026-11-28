package io.github.aiyzkn.lyra

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.aiyzkn.lyra.data.Settings
import io.github.aiyzkn.lyra.data.SettingsRepository
import io.github.aiyzkn.lyra.ui.LyraApp
import io.github.aiyzkn.lyra.ui.theme.LyraTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 地が黒なので、ステータスバー・ナビゲーションバーの文字とアイコンを明るい色にする
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = Settings())
            LyraTheme(accent = settings.accent) {
                LyraApp()
            }
        }
    }
}
