package com.example.android_echo_music

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.android_echo_music.databinding.ActivityMainBinding

/**
 * 单 Activity + NavHost 结构（[C] 维护）。
 * 底部导航负责四个主页面，播放页作为独立目的地全屏展示。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // 播放页全屏，不显示底部导航
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id != R.id.playerFragment
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }
}