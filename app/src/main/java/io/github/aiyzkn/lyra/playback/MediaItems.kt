package io.github.aiyzkn.lyra.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.aiyzkn.lyra.data.Song

/** 曲を Media3 の再生の単位にする。ID は `MediaStore` の ID の文字列。 */
fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setGenre(genre)
            .setTrackNumber(trackNumber.takeIf { it > 0 })
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build(),
    )
    .build()
