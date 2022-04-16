package com.alick.avclip.activity

import androidx.lifecycle.lifecycleScope
import com.alick.avclip.base.BaseAVActivity
import com.alick.avsdk.util.AVConstant
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityVideoAddBgmBinding
import com.alick.avclip.databinding.BottomOptionsBinding
import com.alick.avsdk.clip.AudioClipUtils
import com.alick.avsdk.clip.VideoClipUtils
import com.alick.avsdk.util.VideoAddBGMUtils
import com.alick.utilslibrary.*
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @createTime 2022/4/8 9:47
 * @author 崔兴旺  1607009565@qq.com
 * @description 为视频添加背景音乐
 */
class VideoAddBGMActivity : BaseAVActivity<ActivityVideoAddBgmBinding>() {

    private var isVideoAddBGMing = false    //是否正在为视频添加BGM
    private var clipVideoFile: File? = null
    private var clipAudioFile: File? = null


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

            //===================================视频======
            val inVideoFile = File(viewBinding.baseAudioInfo1.getSrcFilePath())
            val outVideoFile = File(getExternalFilesDir(AVConstant.OUTPUT_DIR), "视频裁剪-" + inVideoFile.name.substringBeforeLast(".") + "-" + TimeUtils.getCurrentTime() + ".mp4")


            VideoClipUtils(
                lifecycleScope,
                inVideoFile,
                outVideoFile,
                viewBinding.baseAudioInfo1.getBeginMicroseconds(),
                viewBinding.baseAudioInfo1.getEndMicroseconds(),
                onProgress = { progress: Long, max: Long ->
                    clipDialog.progress = (progress.toDouble() / max * maxProgress).toInt()
                }, onFinished = {
                    //截取完成,输出所耗时长和文件输出路径
                    clipVideoFile = outVideoFile
                    videoAddBGM(beginTime)
                }
            ).clip()

            //===================================音频======
            val inAudioFile = File(viewBinding.baseAudioInfo2.getSrcFilePath())
            val outAudioFile = File(AppHolder.getApp().getExternalFilesDir(AVConstant.OUTPUT_DIR), "音频裁剪-" + inAudioFile.name.substringBeforeLast(".") + "-" + TimeUtils.getCurrentTime() + ".mp3")

            AudioClipUtils(
                lifecycleScope,
                inAudioFile,
                outAudioFile,
                viewBinding.baseAudioInfo2.getBeginMicroseconds(),
                viewBinding.baseAudioInfo2.getEndMicroseconds(),
                onProgress = { progress: Long, max: Long ->
//                    BLog.i("音频截取进度,progress:${progress},max:${max}")
                    clipDialog.progress = (progress.toDouble() / max * maxProgress).toInt()
                }, onFinished = {
                    //截取完成,输出所耗时长和文件输出路径
                    clipAudioFile = outAudioFile
                    videoAddBGM(beginTime)
                }
            ).clip()
        }
    }


    private fun videoAddBGM(beginTime: Long) {
        if (clipVideoFile == null || clipAudioFile == null || isVideoAddBGMing) {
            return
        }
        isVideoAddBGMing = true
        val outMixMusicFile = File(getExternalFilesDir(AVConstant.OUTPUT_DIR), "视频添加BGM-" + clipVideoFile!!.name)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val videoAudioMix = VideoAddBGMUtils(clipVideoFile!!, clipAudioFile!!, outMixMusicFile, viewBinding.baseAudioInfo1.getVolume(), viewBinding.baseAudioInfo2.getVolume())
                try {
                    videoAudioMix.mix()
                    withContext(Dispatchers.Main) {
                        val duration = "${(System.currentTimeMillis() - beginTime) / 1000}秒"
                        T.show("为视频添加BGM成功!")
                        BLog.i("为视频添加BGM成功!,文件路径:${outMixMusicFile.absolutePath},总耗时:${duration}")
                        viewBinding.bottomOptions.tvOutputPathValue.text = outMixMusicFile.absolutePath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    BLog.i(e.stackTraceToString())
                    withContext(Dispatchers.Main) {
                        BLog.w(e.stackTraceToString())
                        T.show("为视频添加BGM失败:${e.message}")
                        clipDialog.dismiss()
                    }
                } finally {
                    isVideoAddBGMing = false
                }
            }
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