package com.nkds.hosikoouma.jasmine.core.di

import android.content.Context
import androidx.room.Room
import com.nkds.hosikoouma.jasmine.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePlaylistDatabase(@ApplicationContext context: Context): PlaylistDatabase {
        return Room.databaseBuilder(
            context,
            PlaylistDatabase::class.java,
            "playlist_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideLyricsDatabase(@ApplicationContext context: Context): LyricsDatabase {
        return Room.databaseBuilder(
            context,
            LyricsDatabase::class.java,
            "lyrics_database"
        ).build()
    }

    @Provides
    fun providePlaylistDao(db: PlaylistDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideRadioDao(db: PlaylistDatabase): RadioDao = db.radioDao()

    @Provides
    fun provideLyricsDao(db: LyricsDatabase): LyricsDao = db.lyricsDao()
}
