package com.mokostudio.mokomusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControllerConnectionUiStateTest {
    @Test
    fun retryMovesFailedConnectionBackToConnecting() {
        val failedState = ControllerConnectionUiState()
            .onFailed("연결 실패")

        val retryState = failedState.onConnecting()

        assertEquals(ControllerConnectionStatus.CONNECTING, retryState.status)
        assertNull(retryState.errorMessage)
    }

    @Test
    fun successfulConnectionClearsPreviousError() {
        val failedState = ControllerConnectionUiState()
            .onFailed("연결 실패")

        val connectedState = failedState.onConnected()

        assertEquals(ControllerConnectionStatus.CONNECTED, connectedState.status)
        assertNull(connectedState.errorMessage)
    }

    @Test
    fun failedConnectionExposesRetryMessage() {
        val failedState = ControllerConnectionUiState()
            .onFailed("다시 시도해 주세요")

        assertEquals(ControllerConnectionStatus.FAILED, failedState.status)
        assertEquals("다시 시도해 주세요", failedState.errorMessage)
    }
}
