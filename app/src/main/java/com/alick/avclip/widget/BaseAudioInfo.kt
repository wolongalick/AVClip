package com.alick.avclip.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.alick.avclip.R
import com.alick.avclip.databinding.LayoutBaseAudioInfoBinding
import com.alick.avsdk.MediaParser
import com.alick.avsdk.bean.AudioBean
import com.alick.utilslibrary.T
import com.alick.utilslibrary.TimeFormatUtils

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/22 21:50
 */
class BaseAudioInfo : ConstraintLayout {

    private val viewBinding: LayoutBaseAudioInfoBinding = LayoutBaseAudioInfoBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var audioBean: AudioBean
    var onClickImport: (() -> Unit)? = null
    var onParseSuccess: ((filePath: String) -> Unit)? = null

    private var isEnableChangeVolume = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.BaseAudioInfo)
        isEnableChangeVolume = typeArray.getBoolean(R.styleable.BaseAudioInfo_isEnableChangeVolume, false)
        typeArray.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        initListener()
        initView()
    }

    private fun initListener() {
        viewBinding.btnImport.setOnClickListener {
            onClickImport?.invoke()
        }

        viewBinding.btnParse.setOnClickListener {
            parse(true)
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

        if (isEnableChangeVolume) {
            viewBinding.sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    viewBinding.tvVolume.text = "音量:${progress}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }
    }

    private fun initView() {
        if (isEnableChangeVolume) {
            viewBinding.tvVolume.visibility = View.VISIBLE
            viewBinding.sbVolume.visibility = View.VISIBLE
        } else {
            viewBinding.tvVolume.visibility = View.GONE
            viewBinding.sbVolume.visibility = View.GONE
        }
    }

    /**
     * 解析文件
     */
    fun parse(isNeedToast: Boolean = false) {
        val filePath = viewBinding.etSrcFilePath.text.toString().trim()
        if (filePath.isBlank()) {
            if (isNeedToast) {
                T.show("文件路径不能为空")
            }
            return
        }

        audioBean = MediaParser().parseAudio(filePath)
        val sb = StringBuilder()
        sb.append("音频采样率:${audioBean.sampleRate}\n")
            .append("比特率:${audioBean.bitrate}\n")
            .append("时长:${TimeFormatUtils.format((audioBean.durationOfMicroseconds / 1000_000L).toInt())}\n")
            .append("pcm编码:${audioBean.pcmEncoding}\n")
            .append("缓冲区最大尺寸:${audioBean.maxInputSize}\n")
            .append("音频声道数:${audioBean.channelCount}")

        viewBinding.etInfo.setText(sb.toString())
        setupSeekBar(audioBean.durationOfMicroseconds)
        onParseSuccess?.invoke(filePath)
    }

    /**
     * 设置进度条的总长度
     */
    private fun setupSeekBar(durationOfMicroseconds: Long) {
        viewBinding.sbBegin.apply {
            max = durationOfMicroseconds.toInt()
            progress = 0
        }
        viewBinding.sbEnd.apply {
            max = durationOfMicroseconds.toInt()
            progress = 0
        }
    }

    /**
     * 检查范围
     */
    fun checkRange(): Int {
        return viewBinding.sbEnd.progress - viewBinding.sbBegin.progress
    }

    /**
     * 获取源文件路径
     */
    fun getSrcFilePath(): String {
        return viewBinding.etSrcFilePath.text.toString().trim()
    }

    /**
     * 获取开始时间戳,单位:微秒
     */
    fun getBeginMicroseconds(): Long {
        return viewBinding.sbBegin.progress.toLong()
    }

    /**
     * 获取结束时间戳,单位:微秒
     */
    fun getEndMicroseconds(): Long {
        return viewBinding.sbEnd.progress.toLong()
    }

    /**
     * 设置源文件路径
     */
    fun setSrcFilePath(srcFilePath: String) {
        viewBinding.etSrcFilePath.setText(srcFilePath)
    }

    /**
     * 获取音量
     */
    fun getVolume(): Int {
        return viewBinding.sbVolume.progress
    }

}