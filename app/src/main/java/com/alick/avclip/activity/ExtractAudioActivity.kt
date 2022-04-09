package com.alick.avclip.activity

import com.alick.avclip.base.BaseAVActivity
import com.alick.avclip.databinding.ActivityExtractAudioBinding
import com.google.android.material.appbar.MaterialToolbar

/**
 * @createTime 2022/4/8 9:47
 * @author 崔兴旺  1607009565@qq.com
 * @description 从视频中提取音频
 */
class ExtractAudioActivity : BaseAVActivity<ActivityExtractAudioBinding>() {
    override fun getMaterialToolbar(): MaterialToolbar {
        return viewBinding.toolbar
    }

    /**
     * 初始化监听事件
     */
    override fun initListener() {

    }

    /**
     * 初始化数据
     */
    override fun initData() {

    }
}