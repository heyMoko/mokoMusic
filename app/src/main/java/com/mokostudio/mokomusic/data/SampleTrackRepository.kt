package com.mokostudio.mokomusic.data

object SampleTrackRepository {
    val tracks: List<Track> = listOf(
        Track(
            id = "hyak-ep3-suika",
            title = "Suika",
            artist = "hyak",
            uri = "asset:///audio/suika.mp3",
        ),
        Track(
            id = "hyak-ep4-blackhole",
            title = "Black Hole",
            artist = "hyak",
            uri = "asset:///audio/blackhole.mp3",
        ),
        Track(
            id = "hyak-ep6-galaxy",
            title = "Galaxy Collision",
            artist = "hyak",
            uri = "asset:///audio/galaxy.mp3",
        ),
    )
}
