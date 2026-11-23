package com.example.android_echo_music

import android.app.Application

/**
 * 进程入口：只做容器初始化。
 * 数据库、通知渠道等后续在这里补齐（B 的 W2 任务）。
 */
class EchoMusicApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}