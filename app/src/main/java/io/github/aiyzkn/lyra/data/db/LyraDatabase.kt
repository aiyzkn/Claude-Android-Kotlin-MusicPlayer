package io.github.aiyzkn.lyra.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** 利用者が付けたもの（お気に入り・再生の履歴・プレイリスト）を残す。曲そのものは `MediaStore` が持つ。 */
@Database(
    entities = [FavoriteEntity::class, PlayEntity::class, PlaylistEntity::class, PlaylistSongEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LyraDatabase : RoomDatabase() {
    abstract fun userLibraryDao(): UserLibraryDao
}
