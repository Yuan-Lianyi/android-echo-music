package com.example.android_echo_music.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_echo_music.data.model.Playlist
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.data.repository.MusicRepository
import com.example.android_echo_music.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 歌单详情 ViewModel（[C] 维护）。
 *
 * Room 只返回有序 songId，这里与 [MusicRepository] 的歌曲列表合并成可渲染的 [Song]，
 * 已被删除的本地文件会被自动过滤掉（[PlaylistRepository] 侧后续再清理关联）。
 */
class PlaylistDetailViewModel(
    private val playlistRepository: PlaylistRepository,
    musicRepository: MusicRepository,
    private val playlistId: Long
) : ViewModel() {

    val playlist: StateFlow<Playlist?> = playlistRepository.observePlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<List<Song>> = combine(
        playlistRepository.observePlaylistSongIds(playlistId),
        musicRepository.observeSongs()
    ) { songIds, allSongs ->
        val songsById = allSongs.associateBy { it.id }
        songIds.mapNotNull { songId -> songsById[songId] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun renamePlaylist(name: String) = runOperation {
        playlistRepository.renamePlaylist(playlistId, name)
    }

    fun removeSong(songId: Long) = runOperation {
        playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }

    /** 删除成功后回调，由 Fragment 关闭详情页。 */
    fun deletePlaylist(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { playlistRepository.deletePlaylist(playlistId) }
                .onSuccess { onDeleted() }
                .onFailure { _errorMessage.value = it.message.orEmpty() }
        }
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