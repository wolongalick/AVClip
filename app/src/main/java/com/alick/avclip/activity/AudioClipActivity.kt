package com.alick.avclip.activity

import android.app.ProgressDialog
import androidx.lifecycle.lifecycleScope
import com.alick.avclip.base.BaseAVActivity
import com.alick.avclip.constant.AVConstant
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityAudioClipBinding
import com.alick.avclip.uitl.IntentUtils
import com.alick.avsdk.clip.AudioClipUtils4Sync
import com.alick.utilslibrary.*
import com.google.android.material.appbar.MaterialToolbar
import java.io.File


/**
 * @author 崔兴旺
 * @description 音频裁剪
 * @date 2022/3/13 13:46
 */
class AudioClipActivity : BaseAVActivity<ActivityAudioClipBinding>() {

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

    override fun initListener() {
        viewBinding.baseAudioInfo1.onClickImport = { mimeTypes: Array<String> ->
            importMP3(0, mimeTypes)
        }

        viewBinding.baseAudioInfo1.onParseSuccess = {
            StorageUtils.setString(SpConstant.AUDIO_FILE_PATH_OF_CLIP, it)
        }

        viewBinding.btnBegin.setOnClickListener {
            if (viewBinding.baseAudioInfo1.checkRange() <= 0) {
                T.show("截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }

            val beginTime = System.currentTimeMillis()
            val inFile = File(viewBinding.baseAudioInfo1.getSrcFilePath())
            val outFile =
                File(getExternalFilesDir(AVConstant.OUTPUT_DIR), inFile.name.substringBeforeLast(".") + "-" + TimeUtils.getCurrentTime() + ".mp3")
            if (!clipDialog.isShowing) {
                clipDialog.show()
            }
            AudioClipUtils4Sync(
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
                    viewBinding.tvSpendTimeValue.text = duration
                    BLog.i("音频裁剪完毕,文件路径:${outFile.absolutePath}")
                    BLog.i("总耗时:${duration}")
                    viewBinding.tvOutputPathValue.text = outFile.absolutePath
                }
            ).clip()
        }

        viewBinding.btnCopy.setOnClickListener {
            val path = viewBinding.tvOutputPathValue.text.toString()
            if (path.isBlank()) {
                T.show("路径为空")
                return@setOnClickListener
            }
            EditTextUtils.copy2Clipboard(AppHolder.getApp(), path)
            T.show("复制成功")
        }

        viewBinding.btnPlay.setOnClickListener {
            if (viewBinding.tvOutputPathValue.text.toString().isBlank()) {
                T.show("输出路径不能为空")
                return@setOnClickListener
            }
            startActivity(IntentUtils.getAudioFileIntent(viewBinding.tvOutputPathValue.text.toString()))
        }
    }

    override fun initData() {
        val audioFilePath: String = StorageUtils.getString(SpConstant.AUDIO_FILE_PATH_OF_CLIP)
        viewBinding.baseAudioInfo1.setSrcFilePath(audioFilePath)
        viewBinding.baseAudioInfo1.parse()
    }

    override fun onImportMP3(sourceCode: Int, filePath: String) {
        super.onImportMP3(sourceCode, filePath)
        viewBinding.baseAudioInfo1.setSrcFilePath(filePath)
        viewBinding.baseAudioInfo1.parse(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        clipDialog.dismiss()
    }

    override fun getMaterialToolbar(): MaterialToolbar {
        return viewBinding.toolbar
    }
}

