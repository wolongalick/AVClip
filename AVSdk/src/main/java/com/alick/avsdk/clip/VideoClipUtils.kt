package com.alick.avsdk.clip

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.alick.avsdk.util.AVUtils
import com.alick.utilslibrary.BLog
import java.io.File
import java.nio.ByteBuffer


/**
 * @createTime 2022/4/13 13:08
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
class VideoClipUtils(
    private val inFile: File,
    private val outFile: File,
    protected val beginMicroseconds: Long,
    private val endMicroseconds: Long,
    private val TAG: String = "VideoClipUtils",
    protected val onProgress: (progress: Long, max: Long) -> Unit,
    protected val onFinished: () -> Unit,
) {

    private var videoIndex: Int = -1
    private var audioIndex: Int = -1

    private lateinit var videoFormat: MediaFormat
    private lateinit var audioFormat: MediaFormat


    private val mediaExtractor = MediaExtractor()
    private val mediaMuxer: MediaMuxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    fun clip() {
        mediaExtractor.setDataSource(inFile.absolutePath)

        for (index in 0 until mediaExtractor.trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(index)
            if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                videoIndex = index
                mediaMuxer.addTrack(trackFormat)
                videoFormat = trackFormat
            } else if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioIndex = index
                mediaMuxer.addTrack(trackFormat)
                audioFormat = trackFormat
            }
        }

        mediaExtractor.seekTo(beginMicroseconds, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        mediaMuxer.start()

        clip(videoFormat, videoIndex)
        clip(audioFormat, audioIndex)
        BLog.i("正在视频裁剪,准备释放资源")
        release()
        onFinished()
    }


    @SuppressLint("WrongConstant")
    private fun clip(mediaFormat: MediaFormat, trackIndex: Int) {
        BLog.i("开始裁剪的媒体格式:${mediaFormat.getString(MediaFormat.KEY_MIME)}")
        mediaExtractor.selectTrack(trackIndex)
        val byteBuffer = ByteBuffer.allocateDirect(AVUtils.getMaxInputSize(mediaFormat))
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val readSize = mediaExtractor.readSampleData(byteBuffer, 0)
            if (readSize < 0) {
                return
            }

            var isInRange = true

            val sampleTimeUs = mediaExtractor.sampleTime
            when {
                sampleTimeUs != -1L && sampleTimeUs < beginMicroseconds -> {
                    //如果读取的时间戳小于设置的截取起始时间戳,则忽略,避免浪费时间
                    mediaExtractor.advance()//读取下一帧数据
                    isInRange = false
                }

                sampleTimeUs == -1L || sampleTimeUs >= endMicroseconds -> {
                    //结束
                    return
                }

                else -> {
                    //正常执行
                }
            }

            if (isInRange) {
                bufferInfo.size = readSize
                bufferInfo.presentationTimeUs = sampleTimeUs
                bufferInfo.flags = mediaExtractor.sampleFlags

                mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
                mediaExtractor.advance()

                val max = endMicroseconds - beginMicroseconds
                if (trackIndex == videoIndex) {
                    //这里成0.5的目的是:视频剪辑的进度,占进度条的前半部分
                    val progress = ((sampleTimeUs - beginMicroseconds) * 0.5).toLong()
                    onProgress(progress, max)
                } else if (trackIndex == audioIndex) {
                    //这里成0.5的目的是:视频剪辑的进度,占进度条的后半部分
                    val progress = (max * 0.5 + (sampleTimeUs - beginMicroseconds) * 0.5).toLong()
                    onProgress(progress, max)
                }
            }
        }
    }

    private fun release() {
        mediaMuxer.stop()
        mediaMuxer.release()
        mediaExtractor.release()
    }

}