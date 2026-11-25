package com.example.android_echo_music.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.android_echo_music.data.model.Playlist
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.databinding.FragmentPlaylistDetailBinding
import com.example.android_echo_music.ui.common.appContainer
import com.example.android_echo_music.ui.common.showNameInputDialog
import com.example.android_echo_music.ui.library.SongAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 歌单详情页（[C] 维护）：歌曲列表、播放全部、重命名 / 删除、移出歌曲。
 */
class PlaylistDetailFragment : Fragment() {

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SongAdapter

    private val viewModel: PlaylistDetailViewModel by viewModels {
        viewModelFactory {
            initializer {
                PlaylistDetailViewModel(
                    playlistRepository = appContainer.playlistRepository,
                    musicRepository = appContainer.musicRepository,
                    playlistId = requireArguments().getLong(ARG_PLAYLIST_ID)
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SongAdapter(
            onSongClick = ::playSong,
            onSongLongClick = ::confirmRemoveSong
        )
        binding.songList.layoutManager = LinearLayoutManager(requireContext())
        binding.songList.adapter = adapter

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.moreButton.setOnClickListener { showPlaylistOptions() }
        binding.playAllButton.setOnClickListener { playAll() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlist.collect(::renderPlaylist)
                }
                launch {
                    viewModel.songs.collect { songs ->
                        adapter.submitList(songs)
                        binding.songEmpty.isVisible = songs.isEmpty()
                        binding.playAllButton.isEnabled = songs.isNotEmpty()
                    }
                }
                launch {
                    viewModel.errorMessage.collect { message ->
                        if (message != null) {
                            toast(message.ifBlank { getString(R.string.error_generic) })
                            viewModel.consumeError()
                        }
                    }
                }
            }
        }
    }

    private fun renderPlaylist(playlist: Playlist?) {
        binding.playlistTitle.text = playlist?.name ?: getString(R.string.nav_playlist)
        binding.playlistSubtitle.text =
            getString(R.string.playlist_song_count, playlist?.songCount ?: 0)
    }

    private fun playAll() {
        val songs = viewModel.songs.value
        if (songs.isEmpty()) return
        appContainer.playerController.play(songs.first(), songs)
        findNavController().navigate(R.id.action_global_player)
    }

    private fun playSong(song: Song) {
        val songs = viewModel.songs.value
        appContainer.playerController.play(song, songs)
        findNavController().navigate(R.id.action_global_player)
    }

    private fun confirmRemoveSong(song: Song) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_song_remove)
            .setMessage(getString(R.string.playlist_song_remove_message, song.title))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.removeSong(song.id)
                toast(getString(R.string.playlist_song_removed))
            }
            .show()
    }

    private fun showPlaylistOptions() {
        val playlist = viewModel.playlist.value ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(playlist.name)
            .setItems(
                arrayOf(
                    getString(R.string.action_rename),
                    getString(R.string.action_delete)
                )
            ) { _, which ->
                when (which) {
                    0 -> promptRename()
                    1 -> confirmDelete(playlist)
                }
            }
            .show()
    }

    private fun promptRename() {
        val playlist = viewModel.playlist.value ?: return
        showNameInputDialog(
            titleRes = R.string.playlist_rename,
            hintRes = R.string.playlist_rename_hint,
            initialName = playlist.name
        ) { name -> viewModel.renamePlaylist(name) }
    }

    private fun confirmDelete(playlist: Playlist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_delete)
            .setMessage(getString(R.string.playlist_delete_message, playlist.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deletePlaylist {
                    toast(getString(R.string.playlist_deleted))
                    findNavController().navigateUp()
                }
            }
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        binding.songList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_PLAYLIST_ID = "playlistId"
    }
}