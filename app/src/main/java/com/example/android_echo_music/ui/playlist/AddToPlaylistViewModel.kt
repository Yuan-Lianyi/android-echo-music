package com.example.android_echo_music.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_echo_music.data.model.Playlist
import com.example.android_echo_music.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 「添加到歌单」弹层的 ViewModel（[C] 维护）。
 *
 * 支持两种路径：加入已有歌单、新建歌单并直接加入。
 */
class AddToPlaylistViewModel(
    private val repository: PlaylistRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToPlaylist(
        playlistId: Long,
        songId: Long,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(runCatching { repository.addSongToPlaylist(playlistId, songId) })
        }
    }

    fun createPlaylistAndAdd(
        name: String,
        songId: Long,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                runCatching {
                    val playlistId = repository.createPlaylist(name)
                    repository.addSongToPlaylist(playlistId, songId)
                    playlistId
                }
            )
        }
    }
}