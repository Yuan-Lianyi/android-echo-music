package com.example.android_echo_music.ui.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android_echo_music.R
import com.example.android_echo_music.data.model.Playlist
import com.example.android_echo_music.databinding.ItemPlaylistBinding

/** 歌单列表适配器：名称 + 歌曲数，长按回调用于重命名 / 删除。 */
class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onPlaylistLongClick: ((Playlist, View) -> Unit)? = null
) : ListAdapter<Playlist, PlaylistAdapter.PlaylistViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaylistViewHolder(
        private val binding: ItemPlaylistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(playlist: Playlist) {
            val context = binding.root.context
            binding.playlistName.text = playlist.name
            binding.playlistCount.text =
                context.getString(R.string.playlist_song_count_short, playlist.songCount)
            binding.root.setOnClickListener { onPlaylistClick(playlist) }
            binding.root.setOnLongClickListener { view ->
                onPlaylistLongClick?.invoke(playlist, view)
                onPlaylistLongClick != null
            }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Playlist>() {
            override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean =
                oldItem == newItem
        }
    }
}