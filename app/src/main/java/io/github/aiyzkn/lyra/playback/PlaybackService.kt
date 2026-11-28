package io.github.aiyzkn.lyra.playback

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import io.github.aiyzkn.lyra.data.QueueRepository
import io.github.aiyzkn.lyra.data.UserLibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * 再生を受け持つサービス。通知・ロック画面・Bluetooth やイヤホンのボタンは、
 * Media3 の `MediaSessionService` が標準で扱う。曲が鳴り始めたら、再生の履歴に残す。
 * 再生待ちは、変わるたび・止めるたびに残し、次にサービスが起きたとき（一時停止のまま）戻す。
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var userLibrary: UserLibraryRepository
    @Inject lateinit var queue: QueueRepository
    @Inject lateinit var audioSession: AudioSession

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 履歴に残した曲（同じ曲を一時停止・再開のたびに数えないため）。曲が替わったら空に戻す。 */
    private var recordedMediaId: String? = null

    /** 残しておいた再生待ちを戻し終えたか。戻す前に空の再生待ちで上書きしないため。 */
    private var restored = false

    /** 保存を呼んだ順に書く（続けて変わったとき、古い並びが後から書かれないように）。 */
    private val saveLock = Mutex()

    private val historyListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // 1 曲のリピートで頭に戻ったときも、ここを通るので 1 回と数える
            recordedMediaId = null
            session?.player?.let {
                recordIfPlaying(it)
                saveQueue(it)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            session?.player?.let {
                recordIfPlaying(it)
                // 止めたときに、聴いていた位置を残す
                if (!isPlaying) saveQueue(it)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            // 曲の追加・並べ替え・削除
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) session?.player?.let(::saveQueue)
        }
    }

    private fun saveQueue(player: Player) {
        if (!restored) return
        val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        val index = player.currentMediaItemIndex
        val position = player.currentPosition.coerceAtLeast(0)
        scope.launch { saveLock.withLock { queue.save(ids, index, position) } }
    }

    private fun restoreQueue(player: Player) {
        scope.launch {
            val saved = runCatching { queue.load() }.getOrNull()
            // 戻している間に、画面から曲が選ばれていたら、そちらを優先する
            if (saved != null && player.mediaItemCount == 0) {
                player.setMediaItems(saved.songs.map { it.toMediaItem() }, saved.currentIndex, saved.positionMs)
                player.prepare()
            }
            restored = true
        }
    }

    private fun recordIfPlaying(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        if (!player.isPlaying || mediaId == recordedMediaId) return
        recordedMediaId = mediaId
        val songId = mediaId.toLongOrNull() ?: return
        scope.launch { userLibrary.recordPlayed(songId) }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // イヤホンが抜けたら止める
            .setHandleAudioBecomingNoisy(true)
            .build()
        // 画面から開くイコライザーと同じセッションにする
        if (audioSession.id != NO_AUDIO_SESSION) useAudioSession(player, audioSession.id)
        audioSession.announceOpen()
        player.addListener(historyListener)
        session = MediaSession.Builder(this, player).build()
        restoreQueue(player)
    }

    // セッションの ID の指定は Media3 では不安定 API の扱い（ExoPlayer の公開のメソッドだが、注釈が付いている）
    @OptIn(UnstableApi::class)
    private fun useAudioSession(player: ExoPlayer, id: Int) {
        player.setAudioSessionId(id)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** 一時停止中にアプリが閉じられたら、サービスも終える（再生中なら続ける）。 */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = session?.player
        player?.let(::saveQueue)
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        audioSession.announceClose()
        session?.run {
            player.removeListener(historyListener)
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
