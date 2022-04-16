package com.alick.avclip.activity

import androidx.lifecycle.lifecycleScope
import com.alick.avclip.base.BaseAVActivity
import com.alick.avsdk.util.AVConstant
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityExtractPcmBinding
import com.alick.avclip.databinding.BottomOptionsBinding
import com.alick.avsdk.clip.BufferTask
import com.alick.avsdk.util.AVUtils
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.StorageUtils
import com.alick.utilslibrary.T
import com.alick.utilslibrary.TimeUtils
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * @createTime 2022/4/14 13:49
 * @author 崔兴旺  1607009565@qq.com
 * @description 提取pcm
 */
class ExtractPcmActivity : BaseAVActivity<ActivityExtractPcmBinding>() {
    /**
     * 获取底部选项Binding
     */
    override fun getBottomOptionsBinding(): BottomOptionsBinding {
        return viewBinding.bottomOptions
    }

    override fun getMaterialToolbar(): MaterialToolbar {
        return viewBinding.toolbar
    }

    /**
     * 初始化监听事件
     */
    override fun initListener() {
        viewBinding.baseAudioInfo1.onClickImport = { mimeTypes: Array<String> ->
            importMP3(SOURCE_CODE_1, mimeTypes)
        }

        viewBinding.baseAudioInfo1.onParseSuccess = {
            StorageUtils.setString(SpConstant.FILE_PATH_OF_EXTRACT_PCM, it)
        }

        viewBinding.bottomOptions.btnBegin.setOnClickListener {
            if (viewBinding.baseAudioInfo1.checkRange() <= 0) {
                T.show("截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }
            val beginTime = System.currentTimeMillis()
            clipDialog.show()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val srcFilePath = viewBinding.baseAudioInfo1.getSrcFilePath()
                    val inFile = File(srcFilePath)
                    BLog.i("开始提取pcm")
                    val outFile = File(getExternalFilesDir(AVConstant.OUTPUT_DIR), "${inFile.name.removeSuffix(".mp3").removeSuffix(".mp4")}-${TimeUtils.getCurrentTime()}.pcm")
                    AVUtils.extractPcm(
                        inFile,
                        outFile,
                        beginTimeUs = viewBinding.baseAudioInfo1.getBeginMicroseconds(),
                        endTimeUs = viewBinding.baseAudioInfo1.getEndMicroseconds(),
                        onProgress = { progress: Long, max: Long, percent: Float,bufferTask: BufferTask ->
                            BLog.i("progress:${progress},max:${max},进度:${percent}")

                            runOnUiThread {
                                clipDialog.progress = (clipDialog.max*percent).roundToInt()
                            }
                        },
                        onFinish = {
                            runOnUiThread {
                                clipDialog.dismiss()
                                //截取完成,输出所耗时长和文件输出路径
                                val duration = "${(System.currentTimeMillis() - beginTime) / 1000}秒"
                                viewBinding.bottomOptions.tvSpendTimeValue.text = duration
                                BLog.i("pcm提取完毕,文件路径:${outFile.absolutePath}")
                                BLog.i("总耗时:${duration}")
                                viewBinding.bottomOptions.tvOutputPathValue.text = outFile.absolutePath
                            }
                        }
                    )
                }


            }
        }


    }

    /**
     * 初始化数据
     */
    override fun initData() {
        val audioFilePath: String = StorageUtils.getString(SpConstant.FILE_PATH_OF_EXTRACT_PCM)
        viewBinding.baseAudioInfo1.setSrcFilePath(audioFilePath)
        viewBinding.baseAudioInfo1.parse()
    }

    override fun onImportMP3(sourceCode: Int, filePath: String) {
        super.onImportMP3(sourceCode, filePath)
        viewBinding.baseAudioInfo1.setSrcFilePath(filePath)
        viewBinding.baseAudioInfo1.parse(true)
    }
}