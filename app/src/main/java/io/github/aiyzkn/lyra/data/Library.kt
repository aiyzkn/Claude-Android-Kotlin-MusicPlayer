package io.github.aiyzkn.lyra.data

import android.net.Uri

/** 端末の 1 曲。`MediaStore.Audio.Media` の 1 行に当たる。 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String?,
    val durationMs: Long,
    val trackNumber: Int,
    val uri: Uri?,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
)

data class Artist(
    val name: String,
    val albumCount: Int,
    val songCount: Int,
)

data class Genre(
    val name: String,
    val songCount: Int,
)

/** タブに出す一覧をまとめたもの。曲の一覧から組み立てる（[Library.from]）。 */
data class Library(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val genres: List<Genre>,
) {
    companion object {
        val EMPTY = Library(emptyList(), emptyList(), emptyList(), emptyList())

        fun from(songs: List<Song>): Library = Library(
            songs = songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }),
            albums = albumsOf(songs),
            artists = artistsOf(songs),
            genres = genresOf(songs),
        )
    }
}

internal fun albumsOf(songs: List<Song>): List<Album> =
    songs.groupBy { it.albumId }
        .map { (id, tracks) ->
            Album(
                id = id,
                title = tracks.first().album,
                // 曲ごとにアーティストが違うアルバム（コンピレーション）は、いちばん多い名前を出す
                artist = tracks.groupingBy { it.artist }.eachCount().maxBy { it.value }.key,
                songCount = tracks.size,
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

internal fun artistsOf(songs: List<Song>): List<Artist> =
    songs.groupBy { it.artist }
        .map { (name, tracks) ->
            Artist(
                name = name,
                albumCount = tracks.map { it.albumId }.distinct().size,
                songCount = tracks.size,
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

internal fun genresOf(songs: List<Song>): List<Genre> =
    songs.mapNotNull { song -> song.genre?.trim()?.takeIf { it.isNotEmpty() } }
        .groupingBy { it }
        .eachCount()
        .map { (name, count) -> Genre(name, count) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
