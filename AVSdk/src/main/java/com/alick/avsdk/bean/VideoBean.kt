package com.alick.avsdk.bean

/**
 * @createTime 2022/4/13 9:16
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
class VideoBean(
    durationOfSecond: Long,
    maxInputSize:Int,
    val width: Int,
    val height: Int,
    val frameRate: Int,
) : MediaBean(durationOfSecond,maxInputSize) {
}