package com.bez.spinwheel_sdk.domain.model

sealed class WheelResult<out T> {
    data object Loading : WheelResult<Nothing>()
    data class Success<T>(val data: T) : WheelResult<T>()
    data class Error(val exception: Throwable) : WheelResult<Nothing>()
}
