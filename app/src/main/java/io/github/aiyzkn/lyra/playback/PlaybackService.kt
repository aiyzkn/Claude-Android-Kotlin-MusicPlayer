package io.github.aiyzkn.lyra.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import io.github.aiyzkn.lyra.data.UserLibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 再生を受け持つサービス。通知・ロック画面・Bluetooth やイヤホンのボタンは、
 * Media3 の `MediaSessionService` が標準で扱う。曲が鳴り始めたら、再生の履歴に残す。
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var userLibrary: UserLibraryRepository

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 履歴に残した曲（同じ曲を一時停止・再開のたびに数えないため）。曲が替わったら空に戻す。 */
    private var recordedMediaId: String? = null

    private val historyListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // 1 曲のリピートで頭に戻ったときも、ここを通るので 1 回と数える
            recordedMediaId = null
            session?.player?.let(::recordIfPlaying)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            session?.player?.let(::recordIfPlaying)
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
        player.addListener(historyListener)
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** 一時停止中にアプリが閉じられたら、サービスも終える（再生中なら続ける）。 */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        session?.run {
            player.removeListener(historyListener)
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
