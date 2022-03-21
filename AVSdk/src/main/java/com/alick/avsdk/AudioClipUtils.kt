package com.alick.avsdk

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import com.alick.utilslibrary.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * @author 崔兴旺
 * @description 音频裁剪工具类
 * @date 2022/3/11 22:05
 */
class AudioClipUtils(private val inFile: File, private val outFile: File) {


    val dir = AppHolder.getApp().getExternalFilesDir("output")
    var sampleRate: Int = 0
    var bitRate: Int = 0
    var channelCount: Int = 0

    /**
     * 裁剪
     */
    @SuppressLint("WrongConstant")
    fun clip(beginMicroseconds: Long, endMicroseconds: Long, onProgress: (progress: Long, max: Long) -> Unit, onFinished: () -> Unit) {
        BLog.i("开始截取,beginMicroseconds:${beginMicroseconds},endMicroseconds:${endMicroseconds}")
        val pcmFile = File(outFile.parentFile, outFile.name.replace(".mp3", ".pcm"))

        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(inFile.absolutePath)
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
        mediaExtractor.seekTo(beginMicroseconds, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
        sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val encoding = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }

        val maxBufferSize = if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            //使用从实际媒体格式中取出的实际值
            mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            //使用默认值
            100 * 1000
        }

        FileUtils.createFile(pcmFile)
        val writeChannel = FileOutputStream(pcmFile).channel


        val buffer = ByteBuffer.allocateDirect(maxBufferSize)
        //创建解码器
        val mediaCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME)!!)

        val inputBufferInfo = MediaCodec.BufferInfo()

        mediaCodec.setCallback(object : MediaCodec.Callback() {
            /**
             * 当输入缓冲区可用时调用
             *
             * @param codec     MediaCodec对象
             * @param index     可用输入缓冲区的索引
             */
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                BLog.i("index:${index}--->onInputBufferAvailable()")
                val inputBuffer: ByteBuffer? = codec.getInputBuffer(index)

                inputBuffer?.apply {
                    var isEndOfStream = false
                    val sampleTimeUs = mediaExtractor.sampleTime//时间戳,单位:微秒
                    BLog.i("sampleTimeUs:${sampleTimeUs}")
                    when {
                        sampleTimeUs != -1L && sampleTimeUs < beginMicroseconds -> {
                            //如果读取的时间戳小于设置的截取起始时间戳,则忽略,避免浪费时间
                            mediaExtractor.advance()//读取下一帧数据
                            return@apply
                        }
                        sampleTimeUs == -1L || sampleTimeUs >= endMicroseconds -> {
                            isEndOfStream = true
                        }
                        else -> {
                            onProgress(sampleTimeUs - beginMicroseconds, endMicroseconds - beginMicroseconds)
                        }
                    }

                    inputBufferInfo.size = mediaExtractor.readSampleData(buffer, 0)
                    inputBufferInfo.presentationTimeUs = sampleTimeUs
                    inputBufferInfo.flags = mediaExtractor.sampleFlags

                    val content = ByteArray(buffer.remaining())
                    buffer.get(content)

                    val inputBuffer = mediaCodec.getInputBuffer(index)
                    inputBuffer!!.put(content)
                    if (isEndOfStream) {
                        mediaCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        mediaCodec.queueInputBuffer(index, 0, inputBufferInfo.size, inputBufferInfo.presentationTimeUs, inputBufferInfo.flags)
                    }

                    //释放上一帧的压缩数据
                    mediaExtractor.advance()
                }
            }

            /**
             * 当输出缓冲区可用时调用
             *
             * @param codec     MediaCodec对象
             * @param index     可用输出缓冲区的索引
             * @param info      关于可用输出缓冲区的信息
             */
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                BLog.i("index:${index}--->onOutputBufferAvailable()")
                val decodeOutputBuffer: ByteBuffer? = codec.getOutputBuffer(index)
                BLog.i("outputBufferIndex:${index},outputBufferInfo.flags:${info.flags},outputBufferInfo.presentationTimeUs:${info.presentationTimeUs}")
                writeChannel.write(decodeOutputBuffer)
                codec.releaseOutputBuffer(index, false)

                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    finish(writeChannel, mediaExtractor, mediaCodec, sampleRate, channelCount, encoding, pcmFile, outFile)
                    onFinished()
                }
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

        //配置解码器
        mediaCodec.configure(mediaFormat, null, null, 0)
        //启动解码器
        mediaCodec.start()
    }

    private fun finish(
        writeChannel: FileChannel,
        mediaExtractor: MediaExtractor,
        mediaCodec: MediaCodec,
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        pcmFile: File,
        outFile: File
    ) {
        BLog.i("关闭、释放资源")
        writeChannel.close()
        mediaExtractor.release()
        mediaCodec.stop()
        mediaCodec.release()
    }


}