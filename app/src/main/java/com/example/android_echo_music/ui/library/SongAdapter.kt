package com.example.android_echo_music.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android_echo_music.R
import com.example.android_echo_music.common.TimeFormat
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.databinding.ItemSongBinding
import com.example.android_echo_music.ui.common.loadCoverArt

/**
 * 歌曲列表适配器：标题 / 艺术家 / 时长 / 封面。
 *
 * [onSongLongClick] 供「添加到歌单」等长按操作使用，不传则只响应点击。
 */
class SongAdapter(
    private val onSongClick: (Song) -> Unit,
    private val onSongLongClick: ((Song) -> Unit)? = null
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
                R.string.song_artist_album,
                song.artist,
                song.album
            )
            binding.durationText.text = TimeFormat.formatDuration(song.durationMs)

            // 有专辑封面就显示封面，否则保留音符占位图标
            binding.coverArt.loadCoverArt(song.albumArtUri)
            binding.coverNote.isVisible = song.albumArtUri.isNullOrBlank()

            binding.root.setOnClickListener { onSongClick(song) }
            binding.root.setOnLongClickListener {
                onSongLongClick?.invoke(song)
                onSongLongClick != null
            }
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