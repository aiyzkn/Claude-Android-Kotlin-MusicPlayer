package io.github.aiyzkn.lyra.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 設定の読み書きの口（DataStore）。値は名前（[AccentColor.key] など）で残し、知らない名前は既定に戻す。 */
@Singleton
class SettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            accent = AccentColor.fromKey(prefs[ACCENT]),
            albumLayout = AlbumLayout.fromKey(prefs[ALBUM_LAYOUT]),
        )
    }

    suspend fun setAccent(accent: AccentColor) {
        store.edit { it[ACCENT] = accent.key }
    }

    suspend fun setAlbumLayout(layout: AlbumLayout) {
        store.edit { it[ALBUM_LAYOUT] = layout.key }
    }

    private companion object {
        val ACCENT = stringPreferencesKey("accent")
        val ALBUM_LAYOUT = stringPreferencesKey("album_layout")
    }
}
