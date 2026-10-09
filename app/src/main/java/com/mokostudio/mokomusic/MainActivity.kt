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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
                    onSeek = playbackViewModel::seekTo,
                )
            }
        }
    }
}

@Composable
private fun PlaybackScreen(
    uiState: PlaybackUiState,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val duration = uiState.durationMs.toFloat().coerceAtLeast(1f)

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
        Slider(
            value = uiState.positionMs.toFloat().coerceIn(0f, duration),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..duration,
            enabled = uiState.connected && uiState.durationMs > 0L,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = onTogglePlayback, enabled = uiState.connected) {
                Text(if (uiState.isPlaying) "일시정지" else "재생")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (uiState.connected) "MediaSession 연결됨" else "MediaSession 연결 중",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
