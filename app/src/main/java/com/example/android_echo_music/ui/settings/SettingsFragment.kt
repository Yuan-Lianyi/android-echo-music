package com.example.android_echo_music.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.android_echo_music.BuildConfig
import com.example.android_echo_music.databinding.FragmentSettingsBinding

/** 设置页占位（F22：主题、播放模式、扫描目录，W3 补齐）。 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.versionText.text = getString(
            com.example.android_echo_music.R.string.settings_version,
            BuildConfig.VERSION_NAME
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}