package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION = longPreferencesKey("crossfade_duration")
        val MIN_TRACK_DURATION = intPreferencesKey("min_track_duration") // Новая настройка (в секундах)
    }

    val isCrossfadeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CROSSFADE_ENABLED] ?: true
    }

    val crossfadeDuration: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[CROSSFADE_DURATION] ?: 3000L
    }

    val minTrackDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[MIN_TRACK_DURATION] ?: 0 // По умолчанию 0 (показывать всё)
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CROSSFADE_ENABLED] = enabled
        }
    }

    suspend fun setCrossfadeDuration(duration: Long) {
        context.dataStore.edit { preferences ->
            preferences[CROSSFADE_DURATION] = duration
        }
    }

    suspend fun setMinTrackDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[MIN_TRACK_DURATION] = seconds
        }
    }
}
