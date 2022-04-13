package com.alick.avsdk.clip.video

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.lifecycle.LifecycleCoroutineScope
import com.alick.avsdk.util.AVUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer


/**
 * @createTime 2022/4/13 13:08
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
class VideoClipUtils(
    protected val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val inFile: File,
    private val outFile: File,
    protected val beginMicroseconds: Long,
    private val endMicroseconds: Long,
    private val tag: String = "VideoClipUtils",
    protected val onProgress: (progress: Long, max: Long) -> Unit,
    protected val onFinished: () -> Unit
) {

    private var videoIndex: Int = -1
    private var audioIndex: Int = -1

    private lateinit var videoFormat: MediaFormat
    private lateinit var audioFormat: MediaFormat


    private val mediaExtractor = MediaExtractor()
    private val mediaMuxer: MediaMuxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    fun clip() {
        lifecycleCoroutineScope.launch {
            withContext(Dispatchers.IO) {
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
                withContext(Dispatchers.Main) {
                    onFinished()
                }
                release()
            }
        }
    }


    @SuppressLint("WrongConstant")
    private suspend fun clip(mediaFormat: MediaFormat, trackIndex: Int) {
        mediaExtractor.selectTrack(trackIndex)
        val videoByteBuffer = ByteBuffer.allocateDirect(AVUtils.getMaxInputSize(mediaFormat))
        val videoInfo = MediaCodec.BufferInfo()
        while (true) {
            val readSize = mediaExtractor.readSampleData(videoByteBuffer, 0)
            if (readSize < 0) {
                mediaExtractor.unselectTrack(trackIndex)
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
                videoInfo.size = readSize
                videoInfo.presentationTimeUs = sampleTimeUs
                videoInfo.flags = mediaExtractor.sampleFlags

                mediaMuxer.writeSampleData(trackIndex, videoByteBuffer, videoInfo)
                mediaExtractor.advance()

                withContext(Dispatchers.Main) {
                    if (trackIndex == videoIndex) {
                        onProgress(((sampleTimeUs - beginMicroseconds) * 0.5).toLong(), endMicroseconds - beginMicroseconds)
                    } else if (trackIndex == audioIndex) {
                        onProgress(((endMicroseconds - beginMicroseconds) * 0.5 + (sampleTimeUs - beginMicroseconds) * 0.5).toLong(), endMicroseconds - beginMicroseconds)
                    }
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