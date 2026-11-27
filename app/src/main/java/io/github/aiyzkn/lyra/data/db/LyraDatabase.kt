package io.github.aiyzkn.lyra.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 利用者が付けたもの（お気に入り・再生の履歴・プレイリスト）と、再生待ちを残す。曲そのものは `MediaStore` が持つ。
 * 版 2 で再生待ちの表を足した（表を足すだけなので自動の移行）。
 */
@Database(
    entities = [
        FavoriteEntity::class, PlayEntity::class, PlaylistEntity::class, PlaylistSongEntity::class,
        QueueItemEntity::class, QueueStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class LyraDatabase : RoomDatabase() {
    abstract fun userLibraryDao(): UserLibraryDao
    abstract fun queueDao(): QueueDao
}
