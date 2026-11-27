package io.github.aiyzkn.lyra.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aiyzkn.lyra.data.Library
import io.github.aiyzkn.lyra.data.MusicLibrary
import io.github.aiyzkn.lyra.data.PlaylistSummary
import io.github.aiyzkn.lyra.data.UserLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    /** 権限がまだ無い。 */
    data object NeedsPermission : LibraryUiState
    data object Loading : LibraryUiState
    data class Loaded(val library: Library) : LibraryUiState
    data class Failed(val message: String) : LibraryUiState
}

/** 利用者が付けたもの（お気に入り・最近再生・プレイリスト）。曲は ID で持ち、画面で曲に置き換える。 */
data class UserLibraryUiState(
    val favoriteIds: List<Long> = emptyList(),
    val recentlyPlayedIds: List<Long> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
) {
    private val favoriteSet: Set<Long> by lazy { favoriteIds.toSet() }
    fun isFavorite(songId: Long): Boolean = songId in favoriteSet
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicLibrary: MusicLibrary,
    private val userLibrary: UserLibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.NeedsPermission)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    val user: StateFlow<UserLibraryUiState> = combine(
        userLibrary.favorites,
        userLibrary.recentlyPlayed,
        userLibrary.playlists,
    ) { favorites, recent, playlists -> UserLibraryUiState(favorites, recent, playlists) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserLibraryUiState())

    fun playlistSongIds(id: Long): Flow<List<Long>> = userLibrary.playlistSongs(id)

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch { userLibrary.toggleFavorite(songId) }
    }

    fun createPlaylist(name: String, songIds: List<Long> = emptyList()) {
        viewModelScope.launch { userLibrary.createPlaylist(name, songIds) }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { userLibrary.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { userLibrary.deletePlaylist(id) }
    }

    fun addToPlaylist(id: Long, songIds: List<Long>) {
        viewModelScope.launch { userLibrary.addToPlaylist(id, songIds) }
    }

    fun removeFromPlaylist(id: Long, position: Int) {
        viewModelScope.launch { userLibrary.removeFromPlaylist(id, position) }
    }

    /** 権限が得られたら呼ぶ。読み込み中・読み込み済みなら何もしない（[refresh] で読み直す）。 */
    fun onPermissionGranted() {
        if (_state.value is LibraryUiState.NeedsPermission || _state.value is LibraryUiState.Failed) refresh()
    }

    fun refresh() {
        _state.value = LibraryUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                LibraryUiState.Loaded(Library.from(musicLibrary.songs()))
            } catch (e: SecurityException) {
                LibraryUiState.NeedsPermission
            } catch (e: Exception) {
                LibraryUiState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
