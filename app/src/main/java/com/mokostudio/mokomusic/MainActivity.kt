package com.mokostudio.mokomusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.mokomusic.playback.PlaybackUiState
import com.mokostudio.mokomusic.playback.PlaybackViewModel
import com.mokostudio.mokomusic.playback.RepeatMode
import com.mokostudio.mokomusic.ui.theme.MokomusicTheme

class MainActivity : ComponentActivity() {
    private val playbackViewModel: PlaybackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MokomusicTheme {
                val uiState by playbackViewModel.uiState.collectAsStateWithLifecycle()
                PlaybackScreen(
                    uiState = uiState,
                    onTogglePlayback = playbackViewModel::togglePlayback,
                    onPreviousTrack = playbackViewModel::skipToPreviousTrack,
                    onNextTrack = playbackViewModel::skipToNextTrack,
                    onToggleShuffle = playbackViewModel::toggleShuffle,
                    onCycleRepeatMode = playbackViewModel::cycleRepeatMode,
                    onSeek = playbackViewModel::seekTo,
                    onSelectTrack = playbackViewModel::selectTrack,
                    onToggleFavorite = playbackViewModel::toggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun PlaybackScreen(
    uiState: PlaybackUiState,
    onTogglePlayback: () -> Unit,
    onPreviousTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectTrack: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val duration = uiState.durationMs.toFloat().coerceAtLeast(1f)
    var isSeeking by remember(uiState.currentMediaItemIndex, uiState.durationMs) {
        mutableStateOf(false)
    }
    var seekPositionMs by remember(uiState.currentMediaItemIndex, uiState.durationMs) {
        mutableFloatStateOf(uiState.positionMs.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "틈플레이", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = uiState.title, style = MaterialTheme.typography.titleLarge)
        Text(text = uiState.artist, style = MaterialTheme.typography.bodyMedium)
        if (uiState.recentlyPlayedTracks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "최근 재생", style = MaterialTheme.typography.labelLarge)
            Text(
                text = uiState.recentlyPlayedTracks.take(3).joinToString(" · ") { it.title },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            itemsIndexed(uiState.tracks) { index, track ->
                val isFavorite = track.id in uiState.favoriteTrackIds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { onSelectTrack(index) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (index == uiState.currentMediaItemIndex) {
                                "재생 중 · ${track.title}"
                            } else {
                                track.title
                            },
                        )
                    }
                    IconButton(
                        onClick = { onToggleFavorite(track.id) },
                        modifier = Modifier.semantics {
                            contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가"
                        },
                    ) {
                        Text(if (isFavorite) "★" else "☆")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Slider(
            value = (if (isSeeking) seekPositionMs else uiState.positionMs.toFloat())
                .coerceIn(0f, duration),
            onValueChange = {
                seekPositionMs = it
                isSeeking = true
            },
            onValueChangeFinished = {
                onSeek(seekPositionMs.toLong())
                isSeeking = false
            },
            valueRange = 0f..duration,
            enabled = uiState.connected && uiState.durationMs > 0L,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(
                onClick = onPreviousTrack,
                enabled = uiState.connected && uiState.hasPreviousMediaItem,
            ) {
                Text("이전 곡")
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Button(onClick = onTogglePlayback, enabled = uiState.connected) {
                Text(if (uiState.isPlaying) "일시정지" else "재생")
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Button(
                onClick = onNextTrack,
                enabled = uiState.connected && uiState.hasNextMediaItem,
            ) {
                Text("다음 곡")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(
                onClick = onToggleShuffle,
                enabled = uiState.connected && uiState.canChangeShuffle,
            ) {
                Text(if (uiState.shuffleEnabled) "셔플 켜짐" else "셔플 꺼짐")
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Button(
                onClick = onCycleRepeatMode,
                enabled = uiState.connected && uiState.canChangeRepeatMode,
            ) {
                Text(
                    text = when (uiState.repeatMode) {
                        RepeatMode.ALL -> "전체 반복"
                        RepeatMode.ONE -> "한 곡 반복"
                        RepeatMode.OFF -> "반복 꺼짐"
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (uiState.connected) "MediaSession 연결됨" else "MediaSession 연결 중",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
