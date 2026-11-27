package io.github.aiyzkn.lyra.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aiyzkn.lyra.data.Song
import io.github.aiyzkn.lyra.playback.PlaybackService
import io.github.aiyzkn.lyra.playback.toMediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 画面に出す再生の状態。何も読み込んでいないときは [title] が null。 */
data class PlayerUiState(
    /** いまの曲の `MediaStore` の ID。お気に入りの切り替えに使う。 */
    val songId: Long? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
) {
    val hasMedia: Boolean get() = title != null

    /** シークバーの位置（0〜1）。長さが分からないときは 0。 */
    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** 再生待ちの 1 曲。同じ曲が何度も入りうるので、行の鍵は位置と組にする。 */
data class QueueEntry(
    val songId: Long?,
    val title: String,
    val artist: String?,
)

/** 再生待ちの並びと、いま鳴っている曲の位置（無ければ -1）。 */
data class QueueUiState(
    val items: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
)

/** リピートの切り替えの順（Apollo と同じく、なし → 全曲 → 1 曲）。 */
enum class RepeatMode(val media3: Int) {
    Off(Player.REPEAT_MODE_OFF),
    All(Player.REPEAT_MODE_ALL),
    One(Player.REPEAT_MODE_ONE),
    ;

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun from(media3: Int): RepeatMode = entries.firstOrNull { it.media3 == media3 } ?: Off
    }
}

/**
 * [PlaybackService] に `MediaController` でつなぎ、画面からの操作を渡す。
 * 再生そのものはサービスが持つので、画面を閉じても音は止まらない。
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _queue = MutableStateFlow(QueueUiState())
    val queue: StateFlow<QueueUiState> = _queue.asStateFlow()

    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                publishQueue(player)
            }
            if (player.isPlaying) startProgressUpdates() else progressJob?.cancel()
        }
    }

    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
    ).buildAsync().also { future ->
        future.addListener(
            {
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(listener)
                publish(c)
                publishQueue(c)
                if (c.isPlaying) startProgressUpdates()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    /** 一覧の [songs] を再生待ちにして、[index] 番目から再生する。 */
    fun play(songs: List<Song>, index: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.setMediaItems(songs.map(Song::toMediaItem), index.coerceIn(songs.indices), 0L)
        c.prepare()
        c.play()
    }

    /** いまの曲の次に入れる。何も読み込んでいなければ、その曲から再生する。 */
    fun playNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return play(listOf(song), 0)
        c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
    }

    /** 再生待ちの末尾に足す。何も読み込んでいなければ、その曲から再生する。 */
    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return play(listOf(song), 0)
        c.addMediaItem(song.toMediaItem())
    }

    /** 再生待ちの [index] 番目の曲へ飛んで再生する。 */
    fun playQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0L)
        c.play()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        val count = c.mediaItemCount
        if (from == to || from !in 0 until count || to !in 0 until count) return
        c.moveMediaItem(from, to)
    }

    /** 再生待ちから外す。鳴っている曲を外したら、次の曲へ進む（Media3 の既定の動き）。 */
    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.removeMediaItem(index)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNextMediaItem()

    /** 3 秒を過ぎていれば曲の頭へ、そうでなければ前の曲へ（`seekToPrevious` の既定の動き）。 */
    fun previous() = controller?.seekToPrevious()

    fun seekTo(fraction: Float) {
        val c = controller ?: return
        val duration = c.duration.takeIf { it > 0 } ?: return
        c.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
        publish(c)
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = RepeatMode.from(c.repeatMode).next().media3
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = viewModelScope.launch {
            while (isActive) {
                controller?.let(::publish)
                delay(500)
            }
        }
    }

    private fun publish(player: Player) {
        val item: MediaItem? = player.currentMediaItem
        val meta = item?.mediaMetadata
        _state.value = PlayerUiState(
            songId = item?.mediaId?.toLongOrNull(),
            title = meta?.title?.toString() ?: item?.let { "" },
            artist = meta?.artist?.toString(),
            album = meta?.albumTitle?.toString(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: 0,
            hasPrevious = item != null,
            hasNext = player.hasNextMediaItem(),
            shuffle = player.shuffleModeEnabled,
            repeat = RepeatMode.from(player.repeatMode),
        )
    }

    private fun publishQueue(player: Player) {
        _queue.value = QueueUiState(
            items = (0 until player.mediaItemCount).map { i ->
                val item = player.getMediaItemAt(i)
                QueueEntry(
                    songId = item.mediaId.toLongOrNull(),
                    title = item.mediaMetadata.title?.toString().orEmpty(),
                    artist = item.mediaMetadata.artist?.toString(),
                )
            },
            currentIndex = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex,
        )
    }

    override fun onCleared() {
        progressJob?.cancel()
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }
}
