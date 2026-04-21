package com.nkds.hosikoouma.jasmine.core.di

import android.content.Context
import com.nkds.hosikoouma.jasmine.TrackScanner
import com.nkds.hosikoouma.jasmine.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideTrackScanner(@ApplicationContext context: Context): TrackScanner {
        return TrackScanner(context)
    }

    @Provides
    @Singleton
    fun provideFavoritesRepository(@ApplicationContext context: Context): FavoritesRepository {
        return FavoritesRepository(context)
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(
        @ApplicationContext context: Context,
        playlistDao: PlaylistDao,
        m3uManager: M3UManager
    ): PlaylistRepository {
        return PlaylistRepository(context, playlistDao, m3uManager)
    }

    @Provides
    @Singleton
    fun provideRadioRepository(radioDao: RadioDao): RadioRepository {
        return RadioRepository(radioDao)
    }

    @Provides
    @Singleton
    fun provideLyricsRepository(
        @ApplicationContext context: Context,
        lyricsDao: LyricsDao,
        lrcLibService: LrcLibService
    ): LyricsRepository {
        return LyricsRepository(context, lyricsDao, lrcLibService)
    }
}
