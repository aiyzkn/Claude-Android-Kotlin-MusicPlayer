package io.github.aiyzkn.lyra.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aiyzkn.lyra.R
import io.github.aiyzkn.lyra.data.AccentColor
import io.github.aiyzkn.lyra.data.AlbumLayout
import io.github.aiyzkn.lyra.data.Settings
import io.github.aiyzkn.lyra.data.SettingsRepository
import io.github.aiyzkn.lyra.playback.AudioSession
import io.github.aiyzkn.lyra.ui.theme.LyraTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    audioSession: AudioSession,
) : ViewModel() {
    val settings: StateFlow<Settings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    /** イコライザーの画面に渡すセッションの ID（再生のサービスと同じもの）。 */
    val audioSessionId: Int = audioSession.id

    fun setAccent(accent: AccentColor) {
        viewModelScope.launch { repository.setAccent(accent) }
    }

    fun setAlbumLayout(layout: AlbumLayout) {
        viewModelScope.launch { repository.setAlbumLayout(layout) }
    }
}

/** 設定の画面。見た目（アクセントの色・アルバムの表示）と、端末のイコライザーの呼び出し。 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onAccentChange: (AccentColor) -> Unit,
    onAlbumLayoutChange: (AlbumLayout) -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader(stringResource(R.string.settings_appearance))
        Text(
            stringResource(R.string.settings_accent),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccentColor.entries.forEach { accent ->
                AccentSwatch(accent, selected = accent == settings.accent, onClick = { onAccentChange(accent) })
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Text(
            stringResource(R.string.settings_album_layout),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        AlbumLayout.entries.forEach { layout ->
            ChoiceRow(
                label = stringResource(layout.labelRes()),
                selected = layout == settings.albumLayout,
                onClick = { onAlbumLayoutChange(layout) },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        SectionHeader(stringResource(R.string.settings_audio))
        ListItem(
            modifier = Modifier.clickable(onClick = onOpenEqualizer),
            headlineContent = { Text(stringResource(R.string.action_equalizer)) },
            supportingContent = { Text(stringResource(R.string.settings_equalizer_summary)) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
    }
}

/** Holo の設定の見出し（アクセントの色の小さな大文字）。 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun AccentSwatch(accent: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val label = stringResource(accent.labelRes())
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(accent.light))
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 16.dp))
    }
}

private fun AccentColor.labelRes(): Int = when (this) {
    AccentColor.Blue -> R.string.accent_blue
    AccentColor.Green -> R.string.accent_green
    AccentColor.Orange -> R.string.accent_orange
    AccentColor.Purple -> R.string.accent_purple
    AccentColor.Red -> R.string.accent_red
}

private fun AlbumLayout.labelRes(): Int = when (this) {
    AlbumLayout.Grid -> R.string.album_layout_grid
    AlbumLayout.List -> R.string.album_layout_list
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    LyraTheme {
        SettingsScreen(Settings(), {}, {}, {})
    }
}
