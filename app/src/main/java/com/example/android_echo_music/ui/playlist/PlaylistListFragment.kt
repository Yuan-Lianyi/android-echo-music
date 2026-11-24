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
import com.example.android_echo_music.databinding.FragmentPlaylistListBinding
import com.example.android_echo_music.ui.common.appContainer
import com.example.android_echo_music.ui.common.showNameInputDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 歌单列表页（[C] 维护）：列表 + 新建 + 长按重命名 / 删除。
 *
 * 数据全部来自 [com.example.android_echo_music.data.repository.PlaylistRepository]，
 * 页面不出现 Room 类型。
 */
class PlaylistListFragment : Fragment() {

    private var _binding: FragmentPlaylistListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaylistAdapter

    private val viewModel: PlaylistViewModel by viewModels {
        viewModelFactory {
            initializer { PlaylistViewModel(appContainer.playlistRepository) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = PlaylistAdapter(
            onPlaylistClick = ::openPlaylist,
            onPlaylistLongClick = { playlist, _ -> showPlaylistOptions(playlist) }
        )
        binding.playlistList.layoutManager = LinearLayoutManager(requireContext())
        binding.playlistList.adapter = adapter

        binding.createPlaylistButton.setOnClickListener {
            showNameInputDialog(
                titleRes = R.string.playlist_create,
                hintRes = R.string.playlist_create_hint
            ) { name -> viewModel.createPlaylist(name) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlists.collect { playlists ->
                        adapter.submitList(playlists)
                        binding.playlistEmpty.isVisible = playlists.isEmpty()
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

    private fun openPlaylist(playlist: Playlist) {
        val args = Bundle().apply {
            putLong(PlaylistDetailFragment.ARG_PLAYLIST_ID, playlist.id)
        }
        findNavController().navigate(R.id.action_playlist_to_detail, args)
    }

    private fun showPlaylistOptions(playlist: Playlist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(playlist.name)
            .setItems(
                arrayOf(
                    getString(R.string.action_rename),
                    getString(R.string.action_delete)
                )
            ) { _, which ->
                when (which) {
                    0 -> promptRename(playlist)
                    1 -> confirmDelete(playlist)
                }
            }
            .show()
    }

    private fun promptRename(playlist: Playlist) {
        showNameInputDialog(
            titleRes = R.string.playlist_rename,
            hintRes = R.string.playlist_rename_hint,
            initialName = playlist.name
        ) { name -> viewModel.renamePlaylist(playlist.id, name) }
    }

    private fun confirmDelete(playlist: Playlist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_delete)
            .setMessage(getString(R.string.playlist_delete_message, playlist.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deletePlaylist(playlist.id)
                toast(getString(R.string.playlist_deleted))
            }
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        binding.playlistList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}