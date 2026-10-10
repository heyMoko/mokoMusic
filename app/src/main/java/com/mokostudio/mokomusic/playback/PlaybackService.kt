package com.mokostudio.mokomusic.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mokostudio.mokomusic.MainActivity
import com.mokostudio.mokomusic.data.LibraryRepository
import com.mokostudio.mokomusic.data.SampleTrackRepository
import com.mokostudio.mokomusic.data.SharedPreferencesLibraryRepository
import com.mokostudio.mokomusic.data.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultDataSource.Factory(this)),
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

}
