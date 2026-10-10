package com.mokostudio.mokomusic.playback

import android.content.Context
import androidx.media3.common.Player

internal data class SavedPlaybackState(
    val mediaId: String?,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
)

internal class PlaybackStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): SavedPlaybackState = SavedPlaybackState(
        mediaId = preferences.getString(KEY_MEDIA_ID, null),
        positionMs = preferences.getLong(KEY_POSITION_MS, 0L).coerceAtLeast(0L),
        shuffleEnabled = preferences.getBoolean(KEY_SHUFFLE_ENABLED, false),
        repeatMode = when (preferences.getInt(KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF)) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        },
    )

    fun save(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        val positionMs = if (player.playbackState == Player.STATE_ENDED) {
            0L
        } else {
            player.currentPosition.coerceAtLeast(0L)
        }

        preferences.edit()
            .putString(KEY_MEDIA_ID, mediaId)
            .putLong(KEY_POSITION_MS, positionMs)
            .putBoolean(KEY_SHUFFLE_ENABLED, player.shuffleModeEnabled)
            .putInt(KEY_REPEAT_MODE, player.repeatMode)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "playback_state"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_POSITION_MS = "position_ms"
        const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
        const val KEY_REPEAT_MODE = "repeat_mode"
    }
}
