package io.github.aiyzkn.lyra.data

import java.text.Normalizer
import java.util.Locale

/** アルバムの曲。トラック番号の順（同じ番号・番号なしは曲名の順）。 */
fun Library.songsOfAlbum(albumId: Long): List<Song> =
    songs.filter { it.albumId == albumId }
        .sortedWith(compareBy<Song> { it.trackNumber }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })

/** アーティストの曲。アルバムの名前の順 → トラック番号の順。 */
fun Library.songsOfArtist(name: String): List<Song> =
    songs.filter { it.artist == name }
        .sortedWith(
            compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy { it.albumId }
                .thenBy { it.trackNumber }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )

/**
 * アーティストが参加しているアルバム。コンピレーションも含める（曲数はアルバム全体の数）。
 * 並びはアルバムの一覧（名前の順）のまま。
 */
fun Library.albumsOfArtist(name: String): List<Album> {
    val ids = songs.asSequence().filter { it.artist == name }.map { it.albumId }.toSet()
    return albums.filter { it.id in ids }
}

/** ジャンルの曲（前後の空白は無視して比べる）。曲名の順。 */
fun Library.songsOfGenre(name: String): List<Song> {
    val key = name.trim()
    return songs.filter { it.genre?.trim() == key }
}

/** 横断検索の結果。 */
data class SearchResult(
    val artists: List<Artist>,
    val albums: List<Album>,
    val songs: List<Song>,
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && songs.isEmpty()

    companion object {
        val EMPTY = SearchResult(emptyList(), emptyList(), emptyList())
    }
}

/**
 * 曲・アーティスト・アルバムを横断して探す。空白で区切った語が、すべて含まれるものを返す。
 * 大文字・小文字と、全角・半角（NFKC）の違いは無視する。
 * - アーティスト: 名前
 * - アルバム: 名前・アーティスト
 * - 曲: 曲名・アーティスト・アルバム
 */
fun Library.search(query: String): SearchResult {
    val terms = normalizeForSearch(query).split(' ').filter { it.isNotEmpty() }
    if (terms.isEmpty()) return SearchResult.EMPTY
    fun matches(vararg fields: String): Boolean {
        val text = fields.joinToString(" ") { normalizeForSearch(it) }
        return terms.all { it in text }
    }
    return SearchResult(
        artists = artists.filter { matches(it.name) },
        albums = albums.filter { matches(it.title, it.artist) },
        songs = songs.filter { matches(it.title, it.artist, it.album) },
    )
}

/** 検索で比べるための形。NFKC で全角・半角をそろえ、小文字にし、空白を 1 つにする。 */
internal fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .trim()
