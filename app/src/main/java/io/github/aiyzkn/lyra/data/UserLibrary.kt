package io.github.aiyzkn.lyra.data

import java.util.concurrent.TimeUnit

/** プレイリストのタブに並べるもの。自動のもの（お気に入り・最近追加）と、利用者が作ったもの。 */
sealed interface PlaylistRef {
    data object Favorites : PlaylistRef
    data object RecentlyAdded : PlaylistRef
    data class User(val id: Long, val name: String) : PlaylistRef
}

/** 利用者が作ったプレイリストの一覧の 1 行。 */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val songCount: Int,
)

/** 「最近追加」に入れる期間（Apollo と同じく 4 週間・推測）。 */
const val RECENTLY_ADDED_DAYS = 28

/** 「最近再生」に出す曲の数。 */
const val RECENTLY_PLAYED_LIMIT = 100

/** 再生の履歴を残す期間。これより古いものは消す。 */
const val PLAY_HISTORY_DAYS = 90

/** プレイリストの名前の長さの上限。 */
const val PLAYLIST_NAME_MAX = 100

/** [nowSec] から [days] 日の間に加わった曲を、新しい順に返す。 */
fun recentlyAdded(songs: List<Song>, nowSec: Long, days: Int = RECENTLY_ADDED_DAYS): List<Song> {
    val since = nowSec - TimeUnit.DAYS.toSeconds(days.toLong())
    return songs.filter { it.dateAddedSec in since..nowSec }
        .sortedWith(compareByDescending<Song> { it.dateAddedSec }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
}

/** 曲の ID の並びを、端末にある曲に置き換える。端末から消えた曲は飛ばす（並びは保つ）。 */
fun resolveSongs(ids: List<Long>, byId: Map<Long, Song>): List<Song> = ids.mapNotNull(byId::get)

/** 入力されたプレイリストの名前を整える。空なら null。長すぎれば切り詰める。 */
fun normalizePlaylistName(raw: String): String? =
    raw.trim().replace(Regex("\\s+"), " ").take(PLAYLIST_NAME_MAX).takeIf { it.isNotEmpty() }
