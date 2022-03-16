package com.alick.avsdk.bean

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/13 21:18
 */
class AudioBean(
    val sampleRate: Int,
    val bitrate: Int,
    durationOfSecond: Long,
    val pcmEncoding:Int,
) : MediaBean(durationOfSecond) {

}