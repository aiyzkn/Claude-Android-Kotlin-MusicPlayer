package io.github.aiyzkn.lyra

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.aiyzkn.lyra.ui.LyraApp
import io.github.aiyzkn.lyra.ui.theme.LyraTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 地が黒なので、ステータスバー・ナビゲーションバーの文字とアイコンを明るい色にする
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            LyraTheme {
                LyraApp()
            }
        }
    }
}
