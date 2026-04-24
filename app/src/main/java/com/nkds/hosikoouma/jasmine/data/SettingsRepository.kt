package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
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

        // Состояние плеера
        val LAST_MEDIA_ITEM_INDEX = intPreferencesKey("last_media_item_index")
        val LAST_PLAYBACK_POSITION = longPreferencesKey("last_playback_position")
        val IS_RADIO_MODE = booleanPreferencesKey("is_radio_mode")
        val LAST_RADIO_STATION_ID = longPreferencesKey("last_radio_station_id")

        // Навигация
        val NAVIGATION_ITEMS = stringPreferencesKey("navigation_items")
        
        // Кнопки управления плеером
        val PLAYER_CONTROLS_ORDER = stringPreferencesKey("player_controls_order")

        // Аудиофокус
        val MANAGE_AUDIO_FOCUS = booleanPreferencesKey("manage_audio_focus")

        // On Repeat
        val ON_REPEAT_INTERVAL_DAYS = intPreferencesKey("on_repeat_interval_days")
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

    val navigationItems: Flow<String> = context.dataStore.data.map { 
        it[NAVIGATION_ITEMS] ?: "tracks,radio,library,settings" 
    }
    
    val playerControlsOrder: Flow<String> = context.dataStore.data.map {
        it[PLAYER_CONTROLS_ORDER] ?: "shuffle,previous,play_pause,next,repeat"
    }

    // Состояние плеера
    val lastMediaItemIndex: Flow<Int> = context.dataStore.data.map { it[LAST_MEDIA_ITEM_INDEX] ?: 0 }
    val lastPlaybackPosition: Flow<Long> = context.dataStore.data.map { it[LAST_PLAYBACK_POSITION] ?: 0L }
    val isRadioMode: Flow<Boolean> = context.dataStore.data.map { it[IS_RADIO_MODE] ?: false }
    val lastRadioStationId: Flow<Long> = context.dataStore.data.map { it[LAST_RADIO_STATION_ID] ?: -1L }

    // Аудиофокус
    val manageAudioFocus: Flow<Boolean> = context.dataStore.data.map { it[MANAGE_AUDIO_FOCUS] ?: true }

    // On Repeat
    val onRepeatIntervalDays: Flow<Int> = context.dataStore.data.map { it[ON_REPEAT_INTERVAL_DAYS] ?: 7 }

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
    
    suspend fun setNavigationItems(items: String) = context.dataStore.edit { it[NAVIGATION_ITEMS] = items }
    
    suspend fun setPlayerControlsOrder(order: String) = context.dataStore.edit { it[PLAYER_CONTROLS_ORDER] = order }

    suspend fun addFolderToBlacklist(path: String) = context.dataStore.edit { 
        val current = it[BLACKLISTED_FOLDERS] ?: emptySet()
        it[BLACKLISTED_FOLDERS] = current + path
    }
    
    suspend fun removeFolderFromBlacklist(path: String) = context.dataStore.edit { 
        val current = it[BLACKLISTED_FOLDERS] ?: emptySet()
        it[BLACKLISTED_FOLDERS] = current - path
    }

    suspend fun setPlaylistsGridView(enabled: Boolean) = context.dataStore.edit { it[PLAYLISTS_GRID_VIEW] = enabled }

    suspend fun savePlayerState(index: Int, position: Long, isRadio: Boolean = false, radioStationId: Long = -1L) = context.dataStore.edit {
        it[LAST_MEDIA_ITEM_INDEX] = index
        it[LAST_PLAYBACK_POSITION] = position
        it[IS_RADIO_MODE] = isRadio
        it[LAST_RADIO_STATION_ID] = radioStationId
    }

    suspend fun setManageAudioFocus(enabled: Boolean) = context.dataStore.edit { it[MANAGE_AUDIO_FOCUS] = enabled }

    suspend fun setOnRepeatIntervalDays(days: Int) = context.dataStore.edit { it[ON_REPEAT_INTERVAL_DAYS] = days }
}
