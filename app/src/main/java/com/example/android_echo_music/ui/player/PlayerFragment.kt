package com.example.android_echo_music.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import com.example.android_echo_music.R
import com.example.android_echo_music.common.TimeFormat
import com.example.android_echo_music.databinding.FragmentPlayerBinding
import com.example.android_echo_music.playback.PlayMode
import com.example.android_echo_music.playback.PlaybackState
import com.example.android_echo_music.ui.common.appContainer
import kotlinx.coroutines.launch

/** 播放页：封面、进度、播放控制、模式切换。 */
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private var isUserSeeking = false

    private val viewModel: PlayerViewModel by viewModels {
        viewModelFactory {
            initializer { PlayerViewModel(appContainer.playerController) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.playButton.setOnClickListener { viewModel.toggle() }
        binding.nextButton.setOnClickListener { viewModel.next() }
        binding.prevButton.setOnClickListener { viewModel.previous() }
        binding.modeButton.setOnClickListener { viewModel.cycleMode() }

        binding.progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.elapsedText.text = TimeFormat.formatDuration(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                viewModel.seekTo((seekBar?.progress ?: 0).toLong())
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: PlaybackState) {
        val song = state.currentSong
        binding.titleText.text = song?.title ?: getString(R.string.player_no_song)
        binding.artistText.text = song?.artist.orEmpty()

        val duration = state.durationMs.coerceAtLeast(1L)
        binding.progressBar.max = duration.toInt()
        if (!isUserSeeking) {
            binding.progressBar.progress = state.positionMs.toInt().coerceIn(0, binding.progressBar.max)
            binding.elapsedText.text = TimeFormat.formatDuration(state.positionMs)
        }
        binding.durationText.text = TimeFormat.formatDuration(state.durationMs)

        binding.playButton.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )
        binding.playButton.contentDescription = getString(
            if (state.isPlaying) R.string.action_pause else R.string.action_play
        )
        binding.modeButton.setImageResource(modeIcon(state.mode))
        binding.modeButton.contentDescription = getString(modeLabel(state.mode))
    }

    private fun modeIcon(mode: PlayMode): Int = when (mode) {
        PlayMode.SEQUENTIAL -> R.drawable.ic_repeat
        PlayMode.REPEAT_ONE -> R.drawable.ic_repeat_one
        PlayMode.SHUFFLE -> R.drawable.ic_shuffle
    }

    private fun modeLabel(mode: PlayMode): Int = when (mode) {
        PlayMode.SEQUENTIAL -> R.string.mode_sequential
        PlayMode.REPEAT_ONE -> R.string.mode_repeat_one
        PlayMode.SHUFFLE -> R.string.mode_shuffle
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}