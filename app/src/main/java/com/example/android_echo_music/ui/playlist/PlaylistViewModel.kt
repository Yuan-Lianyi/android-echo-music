package com.example.android_echo_music.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_echo_music.data.model.Playlist
import com.example.android_echo_music.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 歌单列表 ViewModel（[C] 维护）：只依赖 [PlaylistRepository] 接口，不碰 Room。
 */
class PlaylistViewModel(
    private val repository: PlaylistRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)

    /** 一次性错误提示（空串表示交给 UI 显示通用文案）。 */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun createPlaylist(name: String) = runOperation {
        repository.createPlaylist(name)
    }

    fun renamePlaylist(playlistId: Long, name: String) = runOperation {
        repository.renamePlaylist(playlistId, name)
    }

    fun deletePlaylist(playlistId: Long) = runOperation {
        repository.deletePlaylist(playlistId)
    }

    fun consumeError() {
        _errorMessage.value = null
    }

    private fun runOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { _errorMessage.value = it.message.orEmpty() }
        }
    }
}