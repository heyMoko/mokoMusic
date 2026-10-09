package com.mokostudio.mokomusic.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mokostudio.mokomusic.MainActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val playbackPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                ProgressiveMediaSource.Factory { ByteArrayDataSource(createSampleWave()) },
            )
            .build()
            .apply {
                setMediaItem(sampleMediaItem())
                prepare()
                playWhenReady = false
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
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun sampleMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId("synthetic-tone")
        .setUri("memory://synthetic-tone.wav")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("틈플레이 샘플")
                .setArtist("직접 생성한 테스트 톤")
                .build(),
        )
        .build()

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
