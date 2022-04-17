package com.alick.avsdk.splice

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.lifecycle.LifecycleCoroutineScope
import com.alick.avsdk.clip.BufferTask
import com.alick.avsdk.util.AVUtils
import com.alick.ffmpeglibrary.FFmpegUtils
import com.alick.lamelibrary.LameUtils
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.FileUtils
import com.alick.utilslibrary.TimeFormatUtils
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
    protected val onGetTempOutPcmFileList: (outPcmFile: File, tempOutFileList: MutableList<ResampleAudioBean>, sampleRate: Int, channelCount: Int) -> Unit,
    protected val onProgress: (progress: Long, max: Long) -> Unit,
    protected val onFinished: () -> Unit,
    private val isNeedPcm2Mp3: Boolean = true,
) {

    //第二个音频播放偏移时间,单位:微秒
    private val secondFileOffsetTime: Long = if (inFileEachList.size > 1) {
        inFileEachList[1].offsetMicroseconds
    } else {
        0
    }

    /**
     * 第一个音频文件,时间戳与文件大小的映射关系
     */
    private var firstFileTimeOfSize: MutableMap<Long, Long>? = null

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
        val offsetMicroseconds: Long = 0L,
    )

    /**
     * 重采样后的音频类
     * @param file  重采样后的音频文件
     * @param timeLocation  音频时刻与文件当时的字节数(业务场景是,第二个音频播放的起始位置不在第0秒,因此需要记录)
     */
    data class ResampleAudioBean(val file: File, var timeLocation: Map<Long, Long>? = null)


    private val channelCount = 2        //暂时写死为双声道
    private var maxSampleRate = 0       //统一的采样率
    private var maxSampleRateIndex = 0  //统一的采样率对应的文件索引

    private val outPcmFile by lazy {
        val outPcmFile = File(outFile.absolutePath.replaceAfterLast(".mp3", ".pcm").replaceAfterLast(".mp4", ".pcm"))
        FileUtils.createFile(outPcmFile)
        outPcmFile
    }

    private val totalDurationMicroseconds = inFileEachList.let { it ->
        var total: Long = 0
        it.forEach { inFileEach ->
            total += inFileEach.endMicroseconds - inFileEach.beginMicroseconds
        }
        total
    }

    private val progressList: MutableList<Long> by lazy {
        mutableListOf<Long>().apply {
            repeat(inFileEachList.size) {
                add(0L)
            }
        }
    }

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

    private val tempResampleOutFileEachList = mutableListOf<ResampleAudioBean>()

    private val queueList by lazy {
        mutableListOf<BlockingQueue<BufferTask>>().apply {
            repeat(inFileEachList.size) {
                val queue: BlockingQueue<BufferTask> by lazy {
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
            //统一采样率，比特率和声道
            val bitRate: Int = paramsList[maxSampleRateIndex].bitRate
            val sampleRate: Int = paramsList[maxSampleRateIndex].sampleRate
            BLog.i("统一后的pcm参数,声道数:${channelCount},比特率:${bitRate},采样率:${sampleRate}")

            initialized(
                outPcmFile.absolutePath,
                channelCount,
                bitRate,
                sampleRate,
                outFile.absolutePath,
                "AudioSpliceUtils4Sync"
            )
        }
    }

    @SuppressLint("WrongConstant")
    fun splice() {
        runPcmToMp3Coroutine()
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
                    params.maxBufferSize = AVUtils.getMaxInputSize(mediaFormat)
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
                                var isInRange = true//是否在起始和结束时间戳范围内
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
                                    BufferTask(
                                        AVUtils.clone(it),
                                        0,
                                        outputBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                        outputBufferInfo.presentationTimeUs,
                                        inFileEachList[fileIndex].beginMicroseconds,
                                        inFileEachList[fileIndex].endMicroseconds,
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

            deferredList.forEachIndexed { _, deferred ->
                deferred.await()
            }
            BLog.i("所有MP3文件都已解码完毕")
        }
    }


    /**
     * 运行pcm转码MP3的协程
     */
    private fun runPcmToMp3Coroutine() {
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

                        progressByWritePcm(fileIndex, bufferTask)

                        if (bufferTask.isEndOfStream || cacheBufferSize >= params.bufferSize) {
                            //只有当达到流末尾时,或新增的文件大小达到一定程度时,才让lame来编码
                            cacheBufferSize = 0
                        }
                        if (bufferTask.isEndOfStream) {
                            params.isEncoding = false
                        }
                    }
                    BLog.i("第${fileIndex + 1}个用于输出pcm的协程已完成")
                    finish(fileIndex, params.mediaExtractor, params.mediaCodec)

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
                //找出最高采样率
                maxSampleRate = paramsList.first().sampleRate
                maxSampleRateIndex = 0
                paramsList.forEachIndexed { index, params ->
                    if (params.sampleRate > maxSampleRate) {
                        //以高采样率为准
                        maxSampleRate = params.sampleRate
                        maxSampleRateIndex = index
                    }
                }

                BLog.i("最高采样率为:${maxSampleRate},对应第${maxSampleRateIndex + 1}个文件:${inFileEachList[maxSampleRateIndex].inFile.name}")

                tempOutFileEachList.forEachIndexed { index, file ->
                    if (index == maxSampleRateIndex) {
                        tempResampleOutFileEachList.add(ResampleAudioBean(file))
                    } else {
                        val tempResampleOutFile = File(file.parent, file.name + ".resample")
                        FFmpegUtils().resample(
                            file.absolutePath,
                            tempResampleOutFile.absolutePath,
                            paramsList[index].sampleRate,
                            maxSampleRate,
                            channelCount,
                            channelCount,
                        )
                        tempResampleOutFileEachList.add(ResampleAudioBean(file))
                    }

                    if (secondFileOffsetTime > 0 && index == 1) {
                        tempResampleOutFileEachList[index].timeLocation = firstFileTimeOfSize
                    }
                }

                BLog.i(
                    "重采样后,临时pcm文件地址为:\n${
                        tempResampleOutFileEachList.joinToString(separator = "\n") {
                            it.file.absolutePath
                        }
                    }"
                )



                onGetTempOutPcmFileList(outPcmFile, tempResampleOutFileEachList, maxSampleRate, channelCount)

                if (isNeedPcm2Mp3) {
                    lameUtils.encode(object : LameUtils.Callback {
                        override fun onProgress(progress: Long, max: Long) {
                            progressByPcmToMp3(progress, max)
                        }
                    })
                    lameUtils.release()
                }
            }

            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
    }

    /**
     * 写入pcm文件的进度
     */
    private fun progressByWritePcm(fileIndex: Int, bufferTask: BufferTask) {
        val inFileEach = inFileEachList[fileIndex]
        progressList[fileIndex] = bufferTask.presentationTimeUs - inFileEach.beginMicroseconds
        onProgress(
            (progressList.let {
                var totalProcess: Long = 0
                it.forEach { progress ->
                    totalProcess += progress
                }
                return@let totalProcess
            } * 0.5).toLong(),
            totalDurationMicroseconds
        )

        //只有当当前为第一个文件,且第二个文件偏移时间大于0,且未给第一个文件设置时间与文件位置的关系时,才需要处理
        if (fileIndex == 0 && secondFileOffsetTime > 0 && firstFileTimeOfSize == null) {
            //时间刚好达到需要偏移的时间时,立刻记录时间与文件大小的关系
            if (bufferTask.presentationTimeUs >= secondFileOffsetTime) {
                firstFileTimeOfSize = mutableMapOf<Long, Long>().apply {
                    val byte = writeChannelList[fileIndex].size()
                    put(secondFileOffsetTime, byte)
                    BLog.i("第一个文件,时间与文件大小关系,${TimeFormatUtils.format((secondFileOffsetTime / 1000_000L).toInt())}对应${byte}字节")
                }
            }
        }

    }

    /**
     * pcm转MP3的进度
     * @param progress  已转换文件的大小
     * @param max       文件总大小
     */
    private fun progressByPcmToMp3(progress: Long, max: Long) {
        val scale: Double = totalDurationMicroseconds.toDouble() / max
        val progressDurationMicroseconds = (progress * scale * 0.5 + totalDurationMicroseconds * 0.5f).toLong()
//        BLog.i("progress:${progress},max:${max},totalDurationMicroseconds:${totalDurationMicroseconds},scale:${scale},progressDurationMicroseconds:${progressDurationMicroseconds}")
        onProgress(progressDurationMicroseconds, totalDurationMicroseconds)
    }


    private fun finish(
        fileIndex: Int,
        mediaExtractor: MediaExtractor,
        mediaCodec: MediaCodec
    ) {
        BLog.i("关闭、释放第${fileIndex + 1}个解码器资源")
        writeChannelList[fileIndex].close()
        mediaExtractor.release()
        mediaCodec.stop()
        mediaCodec.release()
    }
}

