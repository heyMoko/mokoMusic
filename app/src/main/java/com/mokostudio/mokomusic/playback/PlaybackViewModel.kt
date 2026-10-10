package com.mokostudio.mokomusic.playback

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.mokostudio.mokomusic.data.LibraryRepository
import com.mokostudio.mokomusic.data.SampleTrackRepository
import com.mokostudio.mokomusic.data.SharedPreferencesLibraryRepository
import com.mokostudio.mokomusic.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF,
    ALL,
    ONE,
}

data class PlaybackUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "재생 준비 중",
    val artist: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentMediaItemIndex: Int = 0,
    val hasPreviousMediaItem: Boolean = false,
    val hasNextMediaItem: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val canChangeShuffle: Boolean = false,
    val canChangeRepeatMode: Boolean = false,
    val favoriteTrackIds: Set<String> = emptySet(),
    val recentlyPlayedTracks: List<Track> = emptyList(),
    val tracks: List<Track> = emptyList(),
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val libraryRepository: LibraryRepository = SharedPreferencesLibraryRepository(application)
    private val _uiState = MutableStateFlow(
        PlaybackUiState(
            tracks = SampleTrackRepository.tracks,
            favoriteTrackIds = libraryRepository.favoriteTrackIds(),
            recentlyPlayedTracks = loadRecentlyPlayedTracks(),
        ),
    )
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    private var controller: MediaController? = null
    private var progressUpdates: Job? = null

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
        _uiState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
        controller?.seekTo(positionMs)
    }

    fun skipToPreviousTrack() {
        controller?.seekToPreviousMediaItem()
    }

    fun skipToNextTrack() {
        controller?.seekToNextMediaItem()
    }

    fun toggleShuffle() {
        controller?.let { playbackController ->
            if (!playbackController.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE)) return
            playbackController.shuffleModeEnabled = !playbackController.shuffleModeEnabled
            updateState(playbackController)
        }
    }

    fun cycleRepeatMode() {
        controller?.let { playbackController ->
            if (!playbackController.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)) return
            playbackController.repeatMode = when (playbackController.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            updateState(playbackController)
        }
    }

    fun selectTrack(index: Int) {
        controller?.let { playbackController ->
            playbackController.seekToDefaultPosition(index)
            playbackController.play()
        }
    }

    fun toggleFavorite(trackId: String) {
        val favoriteTrackIds = libraryRepository.toggleFavorite(trackId)
        _uiState.update { it.copy(favoriteTrackIds = favoriteTrackIds) }
    }

    private fun updateState(player: Player) {
        if (player.isPlaying) {
            player.currentMediaItem?.mediaId?.let(libraryRepository::recordRecentlyPlayed)
        }
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    connected = true,
                    isPlaying = player.isPlaying,
                    title = player.mediaMetadata.title?.toString() ?: "재생 준비 중",
                    artist = player.mediaMetadata.artist?.toString().orEmpty(),
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L),
                    currentMediaItemIndex = player.currentMediaItemIndex,
                    hasPreviousMediaItem = player.hasPreviousMediaItem(),
                    hasNextMediaItem = player.hasNextMediaItem(),
                    shuffleEnabled = player.shuffleModeEnabled,
                    repeatMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                        else -> RepeatMode.OFF
                    },
                    canChangeShuffle = player.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE),
                    canChangeRepeatMode = player.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE),
                    favoriteTrackIds = currentState.favoriteTrackIds,
                    recentlyPlayedTracks = loadRecentlyPlayedTracks(),
                )
            }
        }
        if (player.isPlaying) {
            startProgressUpdates(player)
        } else {
            progressUpdates?.cancel()
            progressUpdates = null
        }
    }

    private fun loadRecentlyPlayedTracks(): List<Track> {
        val tracksById = SampleTrackRepository.tracks.associateBy(Track::id)
        return libraryRepository.recentlyPlayedTrackIds().mapNotNull(tracksById::get)
    }

    private fun startProgressUpdates(player: Player) {
        if (progressUpdates?.isActive == true) return

        progressUpdates = viewModelScope.launch {
            while (isActive && player.isPlaying) {
                _uiState.update {
                    it.copy(positionMs = player.currentPosition.coerceAtLeast(0L))
                }
                delay(250L)
            }
            progressUpdates = null
        }
    }

    override fun onCleared() {
        progressUpdates?.cancel()
        progressUpdates = null
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
