package com.alick.avsdk

import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import com.alick.avsdk.bean.AudioBean

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/13 21:21
 */
class MediaParser {
    fun parseAudio(filePath: String): AudioBean {
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(filePath)

        val trackIndex = mediaExtractor.let {
            for (i in 0..it.trackCount) {
                val trackFormat = mediaExtractor.getTrackFormat(i)
                if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    return@let i
                }
            }
            return@let 0
        }

        mediaExtractor.selectTrack(trackIndex)
        val mediaFormat = mediaExtractor.getTrackFormat(trackIndex)

        val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val bitrate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        val durationOfMicroseconds = mediaFormat.getLong(MediaFormat.KEY_DURATION)//时长单位:微秒

        val encoding = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }
        val maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)//缓冲区最大尺寸
        val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        return AudioBean(
            sampleRate, bitrate, durationOfMicroseconds,encoding,maxInputSize,channelCount
        )

    }

}