package com.alick.avclip.activity

import androidx.lifecycle.lifecycleScope
import com.alick.avclip.base.BaseAVActivity
import com.alick.avclip.constant.AVConstant
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityVideoAddBgmBinding
import com.alick.avclip.databinding.BottomOptionsBinding
import com.alick.avclip.uitl.IntentUtils
import com.alick.avsdk.splice.AudioSpliceUtils4Sync
import com.alick.avsdk.util.AudioMix
import com.alick.utilslibrary.*
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

/**
 * @createTime 2022/4/8 9:47
 * @author 崔兴旺  1607009565@qq.com
 * @description 为视频添加背景音乐
 */
class VideoAddBGMActivity : BaseAVActivity<ActivityVideoAddBgmBinding>() {
    override fun getMaterialToolbar(): MaterialToolbar {
        return viewBinding.toolbar
    }

    /**
     * 初始化监听事件
     */
    override fun initListener() {
        viewBinding.baseAudioInfo1.apply {
            onClickImport = {
                importMP3(SOURCE_CODE_1, it)
            }
            onParseSuccess = {
                StorageUtils.setString(SpConstant.AUDIO_FILE_PATH_OF_VIDEO_ADD_BGM1, it)
            }
        }

        viewBinding.baseAudioInfo2.apply {
            onClickImport = {
                importMP3(SOURCE_CODE_2, it)
            }
            onParseSuccess = {
                StorageUtils.setString(SpConstant.AUDIO_FILE_PATH_OF_VIDEO_ADD_BGM2, it)
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

            val beginTime = System.currentTimeMillis()
            val outFile = File(getExternalFilesDir(AVConstant.OUTPUT_DIR), "音频混音-" + TimeUtils.getCurrentTime() + ".mp3")
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
                ), outFile = outFile,
                onGetTempOutPcmFileList = { outPcmFile: File, tempOutFileList: MutableList<File> ->
                    BLog.i("准备将多个pcm文件混音")
                    AudioMix.mixPcm(
                        tempOutFileList[0].absolutePath,
                        tempOutFileList[1].absolutePath,
                        outPcmFile.absolutePath,
                        viewBinding.baseAudioInfo1.getVolume(),
                        viewBinding.baseAudioInfo2.getVolume()
                    )
                    BLog.i("将多个pcm文件混音完毕,文件地址是:${outPcmFile.absolutePath}")
                },
                onProgress = { progress: Long, max: Long ->
//                    BLog.i("总进度:${progress}/${max}")
                    runOnUiThread {
                        clipDialog.progress = (progress.toDouble() / max * maxProgress).toInt()
                    }
                }, onFinished = {
                    clipDialog.dismiss()

                    //截取完成,输出所耗时长和文件输出路径
                    val duration = "${(System.currentTimeMillis() - beginTime) / 1000}秒"
                    viewBinding.bottomOptions.tvSpendTimeValue.text = duration
                    BLog.i("音频混音完毕,总耗时:${duration}")
                    viewBinding.bottomOptions.tvOutputPathValue.text = outFile.absolutePath
                }
            ).splice()
        }
    }

    /**
     * 初始化数据
     */
    override fun initData() {
        viewBinding.baseAudioInfo1.apply {
            setSrcFilePath(StorageUtils.getString(SpConstant.AUDIO_FILE_PATH_OF_VIDEO_ADD_BGM1))
            parse()
        }

        viewBinding.baseAudioInfo2.apply {
            setSrcFilePath(StorageUtils.getString(SpConstant.AUDIO_FILE_PATH_OF_VIDEO_ADD_BGM2))
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
        }
    }

    /**
     * 获取底部选项Binding
     */
    override fun getBottomOptionsBinding(): BottomOptionsBinding {
        return viewBinding.bottomOptions
    }
}