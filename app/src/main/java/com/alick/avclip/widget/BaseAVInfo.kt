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
import com.alick.avsdk.bean.MediaBean
import com.alick.utilslibrary.T
import com.alick.utilslibrary.TimeFormatUtils
import java.io.File

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/22 21:50
 */
class BaseAVInfo : ConstraintLayout {

    private val viewBinding: LayoutBaseAudioInfoBinding = LayoutBaseAudioInfoBinding.inflate(LayoutInflater.from(context), this, true)

    private var mediaBean: MediaBean? = null
    var onClickImport: ((mimeTypes: Array<String>) -> Unit)? = null
    var onParseSuccess: ((filePath: String) -> Unit)? = null

    private var isEnableChangeVolume = false
    private var isEnableChangeOffset = false

    private enum class BAIMimeType(val mimeTypes: Array<String>, val hint: String) {
        OnlyAudio(arrayOf("audio/*"), "音频"),
        OnlyVideo(arrayOf("video/*"), "视频"),
        AudioAndVideo(arrayOf("audio/*", "video/*"), "音频或视频"),
    }

    private var mimeType = BAIMimeType.OnlyAudio

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.BaseAVInfo)
        isEnableChangeVolume = typeArray.getBoolean(R.styleable.BaseAVInfo_bai_isEnableChangeVolume, false)
        isEnableChangeOffset = typeArray.getBoolean(R.styleable.BaseAVInfo_bai_isEnableChangeOffset, false)
        mimeType = when (typeArray.getInt(R.styleable.BaseAVInfo_bai_mimeType, 0)) {
            BAIMimeType.OnlyAudio.ordinal -> {
                BAIMimeType.OnlyAudio
            }
            BAIMimeType.OnlyVideo.ordinal -> {
                BAIMimeType.OnlyVideo
            }
            BAIMimeType.AudioAndVideo.ordinal -> {
                BAIMimeType.AudioAndVideo
            }
            else -> {
                BAIMimeType.OnlyAudio
            }
        }
        typeArray.recycle()

        if (isInEditMode) {
            initListener()
            initView()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        initListener()
        initView()
    }

    private fun initListener() {
        viewBinding.btnImport.setOnClickListener {
            onClickImport?.invoke(mimeType.mimeTypes)
        }

        viewBinding.btnParse.setOnClickListener {
            parse(true)
        }

        viewBinding.sbOffsetTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mediaBean?.let {
                    val durationOfSeconds: Int = (progress.toDouble() / seekBar.max * it.durationOfMicroseconds / 1000_000L).toInt()
                    viewBinding.tvBeginLocationValue.text = TimeFormatUtils.format(durationOfSeconds)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        viewBinding.sbBegin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mediaBean?.let {
                    val durationOfSeconds: Int = (progress.toDouble() / seekBar.max * it.durationOfMicroseconds / 1000_000L).toInt()
                    viewBinding.tvBeginTimeValue.text = TimeFormatUtils.format(durationOfSeconds)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        viewBinding.sbEnd.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mediaBean?.let {
                    val durationOfSeconds: Int = (progress.toDouble() / seekBar.max * it.durationOfMicroseconds / 1000_000L).toInt()
                    viewBinding.tvBeginEndValue.text = TimeFormatUtils.format(durationOfSeconds)
                }
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
        viewBinding.etSrcFilePath.hint = "请输入${mimeType.hint}文件路径"
        if (isEnableChangeOffset) {
            viewBinding.tvBeginLocation.visibility = VISIBLE
            viewBinding.tvBeginLocationValue.visibility = VISIBLE
            viewBinding.sbOffsetTime.visibility = VISIBLE
        } else {
            viewBinding.tvBeginLocation.visibility = GONE
            viewBinding.tvBeginLocationValue.visibility = GONE
            viewBinding.sbOffsetTime.visibility = GONE
        }

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

        if(!File(filePath).exists()){
            if (isNeedToast) {
                T.show("文件不存在")
            }
            return
        }

        val info: String = if (filePath.endsWith(".mp3", true)) {
            parseAudio(filePath)
        } else if (filePath.endsWith(".mp4", true) || filePath.endsWith(".flv", true)) {
            parseVideo(filePath) + "\n" + parseAudio(filePath)
        } else {
            ""
        }
        if (info.isNotEmpty()) {
            viewBinding.etInfo.setText(info)
            mediaBean?.let {
                setupSeekBar(it.durationOfMicroseconds)
            }
            onParseSuccess?.invoke(filePath)
        } else {
            T.show("文件解析失败")
        }
    }

    private fun parseAudio(filePath: String): String {
        val sb = StringBuilder()
        MediaParser().parseAudio(filePath).let {
            mediaBean = it
            sb.append("音频流------\n")
                .append("时长:${TimeFormatUtils.format((it.durationOfMicroseconds / 1000_000L).toInt())}\n")
                .append("缓冲区最大尺寸:${it.maxInputSize}\n")
                .append("音频采样率:${it.sampleRate}\n")
                .append("比特率:${it.bitrate}\n")
                .append("pcm编码:${it.pcmEncoding}\n")
                .append("声道数:${it.channelCount}\n")
        }
        return sb.removeSuffix("\n").toString()
    }


    private fun parseVideo(filePath: String): String {
        val sb = StringBuilder()
        MediaParser().parseVideo(filePath).let {
            mediaBean = it
            sb.append("视频流------\n")
                .append("时长:${TimeFormatUtils.format((it.durationOfMicroseconds / 1000_000L).toInt())}\n")
                .append("缓冲区最大尺寸:${it.maxInputSize}\n")
                .append("宽:${it.width}\n")
                .append("高:${it.height}\n")
                .append("帧率:${it.frameRate}\n")
        }
        return sb.removeSuffix("\n").toString()
    }

    /**
     * 设置进度条的总长度
     */
    private fun setupSeekBar(durationOfMicroseconds: Long) {
        viewBinding.sbOffsetTime.apply {
            max = durationOfMicroseconds.toInt()
            progress = 0
        }
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
     * 获取偏移时间戳,单位:微秒
     */
    fun getOffsetMicroseconds(): Long {
        return viewBinding.sbOffsetTime.progress.toLong()
    }

    /**
     * 设置偏移时间戳,单位:微秒
     */
    fun setOffsetMicroseconds(offsetMicroseconds: Long) {
        viewBinding.sbOffsetTime.progress = offsetMicroseconds.toInt()
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
     * 设置开始时间戳,单位:微秒
     */
    fun setBeginMicroseconds(beginMicroseconds: Long) {
        viewBinding.sbBegin.progress = beginMicroseconds.toInt()
    }

    /**
     * 设置结束时间戳,单位:微秒
     */
    fun setEndMicroseconds(endMicroseconds: Long) {
        viewBinding.sbEnd.progress = endMicroseconds.toInt()
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