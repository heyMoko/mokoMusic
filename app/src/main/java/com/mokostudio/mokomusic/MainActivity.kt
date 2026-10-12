package com.mokostudio.mokomusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mokostudio.mokomusic.playback.ControllerConnectionStatus
import com.mokostudio.mokomusic.playback.PlaybackUiState
import com.mokostudio.mokomusic.playback.PlaybackViewModel
import com.mokostudio.mokomusic.playback.RepeatMode
import com.mokostudio.mokomusic.ui.theme.MokomusicTheme

private val Ink = Color(0xFF111315)
private val Panel = Color(0xFF1B1E20)
private val Muted = Color(0xFF9A9FA1)
private val Coral = Color(0xFFFF765F)
private val Cream = Color(0xFFF5F0E8)

class MainActivity : ComponentActivity() {
    private val playbackViewModel: PlaybackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
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
                    onRetryPlayback = playbackViewModel::retryPlayback,
                    onRetryControllerConnection = playbackViewModel::retryControllerConnection,
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
    onRetryPlayback: () -> Unit,
    onRetryControllerConnection: () -> Unit,
) {
    val duration = uiState.durationMs.toFloat().coerceAtLeast(1f)
    val favoriteTracks = uiState.tracks.withIndex()
        .filter { it.value.id in uiState.favoriteTrackIds }
    var isSeeking by remember(uiState.currentMediaItemIndex, uiState.durationMs) {
        mutableStateOf(false)
    }
    var seekPositionMs by remember(uiState.currentMediaItemIndex, uiState.durationMs) {
        mutableFloatStateOf(uiState.positionMs.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Coral),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("m", color = Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("mokoMusic", color = Cream, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("MUSIC FOR YOUR MOMENTS", color = Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
                }
            }
            Surface(
                color = if (uiState.connected) Color(0xFF26342E) else Panel,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (uiState.connected) Color(0xFF8FDAA9) else Muted),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (uiState.connected) "연결됨" else "연결 중",
                        color = if (uiState.connected) Color(0xFFB4E9C6) else Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        uiState.controllerConnection.errorMessage?.let { connectionError ->
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF342321),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(connectionError, modifier = Modifier.weight(1f), color = Cream, fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        onClick = onRetryControllerConnection,
                        shape = CircleShape,
                        color = Coral,
                        contentColor = Ink,
                    ) {
                        Text(
                            "다시 연결",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("지금 듣는 음악", color = Muted, fontSize = 12.sp, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 370.dp)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF513B42), Color(0xFFB55E4E), Color(0xFFE9A76D)),
                    ),
                )
                .semantics { contentDescription = "mokoMusic 추상 앨범 아트" },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.72f, size.height * 0.42f)
                val radius = size.minDimension * 0.36f
                drawCircle(Color(0x22FFF0D9), radius * 1.42f, center)
                drawCircle(Color(0x22FFF0D9), radius * 1.12f, center, style = Stroke(2.dp.toPx()))
                drawCircle(Color(0x33FFF0D9), radius * 0.82f, center, style = Stroke(2.dp.toPx()))
                drawCircle(Color(0x44FFF0D9), radius * 0.52f, center, style = Stroke(2.dp.toPx()))
                drawCircle(Color(0xCCF5F0E8), radius * 0.13f, center)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp),
            ) {
                Text("일상의 순간을 채우는", color = Color(0xFFEED8C8), fontSize = 12.sp)
                Text("A LITTLE PAUSE", color = Cream, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            Text(
                "SIDE 01  ·  2026",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp),
                color = Color(0xFFEED8C8),
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
            )
        }

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    uiState.title,
                    color = Cream,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(uiState.artist, color = Muted, fontSize = 14.sp)
            }
            val currentTrack = uiState.tracks.getOrNull(uiState.currentMediaItemIndex)
            val isFavorite = currentTrack?.id in uiState.favoriteTrackIds
            IconButton(
                onClick = { currentTrack?.let { onToggleFavorite(it.id) } },
                enabled = currentTrack != null,
                modifier = Modifier.semantics {
                    contentDescription = if (isFavorite) "즐겨찾기에서 제거" else "즐겨찾기에 추가"
                },
            ) {
                Text(if (isFavorite) "♥" else "♡", color = Coral, fontSize = 29.sp)
            }
        }

        if (uiState.isBuffering) {
            Text(
                text = "음원 불러오는 중…",
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
                color = Coral,
                fontSize = 12.sp,
            )
        }

        uiState.playbackError?.let { errorMessage ->
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF342321),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("재생할 수 없어요", color = Color(0xFFFFA08F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(errorMessage, color = Cream, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        onClick = onRetryPlayback,
                        enabled = uiState.connected,
                        shape = CircleShape,
                        color = Coral,
                        contentColor = Ink,
                    ) {
                        Text(
                            "다시 시도",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
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
            colors = SliderDefaults.colors(
                thumbColor = Coral,
                activeTrackColor = Coral,
                inactiveTrackColor = Color(0xFF414547),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(if (isSeeking) seekPositionMs.toLong() else uiState.positionMs), color = Muted, fontSize = 11.sp)
            Text(formatTime(uiState.durationMs), color = Muted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportButton("⤨", "셔플 ${if (uiState.shuffleEnabled) "켜짐" else "꺼짐"}", onToggleShuffle, uiState.connected && uiState.canChangeShuffle, uiState.shuffleEnabled)
            Spacer(Modifier.width(12.dp))
            TransportButton("⏮", "이전 곡", onPreviousTrack, uiState.connected && uiState.hasPreviousMediaItem)
            Spacer(Modifier.width(10.dp))
            Surface(
                onClick = onTogglePlayback,
                enabled = uiState.connected,
                modifier = Modifier.size(66.dp),
                shape = CircleShape,
                color = Coral,
                contentColor = Ink,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (uiState.isPlaying) "Ⅱ" else "▶", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            TransportButton("⏭", "다음 곡", onNextTrack, uiState.connected && uiState.hasNextMediaItem)
            Spacer(Modifier.width(12.dp))
            val repeatLabel = when (uiState.repeatMode) {
                RepeatMode.ALL -> "전체 반복"
                RepeatMode.ONE -> "한 곡 반복"
                RepeatMode.OFF -> "반복 꺼짐"
            }
            TransportButton("↻", repeatLabel, onCycleRepeatMode, uiState.connected && uiState.canChangeRepeatMode, uiState.repeatMode != RepeatMode.OFF)
        }

        Spacer(Modifier.height(26.dp))
        if (uiState.recentlyPlayedTracks.isNotEmpty()) {
            SectionHeading("최근 재생", "이어 듣기")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.recentlyPlayedTracks.take(3).forEachIndexed { index, track ->
                    val trackIndex = uiState.tracks.indexOfFirst { it.id == track.id }
                    Surface(
                        onClick = { if (trackIndex >= 0) onSelectTrack(trackIndex) },
                        enabled = trackIndex >= 0,
                        modifier = Modifier.weight(1f),
                        color = Panel,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("0${index + 1}", color = Coral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(track.title, color = Cream, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (favoriteTracks.isNotEmpty()) {
            SectionHeading("즐겨찾기", "${favoriteTracks.size}곡")
            Spacer(Modifier.height(10.dp))
            favoriteTracks.forEach { (index, track) ->
                Surface(
                    onClick = { onSelectTrack(index) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    color = Panel,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("♥", color = Coral, fontSize = 19.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                track.title,
                                color = Cream,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(track.artist, color = Muted, fontSize = 11.sp)
                        }
                        Text("재생", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(17.dp))
        }

        SectionHeading("재생 대기열", "${uiState.tracks.size}곡")
        Spacer(Modifier.height(10.dp))
        uiState.tracks.forEachIndexed { index, track ->
            val isCurrent = index == uiState.currentMediaItemIndex
            val trackIsFavorite = track.id in uiState.favoriteTrackIds
            Surface(
                onClick = { onSelectTrack(index) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                color = if (isCurrent) Color(0xFF2B2524) else Panel,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isCurrent) Coral else Color(0xFF34383A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (isCurrent && uiState.isPlaying) "♫" else "${index + 1}", color = if (isCurrent) Ink else Muted, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = if (isCurrent) Cream else Color(0xFFE2E0DA), fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(if (isCurrent) "지금 재생 중" else track.artist, color = if (isCurrent) Coral else Muted, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = { onToggleFavorite(track.id) },
                        modifier = Modifier.semantics {
                            contentDescription = if (trackIsFavorite) "즐겨찾기에서 제거" else "즐겨찾기에 추가"
                        },
                    ) {
                        Text(if (trackIsFavorite) "♥" else "♡", color = if (trackIsFavorite) Coral else Muted, fontSize = 22.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(13.dp))
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .border(1.dp, Color(0xFF34383A), CircleShape)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("●", color = if (uiState.connected) Color(0xFF8FDAA9) else Muted, fontSize = 8.sp)
            Spacer(Modifier.width(7.dp))
            Text(
                when (uiState.controllerConnection.status) {
                    ControllerConnectionStatus.CONNECTED -> "MediaSession 연결됨"
                    ControllerConnectionStatus.CONNECTING -> "MediaSession 연결 중"
                    ControllerConnectionStatus.FAILED -> "MediaSession 연결 실패"
                },
                color = Muted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun TransportButton(
    symbol: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    selected: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(42.dp)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = if (selected) Color(0xFF342724) else Color.Transparent,
        contentColor = if (selected) Coral else Cream,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = if (symbol == "⏭") 19.sp else 21.sp)
        }
    }
}

@Composable
private fun SectionHeading(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = Cream, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = Muted, fontSize = 11.sp)
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
