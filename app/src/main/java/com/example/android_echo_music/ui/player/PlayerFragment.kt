package com.example.android_echo_music.ui.player

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.core.view.isVisible
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
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.databinding.FragmentPlayerBinding
import com.example.android_echo_music.playback.PlayMode
import com.example.android_echo_music.playback.PlaybackState
import com.example.android_echo_music.ui.common.appContainer
import com.example.android_echo_music.ui.common.loadCoverArt
import kotlinx.coroutines.launch

/**
 * 播放页（[C] 维护）：大封面唱片、进度、播放控制、模式切换。
 *
 * 唱片旋转跟随播放状态：播放时匀速转动，暂停时停在当前角度，切歌回到 0°。
 */
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private var isUserSeeking = false
    private var rotationAnimator: ObjectAnimator? = null
    private var lastSongId: Long? = null

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
        rotationAnimator = ObjectAnimator.ofFloat(binding.coverContainer, View.ROTATION, 0f, 360f)
            .apply {
                duration = VINYL_ROTATION_MS
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
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

        if (song?.id != lastSongId) {
            lastSongId = song?.id
            applyCover(song)
            resetVinyl()
        }
        updateVinylSpin(state.isPlaying)

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

    private fun applyCover(song: Song?) {
        val artUri = song?.albumArtUri
        binding.coverArt.loadCoverArt(artUri)
        binding.coverNote.isVisible = artUri.isNullOrBlank()
    }

    private fun resetVinyl() {
        val animator = rotationAnimator ?: return
        animator.cancel()
        binding.coverContainer.rotation = 0f
    }

    private fun updateVinylSpin(isPlaying: Boolean) {
        val animator = rotationAnimator ?: return
        if (isPlaying) {
            when {
                !animator.isStarted -> animator.start()
                animator.isPaused -> animator.resume()
            }
        } else if (animator.isStarted) {
            animator.pause()
        }
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
        rotationAnimator?.cancel()
        rotationAnimator = null
        // 视图重建后需要重新套用封面与唱片角度
        lastSongId = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        /** 唱片转一圈的耗时：慢速转动，避免视觉上发飘。 */
        const val VINYL_ROTATION_MS = 20_000L
    }
}