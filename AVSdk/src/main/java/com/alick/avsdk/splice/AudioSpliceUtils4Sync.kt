package com.alick.avsdk.splice

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.lifecycle.LifecycleCoroutineScope
import com.alick.avsdk.clip.AbsAudioClipUtils
import com.alick.avsdk.util.AVUtils
import com.alick.avsdk.util.appendAll
import com.alick.lamelibrary.LameUtils
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.FileUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/24 0:21
 */
open class AudioSpliceUtils4Sync(
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val inFileEachList: MutableList<InFileEach>,
    private val outFile: File,
    protected val onProgress: (progress: Long, max: Long) -> Unit,
    protected val onFinished: () -> Unit,
) {
    class Params {
        val bufferSize = 1024 * 256
        var sampleRate: Int = 0
        var bitRate: Int = 0
        var channelCount: Int = 0
        var maxBufferSize: Int = 0
        var isEncoding = false
        val mediaExtractor = MediaExtractor()
        lateinit var mediaCodec: MediaCodec
    }

    /**
     * 每段音频文件
     */
    class InFileEach(
        val inFile: File,
        val beginMicroseconds: Long,
        val endMicroseconds: Long,
    )

    private val paramsList by lazy {
        mutableListOf<Params>().apply {
            repeat(inFileEachList.size) {
                add(Params())
            }
        }
    }

    private val tempOutFileEachList: MutableList<File> by lazy {
        mutableListOf<File>().apply {
            inFileEachList.forEachIndexed { index, _ ->
                add(File(outFile.parent, "${outFile.name}.temp${index + 1}"))
            }
        }
    }

    private val queueList by lazy {
        mutableListOf<BlockingQueue<AbsAudioClipUtils.BufferTask>>().apply {
            repeat(inFileEachList.size) {
                val queue: BlockingQueue<AbsAudioClipUtils.BufferTask> by lazy {
                    ArrayBlockingQueue(5000)
                }
                add(queue)
            }
        }
    }

    private val writeChannelList by lazy {
        mutableListOf<FileChannel>().apply {
            inFileEachList.forEachIndexed { index, _ ->
                val pcmFile = tempOutFileEachList[index]
                FileUtils.createFile(pcmFile)
                val writeChannel = FileOutputStream(pcmFile).channel
                add(writeChannel)
            }
        }
    }

    private val lameUtils by lazy {
        LameUtils().apply {
            init(
                tempOutFileEachList.first().absolutePath,
                paramsList.first().channelCount,
                paramsList.first().bitRate,
                paramsList.first().sampleRate,
                outFile.absolutePath.replace(".mp3", "_Splice.mp3"),
                "AudioSpliceUtils4Sync"
            )
        }
    }

    @SuppressLint("WrongConstant")
    fun splice() {
        lifecycleCoroutineScope.launch {
            BLog.i("开启多个用于输出pcm文件的协程")
            val deferredList = mutableListOf<Deferred<Unit>>()
            inFileEachList.forEachIndexed { fileIndex, _ ->
                val deferred = async(Dispatchers.IO) {
                    BLog.i("第${fileIndex + 1}个用于输出pcm的协程已启动")
                    var cacheBufferSize = 0
                    val params = paramsList[fileIndex]
                    params.isEncoding = true
                    while (params.isEncoding) {
                        val bufferTask = queueList[fileIndex].take()
                        val outputByteBuffer = bufferTask.byteBuffer
                        val wroteSize = writeChannelList[fileIndex].write(outputByteBuffer)
                        cacheBufferSize += wroteSize

                        /*withContext(Dispatchers.Main) {
                            val inFileEach = inFileEachList[fileIndex]
                            onProgress(
                                bufferTask.presentationTimeUs - inFileEach.beginMicroseconds,
                                inFileEach.endMicroseconds - inFileEach.beginMicroseconds
                            )
                        }*/

                        if (bufferTask.isEndOfStream || cacheBufferSize >= params.bufferSize) {
                            //只有当达到流末尾时,或新增的文件大小达到一定程度时,才让lame来编码
                            cacheBufferSize = 0
                        }
                        if (bufferTask.isEndOfStream) {
                            params.isEncoding = false
                        }
                    }
                    BLog.i("pcmToMp3Thread线程结束运行")
                    finish(writeChannelList[fileIndex], params.mediaExtractor, params.mediaCodec)

                }
                deferredList.add(deferred)
            }
            deferredList.forEachIndexed { index, deferred ->
                BLog.i("等待第${index + 1}个pcm输出")
                deferred.await()
                BLog.i("第${index + 1}个pcm输出完成")
            }
            BLog.i(
                "至此,所有pcm文件均已输出完毕,临时pcm文件地址为:\n${
                    tempOutFileEachList.joinToString(separator = "\n") {
                        it.absolutePath
                    }
                }"
            )

            withContext(Dispatchers.IO) {
                BLog.i("准备将多个pcm文件合成")
                val firstPcmTempFile: File = tempOutFileEachList.first()
                firstPcmTempFile.appendAll(files = tempOutFileEachList.subList(1, tempOutFileEachList.size))
                BLog.i("将多个pcm文件合成完毕,文件地址是:${firstPcmTempFile.absolutePath}")
                lameUtils.encode(object : LameUtils.Callback{
                    override fun onProgress(progress: Long, max: Long) {
                        onProgress.invoke(progress,max)
                    }
                })
                lameUtils.destroy()
            }

            withContext(Dispatchers.Main){
                onFinished()
            }
        }

        lifecycleCoroutineScope.launch {
            BLog.i("开启多个用于解码MP3文件的协程")
            val deferredList = mutableListOf<Deferred<Unit>>()
            inFileEachList.forEachIndexed { fileIndex, inFileEach ->
                //运行pcm转码MP3的协程
                val deferred = async(Dispatchers.IO) {
                    val params = paramsList[fileIndex]
                    val mediaExtractor = params.mediaExtractor

                    mediaExtractor.setDataSource(inFileEach.inFile.absolutePath)
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
                    mediaExtractor.seekTo(inFileEach.beginMicroseconds, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    val mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
                    params.sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    params.bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)
                    params.channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    params.maxBufferSize = if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        //使用从实际媒体格式中取出的实际值
                        mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    } else {
                        //使用默认值
                        100 * 1000
                    }
                    //创建解码器
                    params.mediaCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME)!!)
                    //配置解码器
                    params.mediaCodec.configure(mediaFormat, null, null, 0)
                    val buffer = ByteBuffer.allocateDirect(params.maxBufferSize)
                    val inputBufferInfo = MediaCodec.BufferInfo()

                    //启动解码器
                    params.mediaCodec.start()

                    var outputBufferIndex: Int
                    val outputBufferInfo = MediaCodec.BufferInfo()
                    while (true) {
                        val index = params.mediaCodec.dequeueInputBuffer(10 * 1000)

                        if (index >= 0) {

                            val inputBuffer: ByteBuffer? = params.mediaCodec.getInputBuffer(index)
                            inputBuffer?.apply {
                                var isEndOfStream = false
                                var isInRange = true
                                val sampleTimeUs = mediaExtractor.sampleTime//时间戳,单位:微秒
                                when {
                                    sampleTimeUs != -1L && sampleTimeUs < inFileEach.beginMicroseconds -> {
                                        //如果读取的时间戳小于设置的截取起始时间戳,则忽略,避免浪费时间
                                        mediaExtractor.advance()//读取下一帧数据
                                        isInRange = false
                                    }
                                    sampleTimeUs == -1L || sampleTimeUs >= inFileEach.endMicroseconds -> {
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
                                        params.mediaCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    } else {
                                        params.mediaCodec.queueInputBuffer(
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
                        }

                        outputBufferIndex = params.mediaCodec.dequeueOutputBuffer(outputBufferInfo, 0)
                        while (outputBufferIndex >= 0) {
                            val decodeOutputBuffer: ByteBuffer? = params.mediaCodec.getOutputBuffer(outputBufferIndex)
                            decodeOutputBuffer?.let {
                                //这里克隆一份新的ByteBuffer的原因是:如果不可隆,获取完ByteBuffer立即调用releaseOutputBuffer,会导致有杂音,而用克隆出来的ByteBuffer,再releaseOutputBuffer就不会有影响
                                queueList[fileIndex].put(
                                    AbsAudioClipUtils.BufferTask(
                                        AVUtils.clone(it),
                                        outputBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                        outputBufferInfo.presentationTimeUs
                                    )
                                )
                            }
                            params.mediaCodec.releaseOutputBuffer(outputBufferIndex, false)

                            if (outputBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                return@async
                            }
                            outputBufferIndex = params.mediaCodec.dequeueOutputBuffer(outputBufferInfo, 0)
                        }
                    }
                }
                deferredList.add(deferred)
            }

            deferredList.forEachIndexed { index, deferred ->
                deferred.await()
            }
            BLog.i("所有MP3文件都已解码完毕")
        }
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
}

