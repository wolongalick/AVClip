package com.alick.avsdk.clip

import android.annotation.SuppressLint
import android.media.MediaCodec
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * @author 崔兴旺
 * @description 音频裁剪工具类
 * @date 2022/3/11 22:05
 */
class AudioClipUtils4Sync(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    inFile: File,
    outFile: File,
    beginMicroseconds: Long,
    endMicroseconds: Long,
    tag:String="AudioClipUtils4Sync",
    onProgress: (progress: Long, max: Long) -> Unit,
    onFinished: () -> Unit
) : AbsAudioClipUtils(
    lifecycleCoroutineScope,
    inFile,
    outFile,
    beginMicroseconds,
    endMicroseconds,
    tag,
    onProgress = onProgress,
    onFinished = onFinished
) {

    /**
     * 裁剪
     */
    @SuppressLint("WrongConstant")
    override fun clip() {
        //运行pcm转码MP3的协程
        runPcmToMp3Coroutine()
        lifecycleCoroutineScope.launch {
            withContext(Dispatchers.IO) {
                val (buffer, inputBufferInfo) = initMediaCodec(beginMicroseconds)
                //启动解码器
                mediaCodec.start()

                var outputBufferIndex: Int
                val outputBufferInfo = MediaCodec.BufferInfo()
                while (true) {
                    val index = mediaCodec.dequeueInputBuffer(10 * 1000)

                    if (index >= 0) {
                        val inputBuffer: ByteBuffer? = mediaCodec.getInputBuffer(index)
                        inputBuffer?.apply {
                            disposeInputBuffer(inputBufferInfo, buffer, inputBuffer, index)
                        }
                    }

                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(outputBufferInfo, 0)
                    while (outputBufferIndex >= 0) {
                        disposeOutputBuffer(outputBufferIndex, outputBufferInfo)
                        if (outputBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            return@withContext
                        }
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(outputBufferInfo, 0)
                    }
                }

            }
        }
    }
}