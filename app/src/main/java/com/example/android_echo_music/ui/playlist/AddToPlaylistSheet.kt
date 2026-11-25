package com.example.android_echo_music.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android_echo_music.R
import com.example.android_echo_music.data.model.Playlist
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.databinding.SheetAddToPlaylistBinding
import com.example.android_echo_music.ui.common.appContainer
import com.example.android_echo_music.ui.common.showNameInputDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * 「添加到歌单」底部弹层（[C] 维护）。
 *
 * 入口：媒体库 / 首页 / 搜索结果里长按歌曲。支持加入已有歌单，或新建歌单并直接加入。
 */
class AddToPlaylistSheet : BottomSheetDialogFragment() {

    private var _binding: SheetAddToPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaylistAdapter

    private val songId: Long get() = requireArguments().getLong(ARG_SONG_ID)

    private val viewModel: AddToPlaylistViewModel by viewModels {
        viewModelFactory {
            initializer { AddToPlaylistViewModel(appContainer.playlistRepository) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetAddToPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val songTitle = arguments?.getString(ARG_SONG_TITLE).orEmpty()
        binding.songTitle.text = songTitle
        binding.songTitle.isVisible = songTitle.isNotBlank()

        adapter = PlaylistAdapter(onPlaylistClick = ::addToPlaylist)
        binding.playlistList.layoutManager = LinearLayoutManager(requireContext())
        binding.playlistList.adapter = adapter

        binding.createPlaylistButton.setOnClickListener {
            showNameInputDialog(
                titleRes = R.string.playlist_create,
                hintRes = R.string.playlist_create_hint
            ) { name ->
                viewModel.createPlaylistAndAdd(name, songId) { result ->
                    result.onSuccess {
                        toast(getString(R.string.playlist_added, name))
                        dismissAllowingStateLoss()
                    }.onFailure { error ->
                        toast(messageOf(error))
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collect { playlists ->
                    adapter.submitList(playlists)
                    binding.sheetEmpty.isVisible = playlists.isEmpty()
                }
            }
        }
    }

    private fun addToPlaylist(playlist: Playlist) {
        viewModel.addToPlaylist(playlist.id, songId) { result ->
            result.onSuccess {
                toast(getString(R.string.playlist_added, playlist.name))
                dismissAllowingStateLoss()
            }.onFailure { error ->
                toast(messageOf(error))
            }
        }
    }

    private fun messageOf(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: getString(R.string.error_generic)

    private fun toast(message: String) {
        val context = context ?: return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        binding.playlistList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "AddToPlaylistSheet"

        private const val ARG_SONG_ID = "songId"
        private const val ARG_SONG_TITLE = "songTitle"

        /** 统一入口，标题只用于弹层副标题展示。 */
        fun newInstance(song: Song): AddToPlaylistSheet = AddToPlaylistSheet().apply {
            arguments = Bundle().apply {
                putLong(ARG_SONG_ID, song.id)
                putString(ARG_SONG_TITLE, song.title)
            }
        }
    }
}