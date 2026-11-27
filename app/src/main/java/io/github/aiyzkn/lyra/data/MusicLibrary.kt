package io.github.aiyzkn.lyra.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 端末の曲を読む口。テストでは差し替える。 */
interface MusicLibrary {
    suspend fun songs(): List<Song>
}

/** `MediaStore` から曲を読む。権限（[audioPermission]）が無いと空になる。 */
@Singleton
class MediaStoreMusicLibrary @Inject constructor(
    @ApplicationContext private val context: Context,
) : MusicLibrary {

    override suspend fun songs(): List<Song> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val genres = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) emptyMap() else legacyGenres(resolver)
        querySongs(resolver, genres)
    }

    private fun querySongs(resolver: ContentResolver, legacyGenres: Map<Long, String>): List<Song> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.TRACK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(MediaStore.Audio.Media.GENRE)
        }.toTypedArray()

        val songs = mutableListOf<Song>()
        resolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val duration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val track = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val genre = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            } else {
                -1
            }
            while (c.moveToNext()) {
                val songId = c.getLong(id)
                songs += Song(
                    id = songId,
                    title = c.getString(title).orUnknown(),
                    artist = c.getString(artist).orUnknownArtist(),
                    album = c.getString(album).orUnknown(),
                    albumId = c.getLong(albumId),
                    genre = if (genre >= 0) c.getString(genre) else legacyGenres[songId],
                    durationMs = c.getLong(duration),
                    // TRACK はディスクの番号を千の位に持つ（1001 = 1 枚目の 1 曲目）
                    trackNumber = c.getInt(track) % 1000,
                    uri = ContentUris.withAppendedId(collection, songId),
                )
            }
        }
        return songs
    }

    /** Android 10 以下は曲の行にジャンルが無いので、ジャンルの表から曲 ID → ジャンルの対応を作る。 */
    @Suppress("DEPRECATION")
    private fun legacyGenres(resolver: ContentResolver): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        resolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null,
            null,
            null,
        )?.use { g ->
            while (g.moveToNext()) {
                val genreId = g.getLong(0)
                val name = g.getString(1) ?: continue
                resolver.query(
                    MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                    arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                    null,
                    null,
                    null,
                )?.use { m ->
                    while (m.moveToNext()) result.putIfAbsent(m.getLong(0), name)
                }
            }
        }
        return result
    }

    private fun String?.orUnknown() = if (isNullOrBlank()) UNKNOWN else this

    private fun String?.orUnknownArtist() =
        if (isNullOrBlank() || this == MediaStore.UNKNOWN_STRING) UNKNOWN else this

    companion object {
        const val UNKNOWN = "<不明>"
    }
}
