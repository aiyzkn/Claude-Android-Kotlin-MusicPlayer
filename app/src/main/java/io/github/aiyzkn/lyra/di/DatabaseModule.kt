package io.github.aiyzkn.lyra.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.aiyzkn.lyra.data.db.LyraDatabase
import io.github.aiyzkn.lyra.data.db.UserLibraryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LyraDatabase =
        Room.databaseBuilder(context, LyraDatabase::class.java, "lyra.db").build()

    @Provides
    fun provideUserLibraryDao(db: LyraDatabase): UserLibraryDao = db.userLibraryDao()
}
