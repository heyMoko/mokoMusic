package com.mokostudio.mokomusic.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorMessageTest {
    @Test
    fun networkErrorsSuggestCheckingConnectionAndRetrying() {
        val message = playbackErrorMessage(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)

        assertTrue(message.contains("네트워크"))
        assertTrue(message.contains("다시 시도"))
    }

    @Test
    fun missingAudioSuggestsSelectingAnotherTrack() {
        val message = playbackErrorMessage(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)

        assertTrue(message.contains("음원을 찾을 수 없어요"))
        assertTrue(message.contains("다른 곡"))
    }

    @Test
    fun unknownErrorsStillOfferRecoveryActions() {
        val message = playbackErrorMessage(PlaybackException.ERROR_CODE_UNSPECIFIED)

        assertTrue(message.contains("다시 시도"))
        assertTrue(message.contains("다른 곡"))
    }
}
