package io.github.aiyzkn.lyra.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class QueueDao {

    @Query("SELECT songId FROM queue_items ORDER BY position")
    abstract suspend fun songIds(): List<Long>

    @Query("SELECT * FROM queue_state WHERE id = 0")
    abstract suspend fun state(): QueueStateEntity?

    @Query("DELETE FROM queue_items")
    protected abstract suspend fun clearItems()

    @Insert
    protected abstract suspend fun insertItems(items: List<QueueItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveState(state: QueueStateEntity)

    /** 再生待ちを丸ごと置き換える。 */
    @Transaction
    open suspend fun replace(songIds: List<Long>, currentIndex: Int, positionMs: Long) {
        clearItems()
        insertItems(songIds.mapIndexed { i, id -> QueueItemEntity(position = i, songId = id) })
        saveState(QueueStateEntity(currentIndex = currentIndex, positionMs = positionMs))
    }
}
