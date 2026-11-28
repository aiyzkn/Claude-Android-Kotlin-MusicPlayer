package io.github.aiyzkn.lyra.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 再生のサービスから、ウィジェットに状態を渡す口。いま再生している曲は小さな設定のファイルに残し
 * （ウィジェットはサービスが止まった後も描き直されるため）、変わったときだけ描き直す。
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // サービスが終わった後も、描き直しを最後まで行う
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var last: NowPlaying? = null

    fun publish(nowPlaying: NowPlaying) {
        if (nowPlaying == last) return
        last = nowPlaying
        NowPlayingStore.write(context, nowPlaying)
        scope.launch { runCatching { NowPlayingWidget().updateAll(context) } }
    }

    /** 再生の履歴が増えたとき。 */
    fun refreshRecent() {
        scope.launch { runCatching { RecentWidget().updateAll(context) } }
    }
}

/** いま再生している曲の保存先。ウィジェットの描画（別の処理の流れ）からも読む。 */
internal object NowPlayingStore {
    private const val FILE = "widget_now_playing"
    private const val TITLE = "title"
    private const val ARTIST = "artist"
    private const val PLAYING = "playing"

    fun write(context: Context, value: NowPlaying) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putString(TITLE, value.title)
            putString(ARTIST, value.artist)
            putBoolean(PLAYING, value.isPlaying)
        }
    }

    /**
     * 保存先の変化を流す。ウィジェットの描画のセッション（しばらく続く）の間に曲が替わっても、
     * 描き直しに反映させるため。
     */
    fun flow(context: Context): Flow<NowPlaying> = callbackFlow {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        // 設定のファイルは聞き手を弱い参照で持つので、流れが続く間はここで強く持つ
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read(context)) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send(read(context))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    fun read(context: Context): NowPlaying {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return nowPlayingOf(prefs.getString(TITLE, null), prefs.getString(ARTIST, null), prefs.getBoolean(PLAYING, false))
    }
}
