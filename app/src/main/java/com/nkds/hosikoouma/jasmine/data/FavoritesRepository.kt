package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private val FAVORITE_TRACKS = stringSetPreferencesKey("favorite_tracks")
    }

    val favoriteTrackIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_TRACKS] ?: emptySet()
    }

    suspend fun toggleFavorite(trackId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITE_TRACKS] ?: emptySet()
            val newFavorites = if (currentFavorites.contains(trackId)) {
                currentFavorites - trackId
            } else {
                currentFavorites + trackId
            }
            preferences[FAVORITE_TRACKS] = newFavorites
        }
    }

    suspend fun isFavorite(trackId: String): Boolean {
        return false 
    }
}
