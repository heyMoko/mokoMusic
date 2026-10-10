package com.mokostudio.mokomusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.mokomusic.playback.PlaybackUiState
import com.mokostudio.mokomusic.playback.PlaybackViewModel
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
                    onSeek = playbackViewModel::seekTo,
                    onSelectTrack = playbackViewModel::selectTrack,
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
    onSeek: (Long) -> Unit,
    onSelectTrack: (Int) -> Unit,
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
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            itemsIndexed(uiState.tracks) { index, track ->
                Button(
                    onClick = { onSelectTrack(index) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (index == uiState.currentMediaItemIndex) {
                            "재생 중 · ${track.title}"
                        } else {
                            track.title
                        },
                    )
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (uiState.connected) "MediaSession 연결됨" else "MediaSession 연결 중",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
