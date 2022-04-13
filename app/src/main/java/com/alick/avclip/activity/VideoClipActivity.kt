package com.alick.avclip.activity

import androidx.lifecycle.lifecycleScope
import com.alick.avclip.base.BaseAVActivity
import com.alick.avclip.constant.AVConstant
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityVideoClipBinding
import com.alick.avclip.databinding.BottomOptionsBinding
import com.alick.avsdk.clip.AudioClipUtils4Sync
import com.alick.avsdk.clip.video.VideoClipUtils
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.StorageUtils
import com.alick.utilslibrary.T
import com.alick.utilslibrary.TimeUtils
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

/**
 * @createTime 2022/4/13 8:52
 * @author 崔兴旺  1607009565@qq.com
 * @description 视频裁剪
 */
class VideoClipActivity : BaseAVActivity<ActivityVideoClipBinding>() {
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
            importMP3(0, mimeTypes)
        }

        viewBinding.baseAudioInfo1.onParseSuccess = {
            StorageUtils.setString(SpConstant.VIDEO_FILE_PATH_OF_CLIP, it)
        }

        viewBinding.bottomOptions.btnBegin.setOnClickListener {
            if (viewBinding.baseAudioInfo1.checkRange() <= 0) {
                T.show("截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }

            val beginTime = System.currentTimeMillis()
            val inFile = File(viewBinding.baseAudioInfo1.getSrcFilePath())
            val outFile =
                File(getExternalFilesDir(AVConstant.OUTPUT_DIR), "视频裁剪-"+inFile.name.substringBeforeLast(".") + "-" + TimeUtils.getCurrentTime() + ".mp4")
            if (!clipDialog.isShowing) {
                clipDialog.show()
            }

            VideoClipUtils(
                lifecycleScope,
                inFile,
                outFile,
                viewBinding.baseAudioInfo1.getBeginMicroseconds(),
                viewBinding.baseAudioInfo1.getEndMicroseconds(),
                onProgress = { progress: Long, max: Long ->
                    clipDialog.progress = (progress.toDouble() / max * maxProgress).toInt()
                }, onFinished = {
                    clipDialog.dismiss()
                    //截取完成,输出所耗时长和文件输出路径
                    val duration = "${(System.currentTimeMillis() - beginTime) / 1000}秒"
                    viewBinding.bottomOptions.tvSpendTimeValue.text = duration
                    BLog.i("视频裁剪完毕,文件路径:${outFile.absolutePath}")
                    BLog.i("总耗时:${duration}")
                    viewBinding.bottomOptions.tvOutputPathValue.text = outFile.absolutePath
                }
            ).clip()
        }
    }

    /**
     * 初始化数据
     */
    override fun initData() {
        val audioFilePath: String = StorageUtils.getString(SpConstant.VIDEO_FILE_PATH_OF_CLIP)
        viewBinding.baseAudioInfo1.setSrcFilePath(audioFilePath)
        viewBinding.baseAudioInfo1.parse()
    }

    override fun onImportMP3(sourceCode: Int, filePath: String) {
        super.onImportMP3(sourceCode, filePath)
        viewBinding.baseAudioInfo1.setSrcFilePath(filePath)
        viewBinding.baseAudioInfo1.parse(true)
    }
}