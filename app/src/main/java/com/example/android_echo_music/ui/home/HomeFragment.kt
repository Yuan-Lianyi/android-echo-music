package com.example.android_echo_music.ui.home

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
import com.example.android_echo_music.databinding.FragmentHomeBinding
import com.example.android_echo_music.ui.common.appContainer
import com.example.android_echo_music.ui.library.SongAdapter
import com.example.android_echo_music.ui.library.SongListViewModel
import com.example.android_echo_music.ui.playlist.AddToPlaylistSheet
import kotlinx.coroutines.launch

/** 首页：欢迎语 + 最近歌曲入口（W2 换成「最近播放 / 最近添加」）。 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SongAdapter(
            onSongClick = ::playSong,
            onSongLongClick = ::showAddToPlaylistSheet
        )
        binding.homeSongList.layoutManager = LinearLayoutManager(requireContext())
        binding.homeSongList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.songs.collect { songs ->
                    adapter.submitList(songs)
                    binding.homeEmpty.isVisible = songs.isEmpty()
                    binding.homeSubtitle.text = getString(R.string.home_song_count, songs.size)
                }
            }
        }
    }

    private fun playSong(song: Song) {
        appContainer.playerController.play(song, viewModel.songs.value)
        findNavController().navigate(R.id.action_global_player)
    }

    private fun showAddToPlaylistSheet(song: Song) {
        AddToPlaylistSheet.newInstance(song)
            .show(parentFragmentManager, AddToPlaylistSheet.TAG)
    }

    override fun onDestroyView() {
        binding.homeSongList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}