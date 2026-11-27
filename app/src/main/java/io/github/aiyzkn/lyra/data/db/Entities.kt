package io.github.aiyzkn.lyra.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** お気に入りの曲。曲は `MediaStore` の ID で指す（端末から消えた曲は表示のときに飛ばす）。 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long,
)

/** 再生の履歴。曲が鳴り始めるたびに 1 行。 */
@Entity(tableName = "play_history", indices = [Index("songId"), Index("playedAt")])
data class PlayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** プレイリストの中の 1 曲。同じ曲を何度入れてもよいので、位置（0 から）で並べる。 */
@Entity(tableName = "playlist_songs", indices = [Index(value = ["playlistId", "position"])])
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val position: Int,
    val songId: Long,
)

/** 再生待ちの 1 曲（アプリを閉じても続きから聴けるように残す）。位置（0 から）で並べる。 */
@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey val position: Int,
    val songId: Long,
)

/** 再生待ちのどこまで聴いたか。1 行だけ（[id] は常に 0）。 */
@Entity(tableName = "queue_state")
data class QueueStateEntity(
    @PrimaryKey val id: Int = 0,
    val currentIndex: Int,
    val positionMs: Long,
)
