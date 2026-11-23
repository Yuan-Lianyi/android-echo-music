package com.example.android_echo_music.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.android_echo_music.data.model.Song
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
 * [PlaybackState] 的 StateFlow，UI 不感知 Media3。
 */
class MediaSessionPlayerController(context: Context) : PlayerController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var queue: List<Song> = emptyList()
    private var progressJob: Job? = null

    /** 连接完成前收到的播放请求，连接成功后补发，避免首次点击丢事件。 */
    private var pendingPlay: Pair<Song, List<Song>>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState(player)
        }
    }

    init {
        connect()
    }

    /** 异步连接播放服务；连接失败时 UI 保持空状态，不崩溃。 */
    private fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            val connected = runCatching { future.get() }.getOrNull()
            if (connected == null) return@addListener
            controller = connected
            connected.addListener(listener)
            syncState(connected)
            pendingPlay?.let { (song, songs) ->
                pendingPlay = null
                play(song, songs)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    override fun play(song: Song, queue: List<Song>) {
        val player = controller
        if (player == null) {
            pendingPlay = song to queue
            return
        }
        this.queue = queue
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        player.setMediaItems(queue.map { it.toMediaItem() }, index, 0L)
        player.prepare()
        player.play()
    }

    override fun toggle() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun next() {
        controller?.seekToNextMediaItem()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun setMode(mode: PlayMode) {
        val player = controller ?: return
        player.shuffleModeEnabled = mode == PlayMode.SHUFFLE
        player.repeatMode = when (mode) {
            PlayMode.SEQUENTIAL, PlayMode.SHUFFLE -> Player.REPEAT_MODE_ALL
            PlayMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        }
    }

    /** 页面销毁时释放控制器（当前由 AppContainer 持有，进程级生命周期，通常不需要调用）。 */
    fun release() {
        progressJob?.cancel()
        progressJob = null
        controller?.release()
        controller = null
    }

    private fun syncState(player: Player) {
        val index = player.currentMediaItemIndex
        val song = this.queue.getOrNull(index) ?: _state.value.currentSong
        _state.update {
            it.copy(
                currentSong = song,
                queueIndex = index,
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

    private fun readMode(player: Player): PlayMode = when {
        player.shuffleModeEnabled -> PlayMode.SHUFFLE
        player.repeatMode == Player.REPEAT_MODE_ONE -> PlayMode.REPEAT_ONE
        else -> PlayMode.SEQUENTIAL
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()

    private companion object {
        const val PROGRESS_INTERVAL_MS = 500L
    }
}