package com.example.android_echo_music.ui.player

import androidx.lifecycle.ViewModel
import com.example.android_echo_music.playback.PlaybackState
import com.example.android_echo_music.playback.PlayerController
import kotlinx.coroutines.flow.StateFlow

/** 播放页 ViewModel：只做转调，播放逻辑全在 PlayerController。 */
class PlayerViewModel(
    private val controller: PlayerController
) : ViewModel() {

    val state: StateFlow<PlaybackState> = controller.state

    fun toggle() = controller.toggle()

    fun next() = controller.next()

    fun previous() = controller.previous()

    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    fun cycleMode() = controller.setMode(controller.state.value.mode.next())
}