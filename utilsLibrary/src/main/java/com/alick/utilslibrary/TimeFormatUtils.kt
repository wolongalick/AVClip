package com.alick.utilslibrary

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/13 20:43
 */
class TimeFormatUtils {
    companion object {

        /**
         * 格式化时间
         * @param durationOfSeconds 秒
         */
        fun format(durationOfSeconds: Int): String {
            val minutes = durationOfSeconds / 60
            val seconds = durationOfSeconds % 60
            return "${minutes}:${seconds}"
        }
    }
}