package io.github.aiyzkn.lyra.data

import io.github.aiyzkn.lyra.data.db.FavoriteEntity
import io.github.aiyzkn.lyra.data.db.PlayEntity
import io.github.aiyzkn.lyra.data.db.UserLibraryDao
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** お気に入り・再生の履歴・プレイリストの読み書きの口。画面と再生のサービスの両方から使う。 */
@Singleton
class UserLibraryRepository @Inject constructor(
    private val dao: UserLibraryDao,
) {
    val favorites: Flow<List<Long>> = dao.favorites()
    val recentlyPlayed: Flow<List<Long>> = dao.recentlyPlayed(RECENTLY_PLAYED_LIMIT)
    val playlists: Flow<List<PlaylistSummary>> = dao.playlists()

    fun playlistSongs(id: Long): Flow<List<Long>> = dao.playlistSongs(id)

    suspend fun toggleFavorite(songId: Long) {
        if (dao.isFavorite(songId)) dao.removeFavorite(songId) else dao.addFavorite(FavoriteEntity(songId, now()))
    }

    /** 曲が鳴り始めたら呼ぶ。あわせて、古い履歴を消す。 */
    suspend fun recordPlayed(songId: Long) {
        val now = now()
        dao.addPlay(PlayEntity(songId = songId, playedAt = now))
        dao.deletePlaysBefore(now - TimeUnit.DAYS.toMillis(PLAY_HISTORY_DAYS.toLong()))
    }

    /** 名前が空なら何もせず null。 */
    suspend fun createPlaylist(name: String, songIds: List<Long> = emptyList()): Long? {
        val normalized = normalizePlaylistName(name) ?: return null
        return dao.createPlaylist(normalized, songIds, now())
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        val normalized = normalizePlaylistName(name) ?: return
        dao.renamePlaylist(id, normalized)
    }

    suspend fun deletePlaylist(id: Long) = dao.deletePlaylist(id)

    suspend fun addToPlaylist(id: Long, songIds: List<Long>) = dao.addToPlaylist(id, songIds)

    suspend fun removeFromPlaylist(id: Long, position: Int) = dao.removeFromPlaylist(id, position)

    private fun now() = System.currentTimeMillis()
}
