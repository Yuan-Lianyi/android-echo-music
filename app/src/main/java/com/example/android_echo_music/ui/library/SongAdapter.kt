package com.example.android_echo_music.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android_echo_music.common.TimeFormat
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.databinding.ItemSongBinding

/** 歌曲列表适配器：标题 / 艺术家 / 时长 / 封面。 */
class SongAdapter(
    private val onSongClick: (Song) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SongViewHolder(
        private val binding: ItemSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            binding.titleText.text = song.title
            binding.artistText.text = binding.root.context.getString(
                com.example.android_echo_music.R.string.song_artist_album,
                song.artist,
                song.album
            )
            binding.durationText.text = TimeFormat.formatDuration(song.durationMs)
            binding.root.setOnClickListener { onSongClick(song) }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
                oldItem == newItem
        }
    }
}