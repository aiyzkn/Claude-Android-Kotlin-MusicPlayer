package io.github.aiyzkn.lyra

import android.os.Bundle
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
        enableEdgeToEdge()
        setContent {
            LyraTheme {
                LyraApp()
            }
        }
    }
}
