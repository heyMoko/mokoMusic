package com.mokostudio.mokomusic.playback

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
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

enum class ControllerConnectionStatus {
    CONNECTING,
    CONNECTED,
    FAILED,
}

data class ControllerConnectionUiState(
    val status: ControllerConnectionStatus = ControllerConnectionStatus.CONNECTING,
    val errorMessage: String? = null,
) {
    fun onConnecting(): ControllerConnectionUiState =
        copy(status = ControllerConnectionStatus.CONNECTING, errorMessage = null)

    fun onConnected(): ControllerConnectionUiState =
        copy(status = ControllerConnectionStatus.CONNECTED, errorMessage = null)

    fun onFailed(message: String): ControllerConnectionUiState =
        copy(status = ControllerConnectionStatus.FAILED, errorMessage = message)
}

data class PlaybackUiState(
    val controllerConnection: ControllerConnectionUiState = ControllerConnectionUiState(),
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
    val playbackError: String? = null,
    val tracks: List<Track> = emptyList(),
) {
    val connected: Boolean
        get() = controllerConnection.status == ControllerConnectionStatus.CONNECTED
}

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val libraryRepository: LibraryRepository = SharedPreferencesLibraryRepository(application)
    private val sessionToken = SessionToken(
        application,
        ComponentName(application, PlaybackService::class.java),
    )
    private val _uiState = MutableStateFlow(
        PlaybackUiState(
            tracks = SampleTrackRepository.tracks,
            favoriteTrackIds = libraryRepository.favoriteTrackIds(),
            recentlyPlayedTracks = loadRecentlyPlayedTracks(),
        ),
    )
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var connectionAttempt = 0
    private var progressUpdates: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _uiState.update { it.copy(playbackError = playbackErrorMessage(error.errorCode)) }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player)
        }
    }

    init {
        connectToPlaybackService()
    }

    fun retryControllerConnection() {
        if (_uiState.value.controllerConnection.status == ControllerConnectionStatus.FAILED) {
            connectToPlaybackService()
        }
    }

    private fun connectToPlaybackService() {
        val attempt = ++connectionAttempt
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        _uiState.update {
            it.copy(controllerConnection = it.controllerConnection.onConnecting())
        }

        val newControllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture = newControllerFuture
        newControllerFuture.addListener(
            {
                if (attempt != connectionAttempt) return@addListener

                try {
                    val connectedController = newControllerFuture.get()
                    controller = connectedController.also { it.addListener(playerListener) }
                    _uiState.update {
                        it.copy(controllerConnection = it.controllerConnection.onConnected())
                    }
                    updateState(connectedController)
                } catch (exception: Exception) {
                    if (exception is InterruptedException) Thread.currentThread().interrupt()
                    if (controllerFuture === newControllerFuture) {
                        controllerFuture = null
                        MediaController.releaseFuture(newControllerFuture)
                    }
                    _uiState.update {
                        it.copy(controllerConnection = it.controllerConnection.onFailed(CONNECTION_ERROR_MESSAGE))
                    }
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
        controller?.let { playbackController ->
            val recoveringFromError = _uiState.value.playbackError != null
            playbackController.seekToPreviousMediaItem()
            if (recoveringFromError) {
                playbackController.prepare()
                playbackController.play()
                clearPlaybackError()
            }
        }
    }

    fun skipToNextTrack() {
        controller?.let { playbackController ->
            val recoveringFromError = _uiState.value.playbackError != null
            playbackController.seekToNextMediaItem()
            if (recoveringFromError) {
                playbackController.prepare()
                playbackController.play()
                clearPlaybackError()
            }
        }
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
            if (playbackController.playbackState == Player.STATE_IDLE ||
                playbackController.playbackState == Player.STATE_ENDED
            ) {
                playbackController.prepare()
            }
            playbackController.play()
            clearPlaybackError()
        }
    }

    fun retryPlayback() {
        controller?.let { playbackController ->
            clearPlaybackError()
            playbackController.prepare()
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
                    controllerConnection = currentState.controllerConnection.onConnected(),
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

    private fun clearPlaybackError() {
        _uiState.update { it.copy(playbackError = null) }
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
        connectionAttempt++
        progressUpdates?.cancel()
        progressUpdates = null
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        super.onCleared()
    }

    private companion object {
        const val CONNECTION_ERROR_MESSAGE = "플레이어 서비스에 연결하지 못했어요. 다시 시도해 주세요."
    }
}
