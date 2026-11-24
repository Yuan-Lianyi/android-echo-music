package com.example.android_echo_music.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
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
import com.example.android_echo_music.databinding.FragmentSearchBinding
import com.example.android_echo_music.ui.common.appContainer
import com.example.android_echo_music.ui.library.SongAdapter
import com.example.android_echo_music.ui.playlist.AddToPlaylistSheet
import kotlinx.coroutines.launch

/** 搜索页：按歌名 / 艺术家 / 专辑实时过滤。 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SongAdapter

    private val viewModel: SearchViewModel by viewModels {
        viewModelFactory {
            initializer { SearchViewModel(appContainer.musicRepository) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SongAdapter(
            onSongClick = ::playSong,
            onSongLongClick = ::showAddToPlaylistSheet
        )
        binding.searchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResults.adapter = adapter

        binding.searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect { songs ->
                    adapter.submitList(songs)
                    val keyword = binding.searchInput.text?.toString().orEmpty()
                    binding.searchEmpty.isVisible = songs.isEmpty()
                    binding.searchEmpty.text = if (keyword.isBlank()) {
                        getString(R.string.search_hint)
                    } else {
                        getString(R.string.search_empty, keyword)
                    }
                }
            }
        }
    }

    private fun playSong(song: Song) {
        appContainer.playerController.play(song, viewModel.results.value)
        findNavController().navigate(R.id.action_global_player)
    }

    private fun showAddToPlaylistSheet(song: Song) {
        AddToPlaylistSheet.newInstance(song)
            .show(parentFragmentManager, AddToPlaylistSheet.TAG)
    }

    override fun onDestroyView() {
        binding.searchResults.adapter = null
        _binding = null
        super.onDestroyView()
    }
}