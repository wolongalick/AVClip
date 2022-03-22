package com.alick.avsdk

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaFormat
import androidx.lifecycle.LifecycleCoroutineScope
import com.alick.utilslibrary.BLog
import java.io.File
import java.nio.ByteBuffer

/**
 * @author 崔兴旺
 * @description 音频裁剪工具类
 * @date 2022/3/11 22:05
 */
class AudioClipUtils4Callback(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    inFile: File,
    outFile: File,
    beginMicroseconds: Long,
    endMicroseconds: Long,
    onProgress: (progress: Long, max: Long) -> Unit,
    onFinished: () -> Unit
) : AbsAudioClipUtils(
    lifecycleCoroutineScope,
    inFile,
    outFile,
    beginMicroseconds,
    endMicroseconds,
    onProgress = onProgress,
    onFinished = onFinished
) {


    /**
     * 裁剪
     */
    @SuppressLint("WrongConstant")
    override fun clip() {
        //运行pcm转码MP3的线程
        runPcmToMp3Coroutine()
        val (buffer, inputBufferInfo) = initMediaCodec(beginMicroseconds)
        mediaCodec.setCallback(object : MediaCodec.Callback() {
            /**
             * 当输入缓冲区可用时调用
             *
             * @param codec     MediaCodec对象
             * @param index     可用输入缓冲区的索引
             */
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                val inputBuffer: ByteBuffer? = codec.getInputBuffer(index)
                inputBuffer?.apply {
                    disposeInputBuffer(inputBufferInfo, buffer, inputBuffer, index)
                }
            }

            /**
             * 当输出缓冲区可用时调用
             *
             * @param codec     MediaCodec对象
             * @param outputBufferIndex     可用输出缓冲区的索引
             * @param outputBufferInfo      关于可用输出缓冲区的信息
             */
            override fun onOutputBufferAvailable(codec: MediaCodec, outputBufferIndex: Int, outputBufferInfo: MediaCodec.BufferInfo) {
                disposeOutputBuffer(outputBufferIndex, outputBufferInfo)
            }

            /**
             * 当MediaCodec遇到错误时调用
             *
             * @param codec     MediaCodec对象
             * @param e         描述错误。  .
             */
            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                BLog.e("当MediaCodec遇到错误:${e.message}")
            }

            /**
             * 当输出格式改变时调用
             *
             * @param codec     MediaCodec对象
             * @param format    新的输出格式。
             */
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                BLog.e("输出格式被改变,新的格式:${format}")
            }
        })
        //启动解码器
        mediaCodec.start()
    }


}