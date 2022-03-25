package com.alick.avclip

import android.app.ProgressDialog
import androidx.lifecycle.lifecycleScope
import com.alick.avclip.base.BaseAVActivity
import com.alick.avclip.constant.AVConstant
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityAudioSpliceBinding
import com.alick.avsdk.clip.AudioClipUtils4Sync
import com.alick.avsdk.splice.AudioSpliceUtils4Sync
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.StorageUtils
import com.alick.utilslibrary.T
import com.alick.utilslibrary.TimeUtils
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

/**
 * @createTime 2022/3/22 18:11
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
class AudioSpliceActivity : BaseAVActivity<ActivityAudioSpliceBinding>() {

    companion object {
        private const val SOURCE_CODE_1 = 1
        private const val SOURCE_CODE_2 = 2

    }

    private val maxProgress = 100

    private val clipDialog: ProgressDialog by lazy {
        val progressDialog = ProgressDialog(this)
        progressDialog.progress = 0
        progressDialog.max = maxProgress
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog
    }

    override fun getMaterialToolbar(): MaterialToolbar = viewBinding.toolbar

    override fun initListener() {
        viewBinding.baseAudioInfo1.apply {
            onClickImport = {
                importMP3(SOURCE_CODE_1)
            }
            onParseSuccess = {
                StorageUtils.setString(SpConstant.AUDIO_FILE_PATH_OF_SPLICE1, it)
            }
        }

        viewBinding.baseAudioInfo2.apply {
            onClickImport = {
                importMP3(SOURCE_CODE_2)
            }
            onParseSuccess = {
                StorageUtils.setString(SpConstant.AUDIO_FILE_PATH_OF_SPLICE2, it)
            }
        }

        viewBinding.btnBegin.setOnClickListener {
            if (viewBinding.baseAudioInfo1.checkRange() <= 0) {
                T.show("第1个音频的截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }

            if (viewBinding.baseAudioInfo2.checkRange() <= 0) {
                T.show("第2个音频的截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }
            val beginTime = System.currentTimeMillis()
            val outFile = File(getExternalFilesDir(AVConstant.OUTPUT_DIR), "拼接-" + TimeUtils.getCurrentTime() + ".mp3")
            if (!clipDialog.isShowing) {
                clipDialog.show()
            }
            AudioSpliceUtils4Sync(
                lifecycleScope, mutableListOf(
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
                ), outFile, onProgress = { progress: Long, max: Long ->
                    BLog.i("进度:${progress}/${max}")
                    runOnUiThread {
                        clipDialog.progress = (progress.toDouble() / max * maxProgress).toInt()
                    }
                }, onFinished = {
                    clipDialog.hide()
                    //截取完成,输出所耗时长和文件输出路径
                    val duration = "${(System.currentTimeMillis() - beginTime) / 1000}秒"
                    viewBinding.tvSpendTimeValue.text = duration
                    BLog.i("总耗时:${duration}")
                    viewBinding.tvOutputPathValue.text = outFile.absolutePath
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
}