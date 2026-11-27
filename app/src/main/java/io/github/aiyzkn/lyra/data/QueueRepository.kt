package io.github.aiyzkn.lyra.data

import io.github.aiyzkn.lyra.data.db.QueueDao
import javax.inject.Inject
import javax.inject.Singleton

/** 再生待ちの保存と復元。再生のサービスが使う。 */
@Singleton
class QueueRepository @Inject constructor(
    private val dao: QueueDao,
    private val musicLibrary: MusicLibrary,
) {
    suspend fun save(songIds: List<Long>, currentIndex: Int, positionMs: Long) =
        dao.replace(songIds, currentIndex, positionMs)

    /** 残しておいた再生待ちを読む。無い・権限が無くて曲を読めないときは null。 */
    suspend fun load(): RestoredQueue? {
        val ids = dao.songIds()
        if (ids.isEmpty()) return null
        val state = dao.state()
        val songs = runCatching { musicLibrary.songs() }.getOrNull() ?: return null
        return restoreQueue(ids, state?.currentIndex ?: 0, state?.positionMs ?: 0L, songs.associateBy { it.id })
    }
}
