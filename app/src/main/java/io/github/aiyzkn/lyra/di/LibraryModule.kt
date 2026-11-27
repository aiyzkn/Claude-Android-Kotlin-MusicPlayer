package io.github.aiyzkn.lyra.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.aiyzkn.lyra.data.MediaStoreMusicLibrary
import io.github.aiyzkn.lyra.data.MusicLibrary

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {
    @Binds
    abstract fun bindMusicLibrary(impl: MediaStoreMusicLibrary): MusicLibrary
}
