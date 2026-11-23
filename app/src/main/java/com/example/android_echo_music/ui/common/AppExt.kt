package com.example.android_echo_music.ui.common

import androidx.fragment.app.Fragment
import com.example.android_echo_music.AppContainer
import com.example.android_echo_music.EchoMusicApplication

/** 统一获取依赖容器，避免每个 Fragment 重复强转 Application。 */
val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as EchoMusicApplication).container