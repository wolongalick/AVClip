package com.alick.avsdk

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import com.alick.lamelibrary.LameUtils
import com.alick.utilslibrary.AppHolder
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author 崔兴旺
 * @description 音频裁剪工具类
 * @date 2022/3/11 22:05
 */
class AudioClipUtils(private val inFile: File, private val outFile: File, val onProgress: (progress: Long, max: Long) -> Unit, val onFinished: () -> Unit) {

    private val bufferSize = 1024 * 256
    private val dir = AppHolder.getApp().getExternalFilesDir("output")
    private var sampleRate: Int = 0
    private var bitRate: Int = 0
    private var channelCount: Int = 0
    private var isEncoding = false
    private val mediaExtractor = MediaExtractor()
    private lateinit var mediaCodec: MediaCodec

    private class BufferTask(val byteBuffer: ByteBuffer, val isEndOfStream: Boolean, val outputBufferIndex: Int)

    private val lameUtils by lazy {
        LameUtils().apply {
            init(
                outFile.absolutePath.replace(".mp3", ".pcm"), channelCount, bitRate, sampleRate, outFile.absolutePath.replace(".mp3", "_lame.mp3")
            )
        }
    }

    private val writeChannel by lazy {
        val pcmFile = File(outFile.parentFile, outFile.name.replace(".mp3", ".pcm"))
        FileUtils.createFile(pcmFile)
        FileOutputStream(pcmFile).channel
    }

    private val queue: BlockingQueue<BufferTask> by lazy {
        ArrayBlockingQueue(5000)
    }

    private val pcmToMp3Thread: Thread by lazy {
        var cacheBufferSize = 0
        Thread {
            isEncoding = true
            while (isEncoding) {
                val bufferTask = queue.take()
                val outputByteBuffer = bufferTask.byteBuffer
                val wroteSize = writeChannel.write(outputByteBuffer)
                cacheBufferSize += wroteSize
                BLog.i("wroteSize:${wroteSize},cacheBufferSize:${cacheBufferSize},bufferSize:${bufferSize}")
                if (bufferTask.isEndOfStream || cacheBufferSize >= bufferSize) {
                    //只有当达到流末尾时,或新增的文件大小达到一定程度时,才让lame来编码
                    lameUtils.encode(bufferTask.isEndOfStream)
                    cacheBufferSize = 0
                }
                if (bufferTask.isEndOfStream) {
                    isEncoding = false
                }
            }
            BLog.i("pcmToMp3Thread线程结束运行")
            lameUtils.destroy()
            finish(writeChannel, mediaExtractor, mediaCodec)
            onFinished()
        }
    }

    /**
     * 裁剪
     */
    @SuppressLint("WrongConstant")
    fun clip(beginMicroseconds: Long, endMicroseconds: Long) {
        BLog.i("开始截取,beginMicroseconds:${beginMicroseconds},endMicroseconds:${endMicroseconds}")
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


        val buffer = ByteBuffer.allocateDirect(maxBufferSize)
        //创建解码器
        mediaCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME)!!)

        val inputBufferInfo = MediaCodec.BufferInfo()

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
                    var isEndOfStream = false
                    val sampleTimeUs = mediaExtractor.sampleTime//时间戳,单位:微秒
//                    BLog.i("当前解码时间:${TimeFormatUtils.format((sampleTimeUs/1000_000).toInt())}")
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
             * @param outputBufferIndex     可用输出缓冲区的索引
             * @param info      关于可用输出缓冲区的信息
             */
            override fun onOutputBufferAvailable(codec: MediaCodec, outputBufferIndex: Int, info: MediaCodec.BufferInfo) {
//                BLog.i("index:${outputBufferIndex}--->onOutputBufferAvailable()")
                val decodeOutputBuffer: ByteBuffer? = codec.getOutputBuffer(outputBufferIndex)
//                BLog.i("outputBufferIndex:${outputBufferIndex},outputBufferInfo.flags:${info.flags},outputBufferInfo.presentationTimeUs:${info.presentationTimeUs}")
                decodeOutputBuffer?.let {
                    //这里克隆一份新的ByteBuffer的原因是:如果不可隆,获取完ByteBuffer立即调用releaseOutputBuffer,会导致有杂音,而是用克隆出来的ByteBuffer,再releaseOutputBuffer不会有影响
                    val tempBuffer: ByteBuffer = clone(it)
                    addPcmTask(BufferTask(tempBuffer, info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM, outputBufferIndex))
                }
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
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

        pcmToMp3Thread.start()

        //配置解码器
        mediaCodec.configure(mediaFormat, null, null, 0)
        //启动解码器
        mediaCodec.start()
    }


    private fun addPcmTask(bufferTask: BufferTask) {
        queue.put(bufferTask)
    }

    private fun finish(
        writeChannel: FileChannel,
        mediaExtractor: MediaExtractor,
        mediaCodec: MediaCodec
    ) {
        BLog.i("关闭、释放资源")
        writeChannel.close()
        mediaExtractor.release()
        mediaCodec.stop()
        mediaCodec.release()
    }

    fun clone(original: ByteBuffer): ByteBuffer {
        val clone = ByteBuffer.allocate(original.capacity())
        original.rewind() //copy from the beginning
        clone.put(original)
        original.rewind()
        clone.flip()
        return clone
    }
}