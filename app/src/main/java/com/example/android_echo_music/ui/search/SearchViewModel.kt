package com.example.android_echo_music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.data.repository.MusicRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** 搜索页 ViewModel：输入防抖 250ms 后查询。 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    val results: StateFlow<List<Song>> = query
        .debounce(DEBOUNCE_MS)
        .flatMapLatest { keyword -> repository.search(keyword) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChanged(text: String) {
        query.value = text
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
    }
}