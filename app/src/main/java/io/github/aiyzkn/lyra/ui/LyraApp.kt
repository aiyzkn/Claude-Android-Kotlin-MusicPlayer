package io.github.aiyzkn.lyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.ui.theme.LyraTheme

/**
 * アプリの最上位の画面。
 * いまは骨組みだけ（ライブラリのタブ・ミニプレーヤー・再生画面は、次の PR から足す）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyraApp() {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text(stringResource(R.string.app_name)) })
                // Holo の操作バーの下にあった、アクセントの色の細い線
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.library_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun LyraAppPreview() {
    LyraTheme {
        LyraApp()
    }
}
