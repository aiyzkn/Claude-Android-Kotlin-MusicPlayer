package io.github.aiyzkn.lyra.playback

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 再生に使うオーディオ セッションの ID。再生のサービスと画面（イコライザーの呼び出し）で同じ ID を使うため、
 * アプリで 1 つ作って両方に渡す。作れなければ [NO_AUDIO_SESSION]（プレーヤーが自分で作る）。
 */
@Singleton
class AudioSession @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val id: Int by lazy {
        val audioManager = context.getSystemService(AudioManager::class.java)
        audioManager?.generateAudioSessionId()?.takeIf { it > 0 } ?: NO_AUDIO_SESSION
    }

    /** 端末のイコライザー（音のエフェクト）のアプリに、このセッションで再生が始まることを知らせる。 */
    fun announceOpen() = announce(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)

    fun announceClose() = announce(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)

    private fun announce(action: String) {
        if (id == NO_AUDIO_SESSION) return
        context.sendBroadcast(effectIntent(action, id, context.packageName))
    }
}

/** セッションを作れなかったときの値（Media3 の `C.AUDIO_SESSION_ID_UNSET` と同じ 0。こちらは公開の API）。 */
const val NO_AUDIO_SESSION: Int = AudioManager.AUDIO_SESSION_ID_GENERATE

/** 音のエフェクトの画面・知らせに使う Intent。 */
internal fun effectIntent(action: String, sessionId: Int, packageName: String): Intent =
    Intent(action)
        .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)

/**
 * 端末のイコライザーの画面を開く Intent（Apollo と同じく、自前のイコライザーは持たず呼ぶだけ）。
 * `startActivityForResult` で開くこと。MusicFX は呼び出し元を `getCallingPackage()` で調べ、
 * 普通の `startActivity` では null になって、すぐ閉じる。
 */
fun equalizerIntent(context: Context, sessionId: Int): Intent =
    effectIntent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL, sessionId, context.packageName)
