package com.mokostudio.mokomusic.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesLibraryRepositoryTest {
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val preferences by lazy {
        testContext.getSharedPreferences("music_library", Context.MODE_PRIVATE)
    }
    private lateinit var repository: SharedPreferencesLibraryRepository

    @Before
    fun setUp() {
        preferences.edit().clear().commit()
        repository = SharedPreferencesLibraryRepository(testContext)
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun favoritesPersistAcrossRepositoryInstancesAndCanBeRemoved() {
        assertEquals(setOf("track-a"), repository.toggleFavorite("track-a"))

        val restoredRepository = SharedPreferencesLibraryRepository(testContext)
        assertEquals(setOf("track-a"), restoredRepository.favoriteTrackIds())
        assertEquals(emptySet<String>(), restoredRepository.toggleFavorite("track-a"))
        assertEquals(emptySet<String>(), repository.favoriteTrackIds())
    }

    @Test
    fun recentlyPlayedMovesRepeatedTrackToTheFrontWithoutDuplicates() {
        repository.recordRecentlyPlayed("track-a")
        repository.recordRecentlyPlayed("track-b")
        repository.recordRecentlyPlayed("track-b")
        repository.recordRecentlyPlayed("track-a")

        assertEquals(listOf("track-a", "track-b"), repository.recentlyPlayedTrackIds())
    }

    @Test
    fun recentlyPlayedKeepsOnlyTheTenMostRecentTracks() {
        (1..11).forEach { index -> repository.recordRecentlyPlayed("track-$index") }

        assertEquals(10, repository.recentlyPlayedTrackIds().size)
        assertEquals("track-11", repository.recentlyPlayedTrackIds().first())
        assertEquals("track-2", repository.recentlyPlayedTrackIds().last())
    }
}
