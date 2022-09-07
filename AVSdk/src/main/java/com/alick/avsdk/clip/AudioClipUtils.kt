package com.alick.avsdk.clip

import android.annotation.SuppressLint
import androidx.lifecycle.LifecycleCoroutineScope
import com.alick.avsdk.MediaParser
import com.alick.avsdk.util.AVUtils
import com.alick.avsdk.util.Pcm2Mp3Utils
import com.alick.utilslibrary.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author 崔兴旺
 * @description 音频裁剪工具类
 * @date 2022/3/11 22:05
 */
class AudioClipUtils(
    private val lifecycleCoroutineScope: LifecycleCoroutineScope,
    private val inFile: File,
    private val outMp3File: File,
    private val beginMicroseconds: Long,
    private val endMicroseconds: Long,
    private val onProgress: (progress: Long, max: Long) -> Unit,
    private val onFinished: () -> Unit
) {

    /**
     * 裁剪
     */
    fun clip() {
        //运行pcm转码MP3的协程
        lifecycleCoroutineScope.launch {
            withContext(Dispatchers.IO) {
                val outPcmFile = File(outMp3File.parentFile, outMp3File.name.replaceAfterLast(".", "pcm"))
                val audioBean = MediaParser().parseAudio(inFile.absolutePath)
                val pcm2Mp3Utils = Pcm2Mp3Utils(
                    lifecycleCoroutineScope,
                    outPcmFile,
                    outMp3File,
                    audioBean.channelCount,
                    audioBean.bitrate,
                    audioBean.sampleRate,
                    onProgress = onProgress,
                    onFinished = onFinished
                )
                AVUtils.extractPcm(
                    inFile,
                    outPcmFile,
                    beginMicroseconds,
                    endMicroseconds,
                    onProgress = { progress: Long, max: Long, percent: Float, bufferTask: BufferTask ->
                        pcm2Mp3Utils.addPcmTask(bufferTask)
                    },
                    onFinish = {

                    })
            }
        }
    }
}