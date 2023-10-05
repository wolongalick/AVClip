package com.alick.avclip.activity

import androidx.lifecycle.lifecycleScope
import com.alick.avclip.base.BaseAVActivity
import com.alick.avsdk.util.AVConstant
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityAudioSpliceBinding
import com.alick.avclip.databinding.BottomOptionsBinding
import com.alick.avsdk.splice.AudioSpliceUtils4Sync
import com.alick.avsdk.util.AVUtils
import com.alick.utilslibrary.*
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

/**
 * @createTime 2022/3/22 18:11
 * @author 崔兴旺  1607009565@qq.com
 * @description 音频拼接
 */
class AudioSpliceActivity : BaseAVActivity<ActivityAudioSpliceBinding>() {
    override fun getMaterialToolbar(): MaterialToolbar = viewBinding.toolbar

    override fun initListener() {
        viewBinding.baseAudioInfo1.apply {
            onClickImport = {
                importMP3(SOURCE_CODE_1, it)
            }
            onParseSuccess = {
                StorageUtils.setString(SpConstant.AUDIO_FILE_PATH_OF_SPLICE1, it)
            }
        }

        viewBinding.baseAudioInfo2.apply {
            onClickImport = {
                importMP3(SOURCE_CODE_2, it)
            }
            onParseSuccess = {
                StorageUtils.setString(SpConstant.AUDIO_FILE_PATH_OF_SPLICE2, it)
            }
        }

        viewBinding.baseAudioInfo3.apply {
            onClickImport = {
                importMP3(SOURCE_CODE_3, it)
            }
            onParseSuccess = {
                StorageUtils.setString(SpConstant.AUDIO_FILE_PATH_OF_SPLICE3, it)
            }
        }

        viewBinding.bottomOptions.btnBegin.setOnClickListener {
            if (viewBinding.baseAudioInfo1.checkRange() <= 0) {
                T.show("第1个音频的截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }

            if (viewBinding.baseAudioInfo2.checkRange() <= 0) {
                T.show("第2个音频的截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }

            if (viewBinding.baseAudioInfo2.checkRange() < 0) {
                T.show("第3个音频的截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }

            val beginTime = System.currentTimeMillis()
            val outFile = File(getExternalFilesDir(AVConstant.OUTPUT_DIR), "拼接-" + TimeUtils.getCurrentTime() + ".mp3")
            if (!clipDialog.isShowing) {
                clipDialog.show()
            }
            AudioSpliceUtils4Sync(
                lifecycleCoroutineScope = lifecycleScope, inFileEachList = mutableListOf(
                    AudioSpliceUtils4Sync.InFileEach(
                        File(viewBinding.baseAudioInfo1.getSrcFilePath()),
                        viewBinding.baseAudioInfo1.getBeginMicroseconds(),
                        viewBinding.baseAudioInfo1.getEndMicroseconds()
                    ),
                    AudioSpliceUtils4Sync.InFileEach(
                        File(viewBinding.baseAudioInfo2.getSrcFilePath()),
                        viewBinding.baseAudioInfo2.getBeginMicroseconds(),
                        viewBinding.baseAudioInfo2.getEndMicroseconds()
                    ),
                    AudioSpliceUtils4Sync.InFileEach(
                        File(viewBinding.baseAudioInfo3.getSrcFilePath()),
                        viewBinding.baseAudioInfo3.getBeginMicroseconds(),
                        viewBinding.baseAudioInfo3.getEndMicroseconds()
                    ),
                ), outFile = outFile,
                onGetTempOutPcmFileList = { outPcmFile: File, tempOutFileList: MutableList<AudioSpliceUtils4Sync.ResampleAudioBean>, sampleRate: Int, channelCount: Int ->
                    BLog.i("准备将多个pcm文件合成")
                    AVUtils.appendAll(outPcmFile, files = tempOutFileList.map { it.file }.toMutableList())
                    BLog.i("将多个pcm文件合成完毕,文件地址是:${outPcmFile.absolutePath}")
                },
                onProgress = { progress: Long, max: Long ->
//                    BLog.i("总进度:${progress}/${max},maxProgress:${maxProgress}")
                    runOnUiThread {
                        clipDialog.progress = (progress.toDouble() / max * MAX_PROGRESS).toInt()
                    }
                }, onFinished = {
                    clipDialog.dismiss()
                    //截取完成,输出所耗时长和文件输出路径
                    val duration = "${(System.currentTimeMillis() - beginTime) / 1000}秒"
                    viewBinding.bottomOptions.tvSpendTimeValue.text = duration
                    BLog.i("音频拼接完毕,总耗时:${duration}")
                    BLog.i("音频拼接完毕,文件路径:${outFile.absolutePath}")
                    viewBinding.bottomOptions.tvOutputPathValue.text = outFile.absolutePath
                }
            ).splice()
        }

    }

    override fun initData() {
        viewBinding.baseAudioInfo1.apply {
            setSrcFilePath(StorageUtils.getString(SpConstant.AUDIO_FILE_PATH_OF_SPLICE1))
            parse()
        }

        viewBinding.baseAudioInfo2.apply {
            setSrcFilePath(StorageUtils.getString(SpConstant.AUDIO_FILE_PATH_OF_SPLICE2))
            parse()
        }
        viewBinding.baseAudioInfo3.apply {
            setSrcFilePath(StorageUtils.getString(SpConstant.AUDIO_FILE_PATH_OF_SPLICE3))
            parse()
        }
    }

    override fun onImportMP3(sourceCode: Int, filePath: String) {
        super.onImportMP3(sourceCode, filePath)
        when (sourceCode) {
            SOURCE_CODE_1 -> {
                viewBinding.baseAudioInfo1.apply {
                    setSrcFilePath(filePath)
                    parse(true)
                }
            }
            SOURCE_CODE_2 -> {
                viewBinding.baseAudioInfo2.apply {
                    setSrcFilePath(filePath)
                    parse(true)
                }
            }
            SOURCE_CODE_3 -> {
                viewBinding.baseAudioInfo3.apply {
                    setSrcFilePath(filePath)
                    parse(true)
                }
            }
        }

    }

    /**
     * 获取底部选项Binding
     */
    override fun getBottomOptionsBinding(): BottomOptionsBinding {
        return viewBinding.bottomOptions
    }
}