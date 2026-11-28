package io.github.aiyzkn.lyra.widget

import io.github.aiyzkn.lyra.data.Song

/** ウィジェットに出す、いま再生している曲。曲が無ければ [title] が null。 */
data class NowPlaying(
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
) {
    val hasSong: Boolean get() = title != null

    companion object {
        val NONE = NowPlaying(title = null, artist = null, isPlaying = false)
    }
}

/** 再生の情報（Media3 のメタデータ）から、ウィジェットに出す形にする。空の文字は無いものとして扱う。 */
fun nowPlayingOf(title: CharSequence?, artist: CharSequence?, isPlaying: Boolean): NowPlaying {
    val t = title?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return NowPlaying.NONE
    val a = artist?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    return NowPlaying(t, a, isPlaying)
}

/** 「最近再生」のウィジェットに出す曲。履歴の ID の順のまま、端末から消えた曲を飛ばして [limit] 曲まで。 */
fun recentSongsForWidget(ids: List<Long>, byId: Map<Long, Song>, limit: Int = RECENT_WIDGET_LIMIT): List<Song> =
    ids.asSequence().mapNotNull { byId[it] }.distinctBy { it.id }.take(limit).toList()

const val RECENT_WIDGET_LIMIT = 10
