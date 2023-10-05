package com.alick.utilslibrary

import android.os.Handler
import android.os.Looper

val mainHandler = Handler(Looper.getMainLooper())

/**
 * 在主线程中执行操作
 */
inline fun runOnMain(delay: Long = 0, crossinline action: () -> Unit) {
    when {
        delay != 0L -> {
            mainHandler.postDelayed({ action() }, delay)
        }

        Looper.getMainLooper() != Looper.myLooper() -> {
            mainHandler.post { action.invoke() }
        }

        else -> {
            action.invoke()
        }
    }
}