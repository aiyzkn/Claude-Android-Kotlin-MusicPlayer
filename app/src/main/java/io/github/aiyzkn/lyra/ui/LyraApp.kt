package io.github.aiyzkn.lyra.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.audioPermission
import io.github.aiyzkn.lyra.ui.library.LibraryContent
import io.github.aiyzkn.lyra.ui.library.LibraryViewModel

/**
 * アプリの最上位の画面。いまはライブラリのタブだけ（ミニプレーヤー・再生画面は次の PR から）。
 * `viewModel()` は、`@AndroidEntryPoint` の Activity の中では Hilt の生成器を使う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyraApp(viewModel: LibraryViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = audioPermission()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.onPermissionGranted()
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionGranted()
        } else {
            launcher.launch(permission)
        }
    }

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
        LibraryContent(
            state = state,
            onRequestPermission = { launcher.launch(permission) },
            onRetry = viewModel::refresh,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
