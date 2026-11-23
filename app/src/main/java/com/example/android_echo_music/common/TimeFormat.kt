package com.example.android_echo_music.common

import java.util.Locale

/** 时长格式化：`mm:ss`，超过 1 小时用 `h:mm:ss`。 */
object TimeFormat {

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "00:00"
        val totalSeconds = durationMs / 1_000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes >= 60) {
            String.format(Locale.ROOT, "%d:%02d:%02d", minutes / 60, minutes % 60, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }
}