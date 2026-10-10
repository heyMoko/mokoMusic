package com.mokostudio.mokomusic.data

import android.content.Context

interface LibraryRepository {
    fun favoriteTrackIds(): Set<String>

    fun toggleFavorite(trackId: String): Set<String>
}

class SharedPreferencesLibraryRepository(context: Context) : LibraryRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun favoriteTrackIds(): Set<String> =
        preferences.getStringSet(KEY_FAVORITE_TRACK_IDS, emptySet()).orEmpty().toSet()

    override fun toggleFavorite(trackId: String): Set<String> {
        val currentFavorites = favoriteTrackIds()
        val updatedFavorites = if (trackId in currentFavorites) {
            currentFavorites - trackId
        } else {
            currentFavorites + trackId
        }

        preferences.edit()
            .putStringSet(KEY_FAVORITE_TRACK_IDS, updatedFavorites)
            .apply()

        return updatedFavorites
    }

    private companion object {
        const val PREFERENCES_NAME = "music_library"
        const val KEY_FAVORITE_TRACK_IDS = "favorite_track_ids"
    }
}
