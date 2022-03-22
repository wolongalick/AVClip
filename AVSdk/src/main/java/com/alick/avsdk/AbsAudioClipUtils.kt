package com.alick.avsdk

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.lifecycle.LifecycleCoroutineScope
import com.alick.lamelibrary.LameUtils
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @createTime 2022/3/23 15:22
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
abstract class AbsAudioClipUtils(
    protected val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val inFile: File,
    private val outFile: File,
    protected val beginMicroseconds: Long,
    protected val endMicroseconds: Long,
    protected val onProgress: (progress: Long, max: Long) -> Unit,
    protected val onFinished: () -> Unit
) {
    private val bufferSize = 1024 * 256
    private var sampleRate: Int = 0
    private var bitRate: Int = 0
    private var channelCount: Int = 0
    private var maxBufferSize: Int = 0
    private var isEncoding = false
    protected val mediaExtractor = MediaExtractor()
    protected lateinit var mediaCodec: MediaCodec

    protected class BufferTask(val byteBuffer: ByteBuffer, val isEndOfStream: Boolean, val presentationTimeUs: Long)

    private val lameUtils by lazy {
        LameUtils().apply {
            init(
                outFile.absolutePath.replace(".mp3", ".pcm"), channelCount, bitRate, sampleRate, outFile.absolutePath.replace(".mp3", "_lame.mp3")
            )
        }
    }

    private val writeChannel: FileChannel by lazy {
        val pcmFile = File(outFile.parentFile, outFile.name.replace(".mp3", ".pcm"))
        FileUtils.createFile(pcmFile)
        FileOutputStream(pcmFile).channel
    }

    private val queue: BlockingQueue<BufferTask> by lazy {
        ArrayBlockingQueue(5000)
    }

    /**
     * 开始裁剪
     */
    abstract fun clip()

    /**
     * 运行协程:pcm转MP3
     */
    protected fun runPcmToMp3Coroutine() {
        lifecycleCoroutineScope.launch {
            withContext(Dispatchers.IO) {
                BLog.i("PcmToMp3Thread线程名:" + Thread.currentThread().name)
                var cacheBufferSize = 0
                isEncoding = true
                while (isEncoding) {
                    val bufferTask = queue.take()
                    val outputByteBuffer = bufferTask.byteBuffer
                    val wroteSize = writeChannel.write(outputByteBuffer)
                    cacheBufferSize += wroteSize

                    withContext(Dispatchers.Main) {
                        onProgress(bufferTask.presentationTimeUs - beginMicroseconds, endMicroseconds - beginMicroseconds)
                    }

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
            }
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
    }

    /**
     * 初始化编解码器
     */
    protected fun initMediaCodec(beginMicroseconds: Long): Pair<ByteBuffer, MediaCodec.BufferInfo> {
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
        maxBufferSize = if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            //使用从实际媒体格式中取出的实际值
            mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            //使用默认值
            100 * 1000
        }
        //创建解码器
        mediaCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME)!!)
        //配置解码器
        mediaCodec.configure(mediaFormat, null, null, 0)
        val buffer = ByteBuffer.allocateDirect(maxBufferSize)
        val inputBufferInfo = MediaCodec.BufferInfo()
        return Pair(buffer, inputBufferInfo)
    }

    /**
     * 处理输入缓冲队列
     */
    @SuppressLint("WrongConstant")
    protected fun disposeInputBuffer(
        inputBufferInfo: MediaCodec.BufferInfo,
        buffer: ByteBuffer,
        inputBuffer: ByteBuffer,
        index: Int
    ) {
        var isEndOfStream = false
        var isInRange = true
        val sampleTimeUs = mediaExtractor.sampleTime//时间戳,单位:微秒
        when {
            sampleTimeUs != -1L && sampleTimeUs < beginMicroseconds -> {
                //如果读取的时间戳小于设置的截取起始时间戳,则忽略,避免浪费时间
                mediaExtractor.advance()//读取下一帧数据
                isInRange = false
            }
            sampleTimeUs == -1L || sampleTimeUs >= endMicroseconds -> {
                isEndOfStream = true
            }
            else -> {
                //正常执行
                isEndOfStream = false
            }
        }

        if (isInRange) {
            inputBufferInfo.size = mediaExtractor.readSampleData(buffer, 0)
            inputBufferInfo.presentationTimeUs = sampleTimeUs
            inputBufferInfo.flags = mediaExtractor.sampleFlags
            inputBuffer.put(buffer)
            if (isEndOfStream) {
                mediaCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else {
                mediaCodec.queueInputBuffer(
                    index,
                    0,
                    inputBufferInfo.size,
                    inputBufferInfo.presentationTimeUs,
                    inputBufferInfo.flags
                )
            }
            //释放上一帧的压缩数据
            mediaExtractor.advance()
        }
    }

    /**
     * 处理输出缓冲队列
     */
    protected fun disposeOutputBuffer(outputBufferIndex: Int, outputBufferInfo: MediaCodec.BufferInfo) {
        val decodeOutputBuffer: ByteBuffer? = mediaCodec.getOutputBuffer(outputBufferIndex)
        decodeOutputBuffer?.let {
            //这里克隆一份新的ByteBuffer的原因是:如果不可隆,获取完ByteBuffer立即调用releaseOutputBuffer,会导致有杂音,而用克隆出来的ByteBuffer,再releaseOutputBuffer就不会有影响
            addPcmTask(
                BufferTask(
                    clone(it),
                    outputBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                    outputBufferInfo.presentationTimeUs
                )
            )
        }
        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
    }

    /**
     * 加入pcm任务
     */
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

    protected fun clone(original: ByteBuffer): ByteBuffer {
        val clone = ByteBuffer.allocate(original.remaining())
        original.rewind() //copy from the beginning
        clone.put(original)
        original.rewind()
        clone.flip()
        return clone
    }
}
