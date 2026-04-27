package com.nkds.hosikoouma.jasmine.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        private const val TAG = "FavoritesRepository"
        private val FAVORITE_TRACKS = stringSetPreferencesKey("favorite_tracks")
    }

    val favoriteTrackIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val ids = preferences[FAVORITE_TRACKS] ?: emptySet()
        Log.d(TAG, "Loaded favorite IDs: $ids")
        ids
    }

    suspend fun toggleFavorite(trackId: String) {
        // Убираем возможные части UUID, если они просочились
        val cleanId = if (trackId.contains("_")) trackId.split("_")[0] else trackId
        
        Log.d(TAG, "Toggling favorite for clean ID: $cleanId (original: $trackId)")
        
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITE_TRACKS] ?: emptySet()
            val newFavorites = if (currentFavorites.contains(cleanId)) {
                Log.d(TAG, "Removing $cleanId from favorites")
                currentFavorites - cleanId
            } else {
                Log.d(TAG, "Adding $cleanId to favorites")
                currentFavorites + cleanId
            }
            preferences[FAVORITE_TRACKS] = newFavorites
        }
    }

    suspend fun isFavorite(trackId: String): Boolean {
        val cleanId = if (trackId.contains("_")) trackId.split("_")[0] else trackId
        val currentFavorites = favoriteTrackIds.first()
        val result = currentFavorites.contains(cleanId)
        Log.d(TAG, "Check isFavorite for $cleanId: $result")
        return result
    }
}
