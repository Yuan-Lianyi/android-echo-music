package com.example.android_echo_music.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.data.repository.MusicRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 媒体库歌曲列表的 ViewModel（[C] 维护，W2 接入 B 的真实扫描结果）。 */
class SongListViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    val songs: StateFlow<List<Song>> = repository.observeSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            // 首次进入触发一次扫描；没有权限时由 B 的实现自行处理或抛错
            runCatching { repository.refresh() }
        }
    }
}