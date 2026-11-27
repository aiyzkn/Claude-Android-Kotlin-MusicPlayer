package io.github.aiyzkn.lyra.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aiyzkn.lyra.data.Library
import io.github.aiyzkn.lyra.data.MusicLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    /** 権限がまだ無い。 */
    data object NeedsPermission : LibraryUiState
    data object Loading : LibraryUiState
    data class Loaded(val library: Library) : LibraryUiState
    data class Failed(val message: String) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicLibrary: MusicLibrary,
) : ViewModel() {

    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.NeedsPermission)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

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
