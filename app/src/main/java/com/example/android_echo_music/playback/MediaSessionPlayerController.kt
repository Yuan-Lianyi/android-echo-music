package com.example.android_echo_music.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.playback.PlaybackCommands.applyTo
import com.example.android_echo_music.playback.PlaybackCommands.readMode
import com.example.android_echo_music.playback.queue.PlayQueue
import com.example.android_echo_music.playback.queue.QueueSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [PlayerController] 的 Media3 实现（[A] 维护）。
 *
 * 通过 [MediaController] 连接 [PlaybackService]，把 Player 的回调翻译成
 * [PlaybackState] / [QueueSnapshot] 两个 StateFlow，UI 不感知 Media3。
 *
 * 队列的两套结构分工明确：
 * - **Media3 Player**：真正决定播放顺序（含随机序、循环）；
 * - **[PlayQueue]**：UI 侧看到的「原始列表 + 当前下标」，负责下一首播放 / 移除与上一首下一首计算。
 * 每次 Player 的媒体项变化都会把 [PlayQueue] 校正回 Player 的真实状态。
 */
class MediaSessionPlayerController(context: Context) : PlayerController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _queue = MutableStateFlow(QueueSnapshot())
    override val queue: StateFlow<QueueSnapshot> = _queue.asStateFlow()

    private var controller: MediaController? = null
    private val playQueue = PlayQueue()

    private var progressJob: Job? = null

    /** 连接完成前收到的播放请求，连接成功后补发，避免首次点击丢事件。 */
    private var pendingPlay: Pair<Song, List<Song>>? = null

    /** 防止「同步队列 → Player 变化 → 再次同步」的回环。 */
    private var isSyncingToPlayer = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TIMELINE_CHANGED)) {
                syncQueueFromPlayer(player)
            }
            syncState(player)
        }
    }

    init {
        connect()
    }

    /** 异步连接播放服务；连接失败时 UI 保持空状态，不崩溃。 */
    @OptIn(UnstableApi::class)
    private fun connect() {
        val future = buildMediaController(appContext)
        future.addListener({
            val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = connected
            connected.addListener(listener)
            syncQueueFromPlayer(connected)
            syncState(connected)
            pendingPlay?.let { (song, songs) ->
                pendingPlay = null
                play(song, songs)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    // ---------------------------------------------------------------- 播放控制

    override fun play(song: Song, queue: List<Song>) {
        val player = controller
        if (player == null) {
            pendingPlay = song to queue
            return
        }
        // 点击的歌曲排到最前，其余保持列表顺序（本地播放器的常见行为）
        playQueue.startWith(song, queue)
        player.setMediaItems(
            playQueue.items.map { it.toMediaItem() },
            playQueue.indexOfCurrent,
            /* startPositionMs = */ 0L
        )
        player.prepare()
        player.play()
        publishQueue()
    }

    override fun toggle() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    /**
     * 下一首。
     *
     * 随机模式交给 Player 的内部随机序；顺序模式按原始列表下标跳转，
     * 避免 Player 的 `seekToNextMediaItem` 在单曲循环模式下原地打转。
     */
    override fun next() {
        val player = controller ?: return
        val index = playQueue.nextIndex()
        if (index >= 0) {
            player.seekToDefaultPosition(index)
        } else {
            player.seekToNextMediaItem()
        }
    }

    /**
     * 上一首。
     *
     * 与系统播放器一致：播放超过 3 秒时先回到本曲开头，否则切到上一首。
     */
    override fun previous() {
        val player = controller ?: return
        if (player.currentPosition > SEEK_TO_PREVIOUS_THRESHOLD_MS) {
            player.seekTo(0L)
            return
        }
        val index = playQueue.previousIndex()
        if (index >= 0) {
            player.seekToDefaultPosition(index)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    @OptIn(UnstableApi::class)
    override fun setMode(mode: PlayMode) {
        val player = controller ?: return
        mode.applyTo(player)
        playQueue.setShuffle(mode == PlayMode.SHUFFLE)
        publishQueue()
        syncState(player)
    }

    // ---------------------------------------------------------------- 播放队列

    override fun playNext(song: Song) {
        val player = controller ?: return
        if (playQueue.size == 0) {
            // 队列为空时「下一首播放」等价于立即播放
            play(song, listOf(song))
            return
        }
        playQueue.insertNext(song)
        isSyncingToPlayer = true
        try {
            player.setMediaItems(
                playQueue.items.map { it.toMediaItem() },
                playQueue.indexOfCurrent,
                player.currentPosition
            )
            player.prepare()
        } finally {
            isSyncingToPlayer = false
        }
        publishQueue()
    }

    override fun removeFromQueue(index: Int) {
        val player = controller ?: return
        val removingCurrent = index == playQueue.indexOfCurrent
        playQueue.removeAt(index) ?: return
        if (playQueue.size == 0) {
            player.clearMediaItems()
            player.stop()
            publishQueue()
            return
        }
        isSyncingToPlayer = true
        try {
            player.setMediaItems(
                playQueue.items.map { it.toMediaItem() },
                playQueue.indexOfCurrent,
                if (removingCurrent) 0L else player.currentPosition
            )
            player.prepare()
        } finally {
            isSyncingToPlayer = false
        }
        publishQueue()
    }

    override fun playAt(index: Int) {
        val player = controller ?: return
        if (!playQueue.moveTo(index)) return
        player.seekToDefaultPosition(playQueue.indexOfCurrent)
        player.play()
        publishQueue()
    }

    /** 页面销毁时释放控制器（当前由 AppContainer 持有，进程级生命周期，通常不需要调用）。 */
    fun release() {
        progressJob?.cancel()
        progressJob = null
        controller?.release()
        controller = null
    }

    // ---------------------------------------------------------------- 状态同步

    @OptIn(UnstableApi::class)
    private fun syncState(player: Player) {
        val index = player.currentMediaItemIndex
        val itemId = player.currentMediaItem?.mediaId?.toLongOrNull()
        val song = playQueue.items.firstOrNull { it.id == itemId } ?: _state.value.currentSong
        _state.update {
            it.copy(
                currentSong = song,
                queueIndex = playQueue.indexOfCurrent.takeIf { i -> i >= 0 } ?: index,
                queueSize = player.mediaItemCount,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { d -> d > 0L } ?: song?.durationMs ?: 0L,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                mode = readMode(player)
            )
        }
        tickProgress(player)
    }

    /** 播放中每 500ms 刷新一次进度，暂停即停止计时器。 */
    private fun tickProgress(player: Player) {
        if (!player.isPlaying) {
            progressJob?.cancel()
            progressJob = null
            return
        }
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (true) {
                _state.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0L)) }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    /** 把 PlayQueue 校正到 Player 的真实状态（随机序下按媒体项 id 找回当前位置）。 */
    @OptIn(UnstableApi::class)
    private fun syncQueueFromPlayer(player: Player) {
        if (isSyncingToPlayer) return
        val ids = (0 until player.mediaItemCount).mapNotNull {
            player.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        if (ids != playQueue.items.map { it.id }) {
            // Player 里有 PlayQueue 不认识的媒体项（例如外部广播），以 Player 为准重建
            playQueue.replaceAll(playQueue.items.filter { it.id in ids })
        }
        playQueue.moveToSong(player.currentMediaItem?.mediaId?.toLongOrNull())
        playQueue.setShuffle(player.shuffleModeEnabled)
        publishQueue()
    }

    private fun publishQueue() {
        _queue.value = playQueue.snapshot()
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                // 通知栏 / 锁屏 / 车载封面；Media3 默认 BitmapLoader 可直接加载 content:// 封面
                .setArtworkUri(albumArtUri?.toUriOrNull())
                .build()
        )
        .build()

    private fun String?.toUriOrNull(): android.net.Uri? =
        this?.let { runCatching { it.toUri() }.getOrNull() }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 500L
        const val SEEK_TO_PREVIOUS_THRESHOLD_MS = 3_000L
    }
}