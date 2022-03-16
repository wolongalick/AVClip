package com.alick.utilslibrary

import android.util.Log

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/13 19:52
 */
class BLog {
    companion object {
        private val TAG = "alick"

        fun i(content: String, tag: String = TAG) {
            Log.i(tag, content)
        }

        fun e(content: String, tag: String = TAG) {
            Log.e(tag, content)
        }

        fun w(content: String, tag: String = TAG) {
            Log.w(tag, content)
        }
    }
}