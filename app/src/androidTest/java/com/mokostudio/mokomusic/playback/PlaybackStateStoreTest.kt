package com.mokostudio.mokomusic.playback

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mokostudio.mokomusic.data.SampleTrackRepository
import com.mokostudio.mokomusic.data.toMediaItem
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackStateStoreTest {
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val preferences by lazy {
        testContext.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
    }
    private lateinit var store: PlaybackStateStore

    @Before
    fun setUp() {
        preferences.edit().clear().commit()
        store = PlaybackStateStore(testContext)
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun loadReturnsDefaultsWhenNoPlaybackStateWasSaved() {
        assertEquals(
            SavedPlaybackState(
                mediaId = null,
                positionMs = 0L,
                shuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_OFF,
            ),
            store.load(),
        )
    }

    @Test
    fun savedTrackPositionAndModesAreRestoredByANewStoreInstance() {
        store.save(
            SavedPlaybackState(
                mediaId = "commons-the-entertainer",
                positionMs = 126_000L,
                shuffleEnabled = true,
                repeatMode = Player.REPEAT_MODE_ONE,
            ),
        )

        val restoredState = PlaybackStateStore(testContext).load()

        assertEquals("commons-the-entertainer", restoredState.mediaId)
        assertEquals(126_000L, restoredState.positionMs)
        assertEquals(true, restoredState.shuffleEnabled)
        assertEquals(Player.REPEAT_MODE_ONE, restoredState.repeatMode)
    }

    @Test
    fun saveReadsCurrentTrackPositionAndModesFromPlayer() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val playerReady = CountDownLatch(1)
        lateinit var player: ExoPlayer

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            player = ExoPlayer.Builder(appContext)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(DefaultDataSource.Factory(appContext)),
                )
                .build()
                .apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) playerReady.countDown()
                        }
                    })
                    setMediaItem(SampleTrackRepository.tracks.first().toMediaItem())
                    shuffleModeEnabled = true
                    repeatMode = Player.REPEAT_MODE_ALL
                    prepare()
                }
        }

        try {
            assertTrue("Player did not become ready", playerReady.await(10, TimeUnit.SECONDS))
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                player.seekTo(12_000L)
                store.save(player)
            }

            val restoredState = PlaybackStateStore(testContext).load()
            assertEquals(SampleTrackRepository.tracks.first().id, restoredState.mediaId)
            assertEquals(12_000L, restoredState.positionMs)
            assertEquals(true, restoredState.shuffleEnabled)
            assertEquals(Player.REPEAT_MODE_ALL, restoredState.repeatMode)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { player.release() }
        }
    }

    @Test
    fun loadClampsInvalidPositionAndFallsBackForUnknownRepeatMode() {
        preferences.edit()
            .putString("media_id", "track-a")
            .putLong("position_ms", -5L)
            .putInt("repeat_mode", 99)
            .commit()

        val restoredState = store.load()

        assertEquals(0L, restoredState.positionMs)
        assertEquals(Player.REPEAT_MODE_OFF, restoredState.repeatMode)
    }
}
