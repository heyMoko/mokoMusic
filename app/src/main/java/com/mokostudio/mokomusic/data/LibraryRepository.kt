package com.mokostudio.mokomusic.data

import android.content.Context
import org.json.JSONArray

interface LibraryRepository {
    fun favoriteTrackIds(): Set<String>

    fun toggleFavorite(trackId: String): Set<String>

    fun recentlyPlayedTrackIds(): List<String>

    fun recordRecentlyPlayed(trackId: String): List<String>
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

    override fun recentlyPlayedTrackIds(): List<String> {
        val json = preferences.getString(KEY_RECENT_TRACK_IDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                repeat(array.length()) { index ->
                    array.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    override fun recordRecentlyPlayed(trackId: String): List<String> {
        val currentRecentTracks = recentlyPlayedTrackIds()
        if (currentRecentTracks.firstOrNull() == trackId) return currentRecentTracks

        val updatedRecentTracks = (listOf(trackId) + currentRecentTracks.filterNot { it == trackId })
            .take(MAX_RECENT_TRACKS)
        preferences.edit()
            .putString(KEY_RECENT_TRACK_IDS, JSONArray(updatedRecentTracks).toString())
            .apply()

        return updatedRecentTracks
    }

    private companion object {
        const val PREFERENCES_NAME = "music_library"
        const val KEY_FAVORITE_TRACK_IDS = "favorite_track_ids"
        const val KEY_RECENT_TRACK_IDS = "recent_track_ids"
        const val MAX_RECENT_TRACKS = 10
    }
}
