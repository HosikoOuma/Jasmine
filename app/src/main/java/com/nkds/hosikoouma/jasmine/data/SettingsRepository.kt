package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    }

    val isCrossfadeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CROSSFADE_ENABLED] ?: true
    }

    val crossfadeDuration: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[CROSSFADE_DURATION] ?: 3000L
    }

    val minTrackDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[MIN_TRACK_DURATION] ?: 0
    }

    val defaultSortType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_SORT_TYPE] ?: "BY_DATE"
    }

    val isDefaultSortReversed: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_SORT_REVERSED] ?: false
    }

    val progressBarStyle: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PROGRESS_BAR_STYLE] ?: "STANDARD"
    }

    val appFontFamily: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_FONT_FAMILY] ?: "DEFAULT"
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setCrossfadeDuration(duration: Long) {
        context.dataStore.edit { preferences -> preferences[CROSSFADE_DURATION] = duration }
    }

    suspend fun setMinTrackDuration(seconds: Int) {
        context.dataStore.edit { preferences -> preferences[MIN_TRACK_DURATION] = seconds }
    }

    suspend fun setDefaultSortType(sortType: String) {
        context.dataStore.edit { preferences -> preferences[DEFAULT_SORT_TYPE] = sortType }
    }

    suspend fun setDefaultSortReversed(reversed: Boolean) {
        context.dataStore.edit { preferences -> preferences[DEFAULT_SORT_REVERSED] = reversed }
    }

    suspend fun setProgressBarStyle(style: String) {
        context.dataStore.edit { preferences -> preferences[PROGRESS_BAR_STYLE] = style }
    }

    suspend fun setAppFontFamily(fontFamily: String) {
        context.dataStore.edit { preferences -> preferences[APP_FONT_FAMILY] = fontFamily }
    }
}
