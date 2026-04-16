package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION = longPreferencesKey("crossfade_duration")
        val MIN_TRACK_DURATION = intPreferencesKey("min_track_duration")
        val DEFAULT_SORT_TYPE = stringPreferencesKey("default_sort_type")
        val DEFAULT_SORT_REVERSED = booleanPreferencesKey("default_sort_reversed")
        val PROGRESS_BAR_STYLE = stringPreferencesKey("progress_bar_style")
        val APP_FONT_FAMILY = stringPreferencesKey("app_font_family")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        
        val PALETTE_STYLE = stringPreferencesKey("palette_style")
        val AMOLED_DARK_MODE = booleanPreferencesKey("amoled_dark_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val USE_ALBUM_ART_COLOR = booleanPreferencesKey("use_album_art_color")
        val SEED_COLOR = intPreferencesKey("seed_color")
        
        val BLACKLISTED_FOLDERS = stringSetPreferencesKey("blacklisted_folders")
        val PLAYLISTS_GRID_VIEW = booleanPreferencesKey("playlists_grid_view")
    }

    val isCrossfadeEnabled: Flow<Boolean> = context.dataStore.data.map { it[CROSSFADE_ENABLED] ?: true }
    val crossfadeDuration: Flow<Long> = context.dataStore.data.map { it[CROSSFADE_DURATION] ?: 3000L }
    val minTrackDuration: Flow<Int> = context.dataStore.data.map { it[MIN_TRACK_DURATION] ?: 0 }
    val defaultSortType: Flow<String> = context.dataStore.data.map { it[DEFAULT_SORT_TYPE] ?: "BY_DATE" }
    val isDefaultSortReversed: Flow<Boolean> = context.dataStore.data.map { it[DEFAULT_SORT_REVERSED] ?: false }
    val progressBarStyle: Flow<String> = context.dataStore.data.map { it[PROGRESS_BAR_STYLE] ?: "STANDARD" }
    val appFontFamily: Flow<String> = context.dataStore.data.map { it[APP_FONT_FAMILY] ?: "DEFAULT" }
    val darkMode: Flow<String> = context.dataStore.data.map { it[DARK_MODE] ?: "FOLLOW_SYSTEM" }

    val paletteStyle: Flow<String> = context.dataStore.data.map { it[PALETTE_STYLE] ?: "TonalSpot" }
    val amoledDarkMode: Flow<Boolean> = context.dataStore.data.map { it[AMOLED_DARK_MODE] ?: false }
    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { it[USE_DYNAMIC_COLOR] ?: true }
    val useAlbumArtColor: Flow<Boolean> = context.dataStore.data.map { it[USE_ALBUM_ART_COLOR] ?: true }
    val seedColor: Flow<Int> = context.dataStore.data.map { it[SEED_COLOR] ?: 0xFF6750A4.toInt() }
    
    val blacklistedFolders: Flow<Set<String>> = context.dataStore.data.map { it[BLACKLISTED_FOLDERS] ?: emptySet() }
    val isPlaylistsGridView: Flow<Boolean> = context.dataStore.data.map { it[PLAYLISTS_GRID_VIEW] ?: false }

    suspend fun setCrossfadeEnabled(enabled: Boolean) = context.dataStore.edit { it[CROSSFADE_ENABLED] = enabled }
    suspend fun setCrossfadeDuration(duration: Long) = context.dataStore.edit { it[CROSSFADE_DURATION] = duration }
    suspend fun setMinTrackDuration(seconds: Int) = context.dataStore.edit { it[MIN_TRACK_DURATION] = seconds }
    suspend fun setDefaultSortType(sortType: String) = context.dataStore.edit { it[DEFAULT_SORT_TYPE] = sortType }
    suspend fun setDefaultSortReversed(reversed: Boolean) = context.dataStore.edit { it[DEFAULT_SORT_REVERSED] = reversed }
    suspend fun setProgressBarStyle(style: String) = context.dataStore.edit { it[PROGRESS_BAR_STYLE] = style }
    suspend fun setAppFontFamily(fontFamily: String) = context.dataStore.edit { it[APP_FONT_FAMILY] = fontFamily }
    suspend fun setDarkMode(darkMode: String) = context.dataStore.edit { it[DARK_MODE] = darkMode }

    suspend fun setPaletteStyle(style: String) = context.dataStore.edit { it[PALETTE_STYLE] = style }
    suspend fun setAmoledDarkMode(enabled: Boolean) = context.dataStore.edit { it[AMOLED_DARK_MODE] = enabled }
    suspend fun setUseDynamicColor(enabled: Boolean) = context.dataStore.edit { it[USE_DYNAMIC_COLOR] = enabled }
    suspend fun setUseAlbumArtColor(enabled: Boolean) = context.dataStore.edit { it[USE_ALBUM_ART_COLOR] = enabled }
    suspend fun setSeedColor(color: Int) = context.dataStore.edit { it[SEED_COLOR] = color }
    
    suspend fun addFolderToBlacklist(path: String) = context.dataStore.edit { 
        val current = it[BLACKLISTED_FOLDERS] ?: emptySet()
        it[BLACKLISTED_FOLDERS] = current + path
    }
    
    suspend fun removeFolderFromBlacklist(path: String) = context.dataStore.edit { 
        val current = it[BLACKLISTED_FOLDERS] ?: emptySet()
        it[BLACKLISTED_FOLDERS] = current - path
    }

    suspend fun setPlaylistsGridView(enabled: Boolean) = context.dataStore.edit { it[PLAYLISTS_GRID_VIEW] = enabled }
}
