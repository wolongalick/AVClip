package com.alick.avclip

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.widget.SeekBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.alick.avclip.constant.AVConstant
import com.alick.avclip.constant.SpConstant
import com.alick.avclip.databinding.ActivityAudioClipBinding
import com.alick.avsdk.AudioClipUtils
import com.alick.avsdk.MediaParser
import com.alick.avsdk.bean.AudioBean
import com.alick.commonlibrary.BaseActivity
import com.alick.commonlibrary.UriUtils
import com.alick.lamelibrary.LameUtils
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
    private val MSG_TYPE_PCM_TRANSITION_MP3 = 1000

    private val clipDialog: ProgressDialog by lazy {
        val progressDialog = ProgressDialog(this)
        progressDialog.progress = 0
        progressDialog.max = maxProgress
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog
    }

    private val pcmTransitionMp3Dialog: ProgressDialog by lazy {
        val progressDialog = ProgressDialog(this)
        progressDialog.progress = 0
        progressDialog.max = maxProgress
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog
    }

    private val handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_TYPE_PCM_TRANSITION_MP3 -> {
                    val process = (maxProgress * ((msg.obj as Double))).toInt()
                    pcmTransitionMp3Dialog.progress = process
                    if (process == maxProgress) {
                        pcmTransitionMp3Dialog.hide()
                    } else if (!pcmTransitionMp3Dialog.isShowing) {
                        pcmTransitionMp3Dialog.show()
                    }
                }
            }
        }
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

            val beginTime = System.currentTimeMillis()

            val outFile = File(getExternalFilesDir(AVConstant.OUTPUT_DIR), TimeUtils.getCurrentTime() + ".mp3")
            AudioClipUtils(
                File(viewBinding.etSrcFilePath.text.toString().trim()),
                outFile
            ).clip(
                (viewBinding.sbBegin.progress.toDouble() / maxProgress * audioBean.durationOfMicroseconds).toLong(),
                (viewBinding.sbEnd.progress.toDouble() / maxProgress * audioBean.durationOfMicroseconds).toLong(),
                onProgress = { progress: Long, max: Long ->
                    BLog.i("处理进度,progress:${progress},max:${max}")
                    clipDialog.progress = (progress.toDouble() / max * maxProgress).toInt()
                    if (!clipDialog.isShowing) {
                        clipDialog.show()
                    }
                }, onFinished = {
                    clipDialog.hide()
                    //截取完成,输出所耗时长和文件输出路径
                    viewBinding.tvSpendTimeValue.text = "${(System.currentTimeMillis() - beginTime) / 1000}秒"
                    viewBinding.tvOutputPathValue.text = outFile.absolutePath

                    pcmTransitionMp3(File(outFile.absolutePath.replace(".mp3", ".pcm")), File(outFile.absolutePath.replace(".mp3", "_lame.mp3")))
                })
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
            .append("pcm编码:${audioBean.pcmEncoding}\n")
            .append("缓冲区最大尺寸:${audioBean.maxInputSize}\n")
            .append("音频声道数:${audioBean.channelCount}\n")

        viewBinding.etInfo.setText(sb.toString())
        setupSeekBar(audioBean.durationOfMicroseconds)
    }


    private fun pcmTransitionMp3(inFile: File, outFile: File) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val lameUtils = LameUtils()
                BLog.i("lame将pcm转换为MP3,准备开始")
                lameUtils.init(
                    inFile.absolutePath,
                    audioBean.channelCount,
                    audioBean.bitrate,
                    audioBean.sampleRate,
                    outFile.absolutePath
                )
                lameUtils.encode(object : LameUtils.Callback {
                    override fun onProgress(progress: Long, max: Long) {
                        BLog.i("pcm转MP3进度:${progress}/${max},当前线程:${Thread.currentThread().name}")

                        handler.sendMessage(handler.obtainMessage().apply {
                            this.what = MSG_TYPE_PCM_TRANSITION_MP3
                            this.obj = progress.toDouble() / max
                        })
                    }
                })
                lameUtils.destroy()
                BLog.i("lame将pcm转换为MP3,已完成")
            }
        }

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
        clipDialog.dismiss()
    }
}

