package com.cardlock.app.util

import android.os.Handler
import android.os.Looper

class AutoLockManager(
    private var timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val onAutoLockTriggered: () -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())
    private var isEnabled = true

    private val lockRunnable = Runnable {
        if (isEnabled) {
            onAutoLockTriggered()
        }
    }

    fun userInteraction() {
        handler.removeCallbacks(lockRunnable)
        if (isEnabled && timeoutMs > 0) {
            handler.postDelayed(lockRunnable, timeoutMs)
        }
    }

    fun updateTimeout(newTimeoutMs: Long) {
        timeoutMs = newTimeoutMs
        userInteraction()
    }

    fun stop() {
        handler.removeCallbacks(lockRunnable)
    }

    companion object {
        const val TIMEOUT_30_SEC = 30_000L
        const val TIMEOUT_1_MIN = 60_000L
        const val TIMEOUT_5_MIN = 300_000L
        const val DEFAULT_TIMEOUT_MS = TIMEOUT_1_MIN
    }
}
