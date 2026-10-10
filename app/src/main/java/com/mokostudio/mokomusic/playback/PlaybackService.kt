package com.mokostudio.mokomusic.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mokostudio.mokomusic.MainActivity
import com.mokostudio.mokomusic.data.LibraryRepository
import com.mokostudio.mokomusic.data.SampleTrackRepository
import com.mokostudio.mokomusic.data.SharedPreferencesLibraryRepository
import com.mokostudio.mokomusic.data.toMediaItem
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var playbackStateStore: PlaybackStateStore
    private lateinit var libraryRepository: LibraryRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var savePlaybackStateJob: Job? = null

    private val playbackStateListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            playbackStateStore.save(player)
        }
    }

    private val recentTrackListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                mediaSession?.player?.currentMediaItem?.mediaId?.let(
                    libraryRepository::recordRecentlyPlayed,
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaSession?.player?.isPlaying == true) {
                mediaItem?.mediaId?.let(libraryRepository::recordRecentlyPlayed)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        playbackStateStore = PlaybackStateStore(this)
        libraryRepository = SharedPreferencesLibraryRepository(this)
        val savedState = playbackStateStore.load()
        val tracks = SampleTrackRepository.tracks
        val restoredIndex = tracks.indexOfFirst { it.id == savedState.mediaId }
            .takeIf { it >= 0 }
            ?: 0

        val playbackPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                ProgressiveMediaSource.Factory { ByteArrayDataSource(createSampleWave()) },
            )
            .build()
            .apply {
                repeatMode = savedState.repeatMode
                shuffleModeEnabled = savedState.shuffleEnabled
                setMediaItems(
                    tracks.map { it.toMediaItem() },
                    restoredIndex,
                    savedState.positionMs,
                )
                addListener(playbackStateListener)
                addListener(recentTrackListener)
                prepare()
                playWhenReady = false
            }
        savePlaybackStateJob = serviceScope.launch {
            while (isActive) {
                delay(5_000L)
                playbackStateStore.save(playbackPlayer)
            }
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaSession = MediaSession.Builder(this, playbackPlayer)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (mediaSession?.player?.playWhenReady == true) {
            return
        }
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.player?.let(playbackStateStore::save)
        savePlaybackStateJob?.cancel()
        savePlaybackStateJob = null
        serviceScope.cancel()
        mediaSession?.run {
            player.removeListener(playbackStateListener)
            player.removeListener(recentTrackListener)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun createSampleWave(): ByteArray {
        val sampleRate = 44_100
        val durationSeconds = 8
        val channelCount = 1
        val bitsPerSample = 16
        val sampleCount = sampleRate * durationSeconds
        val audioDataSize = sampleCount * channelCount * bitsPerSample / 8
        val wave = ByteBuffer.allocate(44 + audioDataSize).order(ByteOrder.LITTLE_ENDIAN)

        wave.put("RIFF".toByteArray())
        wave.putInt(36 + audioDataSize)
        wave.put("WAVE".toByteArray())
        wave.put("fmt ".toByteArray())
        wave.putInt(16)
        wave.putShort(1)
        wave.putShort(channelCount.toShort())
        wave.putInt(sampleRate)
        wave.putInt(sampleRate * channelCount * bitsPerSample / 8)
        wave.putShort((channelCount * bitsPerSample / 8).toShort())
        wave.putShort(bitsPerSample.toShort())
        wave.put("data".toByteArray())
        wave.putInt(audioDataSize)

        repeat(sampleCount) { sampleIndex ->
            val timeSeconds = sampleIndex.toDouble() / sampleRate
            val envelope = when {
                timeSeconds < 0.1 -> timeSeconds / 0.1
                timeSeconds > durationSeconds - 0.2 -> (durationSeconds - timeSeconds) / 0.2
                else -> 1.0
            }
            val sample = sin(2.0 * PI * 440.0 * timeSeconds) * 0.25 * envelope
            wave.putShort((sample * Short.MAX_VALUE).toInt().toShort())
        }
        return wave.array()
    }
}
