package io.github.aiyzkn.lyra.data

/** 残しておいた再生待ちを、端末の曲に置き換えたもの。 */
data class RestoredQueue(
    val songs: List<Song>,
    val currentIndex: Int,
    val positionMs: Long,
)

/**
 * 残しておいた再生待ち（曲 ID の並び・聴いていた位置）を、いまの端末の曲に置き換える。
 * 端末から消えた曲は飛ばし、聴いていた曲の位置をそれに合わせてずらす。聴いていた曲が消えていたら、
 * 次の曲の頭から（後ろが無ければ最後の曲の頭から）。1 曲も残らなければ null。
 */
fun restoreQueue(savedIds: List<Long>, savedIndex: Int, savedPositionMs: Long, songsById: Map<Long, Song>): RestoredQueue? {
    val songs = mutableListOf<Song>()
    var index = -1
    var keepPosition = false
    savedIds.forEachIndexed { i, id ->
        val song = songsById[id]
        if (i == savedIndex) {
            index = songs.size
            keepPosition = song != null
        }
        if (song != null) songs += song
    }
    if (songs.isEmpty()) return null
    if (index !in songs.indices) {
        // 聴いていた位置が並びの外（壊れた値）なら前後の端へ。聴いていた曲が末尾で消えていたら最後の曲へ
        index = if (savedIndex < 0) 0 else songs.lastIndex
        keepPosition = false
    }
    return RestoredQueue(songs, index, if (keepPosition) savedPositionMs.coerceAtLeast(0) else 0L)
}

/**
 * 並べ替えで、[from] 番目の行を [offsetPx] だけ動かして離したときの行き先。
 * 行の高さ [itemHeightPx] の半分を越えるごとに 1 つずれる。一覧の外には出さない。
 */
fun dragTarget(from: Int, offsetPx: Float, itemHeightPx: Float, size: Int): Int {
    if (size <= 0) return 0
    if (itemHeightPx <= 0f) return from.coerceIn(0, size - 1)
    val steps = Math.round(offsetPx / itemHeightPx)
    return (from + steps).coerceIn(0, size - 1)
}
