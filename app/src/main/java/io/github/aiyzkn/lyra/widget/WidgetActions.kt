package io.github.aiyzkn.lyra.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.aiyzkn.lyra.data.MusicLibrary
import io.github.aiyzkn.lyra.data.UserLibraryRepository
import io.github.aiyzkn.lyra.playback.PlaybackService
import io.github.aiyzkn.lyra.playback.toMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** ウィジェットから使う、アプリの中の口（ウィジェットは Hilt の注入の対象にならないため）。 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun userLibrary(): UserLibraryRepository
    fun musicLibrary(): MusicLibrary
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withPlayer(context) { player ->
            // サービスが起きたばかりなら、残しておいた再生待ちが戻るのを少し待つ
            waitForQueue(player)
            if (player.mediaItemCount == 0) return@withPlayer
            if (player.isPlaying) player.pause() else {
                player.prepare()
                player.play()
            }
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withPlayer(context) { player ->
            waitForQueue(player)
            if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        }
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // 再生画面の「前」と同じく、曲の途中なら頭に戻る
        withPlayer(context) { player ->
            waitForQueue(player)
            player.seekToPrevious()
        }
    }
}

/** 「最近再生」の曲を押したとき。最近再生の並びを再生待ちにして、その曲から鳴らす。 */
class PlayRecentAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val songId = parameters[SONG_ID] ?: return
        val entry = context.widgetEntryPoint()
        val songs = recentSongsForWidget(
            entry.userLibrary().recentlyPlayed.first(),
            runCatching { entry.musicLibrary().songs() }.getOrDefault(emptyList()).associateBy { it.id },
        )
        val index = songs.indexOfFirst { it.id == songId }.takeIf { it >= 0 } ?: return
        withPlayer(context) { player ->
            player.setMediaItems(songs.map { it.toMediaItem() }, index, 0L)
            player.prepare()
            player.play()
        }
    }

    companion object {
        val SONG_ID = ActionParameters.Key<Long>("songId")
    }
}

/**
 * 再生のサービスにつないで [block] を行い、切る。`MediaController` は作ったスレッドの
 * ルーパーで使う決まりなので、主スレッドで行う。ウィジェットの操作で渡る [context] は
 * ブロードキャストレシーバーのもので、サービスにつなげない（`ReceiverCallNotAllowedException`）ため、
 * アプリのコンテキストを使う。
 */
private suspend fun withPlayer(context: Context, block: suspend (MediaController) -> Unit) = withContext(Dispatchers.Main) {
    val app = context.applicationContext
    val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
    val controller = MediaController.Builder(app, token).buildAsync().await()
    try {
        block(controller)
        // 送った操作がサービスに届き、再生なら前面のサービスになるまで、つないだままにする
        delay(COMMAND_GRACE_MS)
    } finally {
        controller.release()
    }
}

private suspend fun waitForQueue(player: MediaController) {
    repeat(QUEUE_WAIT_STEPS) {
        if (player.mediaItemCount > 0) return
        delay(QUEUE_WAIT_STEP_MS)
    }
}

private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    addListener({
        try {
            cont.resume(get())
        } catch (e: ExecutionException) {
            cont.resumeWithException(e.cause ?: e)
        }
    }, Runnable::run)
    cont.invokeOnCancellation { cancel(false) }
}

private const val COMMAND_GRACE_MS = 500L
private const val QUEUE_WAIT_STEPS = 20
private const val QUEUE_WAIT_STEP_MS = 100L
