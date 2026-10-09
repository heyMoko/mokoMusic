package com.mokostudio.mokomusic.playback

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.mokostudio.mokomusic.data.SampleTrackRepository
import com.mokostudio.mokomusic.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "재생 준비 중",
    val artist: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentMediaItemIndex: Int = 0,
    val tracks: List<Track> = emptyList(),
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(
        PlaybackUiState(tracks = SampleTrackRepository.tracks),
    )
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player)
        }
    }

    init {
        val sessionToken = SessionToken(
            application,
            ComponentName(application, PlaybackService::class.java),
        )
        val controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                controller = controllerFuture.get().also { connectedController ->
                    connectedController.addListener(playerListener)
                    updateState(connectedController)
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun togglePlayback() {
        controller?.let { playbackController ->
            if (playbackController.isPlaying) playbackController.pause() else playbackController.play()
            updateState(playbackController)
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun selectTrack(index: Int) {
        controller?.let { playbackController ->
            playbackController.seekToDefaultPosition(index)
            playbackController.play()
        }
    }

    private fun updateState(player: Player) {
        viewModelScope.launch {
            _uiState.emit(
                PlaybackUiState(
                    connected = true,
                    isPlaying = player.isPlaying,
                    title = player.mediaMetadata.title?.toString() ?: "재생 준비 중",
                    artist = player.mediaMetadata.artist?.toString().orEmpty(),
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L),
                    currentMediaItemIndex = player.currentMediaItemIndex,
                    tracks = SampleTrackRepository.tracks,
                ),
            )
        }
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
