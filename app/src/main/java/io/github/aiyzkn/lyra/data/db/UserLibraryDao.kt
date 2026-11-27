package io.github.aiyzkn.lyra.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.aiyzkn.lyra.data.PlaylistSummary
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserLibraryDao {

    // お気に入り

    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    abstract fun favorites(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    abstract suspend fun removeFavorite(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    abstract suspend fun isFavorite(songId: Long): Boolean

    // 再生の履歴

    @Insert
    abstract suspend fun addPlay(play: PlayEntity)

    /** 曲ごとに、最後に鳴った時刻の新しい順。 */
    @Query("SELECT songId FROM play_history GROUP BY songId ORDER BY MAX(playedAt) DESC LIMIT :limit")
    abstract fun recentlyPlayed(limit: Int): Flow<List<Long>>

    @Query("DELETE FROM play_history WHERE playedAt < :before")
    abstract suspend fun deletePlaysBefore(before: Long)

    // プレイリスト

    @Query(
        """
        SELECT p.id AS id, p.name AS name, COUNT(s.id) AS songCount
        FROM playlists p LEFT JOIN playlist_songs s ON s.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE, p.id
        """,
    )
    abstract fun playlists(): Flow<List<PlaylistSummary>>

    @Insert
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    protected abstract suspend fun deletePlaylistRow(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :id")
    protected abstract suspend fun deletePlaylistSongs(id: Long)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :id ORDER BY position")
    abstract fun playlistSongs(id: Long): Flow<List<Long>>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM playlist_songs WHERE playlistId = :id")
    protected abstract suspend fun nextPosition(id: Long): Int

    @Insert
    protected abstract suspend fun insertPlaylistSongs(songs: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :id AND position = :position")
    protected abstract suspend fun deleteAt(id: Long, position: Int)

    @Query("UPDATE playlist_songs SET position = position - 1 WHERE playlistId = :id AND position > :position")
    protected abstract suspend fun shiftAfter(id: Long, position: Int)

    @Transaction
    open suspend fun deletePlaylist(id: Long) {
        deletePlaylistSongs(id)
        deletePlaylistRow(id)
    }

    /** 末尾に足す。 */
    @Transaction
    open suspend fun addToPlaylist(id: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        val start = nextPosition(id)
        insertPlaylistSongs(
            songIds.mapIndexed { i, songId -> PlaylistSongEntity(playlistId = id, position = start + i, songId = songId) },
        )
    }

    /** [position] の 1 曲を外し、後ろを詰める。 */
    @Transaction
    open suspend fun removeFromPlaylist(id: Long, position: Int) {
        deleteAt(id, position)
        shiftAfter(id, position)
    }

    @Transaction
    open suspend fun createPlaylist(name: String, songIds: List<Long>, now: Long): Long {
        val id = insertPlaylist(PlaylistEntity(name = name, createdAt = now))
        addToPlaylist(id, songIds)
        return id
    }
}
