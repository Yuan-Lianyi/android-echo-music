package com.example.android_echo_music.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android_echo_music.R
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.databinding.FragmentSongListBinding
import com.example.android_echo_music.ui.common.appContainer
import kotlinx.coroutines.launch

/** 媒体库：全部本地歌曲（W1 用假数据，W2 接 MediaStore）。 */
class SongListFragment : Fragment() {

    private var _binding: FragmentSongListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SongAdapter

    private val viewModel: SongListViewModel by viewModels {
        viewModelFactory {
            initializer { SongListViewModel(appContainer.musicRepository) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SongAdapter(::playSong)
        binding.songList.layoutManager = LinearLayoutManager(requireContext())
        binding.songList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.songs.collect { songs ->
                    adapter.submitList(songs)
                    binding.songEmpty.isVisible = songs.isEmpty()
                }
            }
        }
    }

    private fun playSong(song: Song) {
        appContainer.playerController.play(song, viewModel.songs.value)
        findNavController().navigate(R.id.action_global_player)
    }

    override fun onDestroyView() {
        binding.songList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}