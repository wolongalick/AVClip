package com.alick.avclip

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.widget.SeekBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityAudioClipBinding
import com.alick.avsdk.AudioClipUtils
import com.alick.avsdk.MediaParser
import com.alick.avsdk.bean.AudioBean
import com.alick.commonlibrary.BaseActivity
import com.alick.commonlibrary.UriUtils
import com.alick.utilslibrary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/13 13:46
 */
class AudioClipActivity : BaseActivity<ActivityAudioClipBinding>() {

    private val AUDIO_FILE_REQUEST_CODE = 1
    private lateinit var audioBean: AudioBean
    private val maxProgress = 100

    private val dialog: ProgressDialog by lazy {
        val progressDialog = ProgressDialog(this)
        progressDialog.progress = 0
        progressDialog.max = maxProgress
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog
    }


    override fun initListener() {
        viewBinding.btnImport.setOnClickListener {
            import()
        }


        viewBinding.btnParse.setOnClickListener {
            parse()
        }


        viewBinding.sbBegin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val durationOfSeconds: Int = (progress.toDouble() / seekBar.max * audioBean.durationOfMicroseconds / 1000_000L).toInt()
                viewBinding.tvBeginTimeValue.text = TimeFormatUtils.format(durationOfSeconds)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        viewBinding.sbEnd.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val durationOfSeconds: Int = (progress.toDouble() / seekBar.max * audioBean.durationOfMicroseconds / 1000_000L).toInt()
                viewBinding.tvBeginEndValue.text = TimeFormatUtils.format(durationOfSeconds)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        viewBinding.btnBegin.setOnClickListener {
            if (viewBinding.sbBegin.progress >= viewBinding.sbEnd.progress) {
                T.show("截取的起始时间应该小于结束时间")
                return@setOnClickListener
            }
            BLog.i("开始截取")
            val beginTime = System.currentTimeMillis()

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val outFile = File(getExternalFilesDir("output"), TimeUtils.getCurrentTime() + ".mp3")
                    AudioClipUtils.clip(
                        File(viewBinding.etSrcFilePath.text.toString().trim()),
                        outFile,
                        (viewBinding.sbBegin.progress.toDouble() / maxProgress * audioBean.durationOfMicroseconds).toLong(),
                        (viewBinding.sbEnd.progress.toDouble() / maxProgress * audioBean.durationOfMicroseconds).toLong(),
                    ) { progress: Long, max: Long ->

                        launch {
                            withContext(Dispatchers.Main) {
                                BLog.i("处理进度,progress:${progress},max:${max}")
                                dialog.progress = (progress.toDouble() / max * maxProgress).toInt()
                                if (!dialog.isShowing && progress < max) {
                                    dialog.show()
                                } else if (dialog.isShowing && progress >= max) {
                                    dialog.hide()
                                    //截取完成,输出所耗时长和文件输出路径
                                    viewBinding.tvSpendTimeValue.text = "${(System.currentTimeMillis() - beginTime) / 1000}秒"
                                    viewBinding.tvOutputPathValue.text = outFile.absolutePath
                                }
                            }
                        }

                    }
                }
            }
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
    }

    override fun initData() {
        val audioFilePath: String = StorageUtils.getString(SpConstant.AUDIO_FILE_PATH)
        viewBinding.etSrcFilePath.setText(audioFilePath)
    }

    /**
     * 导入文件
     */
    private fun import() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        //任意类型文件
        intent.type = "audio/mp3"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, AUDIO_FILE_REQUEST_CODE)
    }


    /**
     * 解析文件
     */
    private fun parse() {
        val filePath = viewBinding.etSrcFilePath.text.toString().trim()
        if (filePath.isBlank()) {
            T.show("文件路径不能为空")
            return
        }

        StorageUtils.setString(SpConstant.AUDIO_FILE_PATH, filePath)
        audioBean = MediaParser.parseAudio(filePath)
        val sb = StringBuilder()
        sb.append("音频采样率:${audioBean.sampleRate}\n")
            .append("比特率:${audioBean.bitrate}\n")
            .append("时长:${TimeFormatUtils.format((audioBean.durationOfMicroseconds / 1000_000L).toInt())}\n")
            .append("pcm编码:${audioBean.pcmEncoding}")

        viewBinding.etInfo.setText(sb.toString())
        setupSeekBar(audioBean.durationOfMicroseconds)
    }

    /**
     * 设置进度条的总长度
     */
    private fun setupSeekBar(durationOfMicroseconds: Long) {
        viewBinding.sbBegin.apply {
            max = maxProgress
            progress = 0
        }
        viewBinding.sbEnd.apply {
            max = maxProgress
            progress = 0
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AUDIO_FILE_REQUEST_CODE -> {
                val uri: Uri? = data?.data
                if (uri == null) {
                    Toast.makeText(this@AudioClipActivity, "选择的文件路径为空", Toast.LENGTH_SHORT).show()
                } else {
                    val filePath = UriUtils.uri2FilePath(this@AudioClipActivity, uri)
                    if (filePath == null) {
                        Toast.makeText(this@AudioClipActivity, "文件路径为空", Toast.LENGTH_SHORT).show()
                        return
                    }
                    viewBinding.etSrcFilePath.setText(filePath)
                    StorageUtils.setString(SpConstant.AUDIO_FILE_PATH, filePath)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }
}
