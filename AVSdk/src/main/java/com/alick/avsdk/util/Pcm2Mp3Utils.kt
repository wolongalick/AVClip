package com.alick.avsdk.util

import com.alick.avsdk.clip.BufferTask
import com.alick.lamelibrary.LameUtils
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.ThreadPoolManager
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @createTime 2022/4/14 16:41
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
open class Pcm2Mp3Utils(
    private val inPcmFile: File,
    private val outMp3File: File,
    private val channelCount: Int,
    private val bitRate: Int,
    private val sampleRate: Int,
    private val onFinished: () -> Unit,

    ) {
    private val lameUtils = LameUtils()
    private var isEncoding = false

    private val bufferSize = 1024 * 256

    private val queue: BlockingQueue<BufferTask> by lazy {
        ArrayBlockingQueue(5000)
    }

    private fun start() {
        isEncoding = true
        ThreadPoolManager.execute(object : Runnable {
            override fun run() {
                BLog.i("PcmToMp3Thread线程名:" + Thread.currentThread().name)
                var cacheBufferSize = 0
                while (isEncoding) {
                    val bufferTask = queue.take()//task方法为阻塞方法,只有当queue.put后,take方法才会获取到返回值
                    val wroteSize = bufferTask.wroteSize
                    cacheBufferSize += wroteSize
//                    BLog.i("wroteSize:${wroteSize},cacheBufferSize:${cacheBufferSize}")

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
                onFinished()
                BLog.i("lame释放资源")
                lameUtils.release()
            }
        })

    }

    /**
     * 加入pcm任务
     */
    fun addPcmTask(bufferTask: BufferTask) {
        if (!isEncoding) {
            start()
        }
        if (!lameUtils.isInitialized) {
            lameUtils.initialized(inPcmFile.absolutePath, channelCount, bitRate, sampleRate, outMp3File.absolutePath, "Pcm2Mp3Utils")
        }
        queue.put(bufferTask)
    }
}