package com.mokostudio.mokomusic.data

object SampleTrackRepository {
    val tracks: List<Track> = listOf(
        Track(
            id = "synthetic-tone-1",
            title = "새벽의 점",
            artist = "틈플레이 테스트 샘플",
            uri = "memory://synthetic-tone-1.wav",
        ),
        Track(
            id = "synthetic-tone-2",
            title = "도시의 잔향",
            artist = "틈플레이 테스트 샘플",
            uri = "memory://synthetic-tone-2.wav",
        ),
        Track(
            id = "synthetic-tone-3",
            title = "비 오는 창가",
            artist = "틈플레이 테스트 샘플",
            uri = "memory://synthetic-tone-3.wav",
        ),
    )
}
