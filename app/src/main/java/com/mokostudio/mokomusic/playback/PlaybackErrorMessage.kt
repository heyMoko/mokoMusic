package com.mokostudio.mokomusic.playback

import androidx.media3.common.PlaybackException

internal fun playbackErrorMessage(errorCode: Int): String = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    -> "연결에 문제가 있어 재생할 수 없어요. 네트워크를 확인하고 다시 시도해 주세요."

    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "음원을 찾을 수 없어요. 다른 곡을 선택하거나 다시 시도해 주세요."

    PlaybackException.ERROR_CODE_DECODING_FAILED ->
        "이 음원 형식은 재생할 수 없어요. 다른 곡을 선택해 주세요."

    else -> "재생 중 문제가 발생했어요. 다시 시도하거나 다른 곡을 선택해 주세요."
}
