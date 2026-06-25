package com.jacobleighty.musictracker.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DataChangeEvents {
    private val _flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val flow = _flow.asSharedFlow()
    fun emit() { _flow.tryEmit(Unit) }
}
