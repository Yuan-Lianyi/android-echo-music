package com.example.android_echo_music.playback

/**
 * 播放模式（[A] 维护）。
 *
 * 顺序播放：列表放完循环；单曲循环；随机播放。
 */
enum class PlayMode {
    SEQUENTIAL,
    REPEAT_ONE,
    SHUFFLE;

    /** 点击模式按钮时的轮换顺序。 */
    fun next(): PlayMode = when (this) {
        SEQUENTIAL -> REPEAT_ONE
        REPEAT_ONE -> SHUFFLE
        SHUFFLE -> SEQUENTIAL
    }
}