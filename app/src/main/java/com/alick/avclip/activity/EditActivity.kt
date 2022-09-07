package com.alick.avclip.activity

import com.alick.avclip.base.BaseAVActivity
import com.alick.avclip.constant.IntentKey
import com.alick.avclip.databinding.ActivityEditBinding
import com.alick.avclip.databinding.BottomOptionsBinding
import com.google.android.material.appbar.MaterialToolbar
import com.luck.picture.lib.entity.LocalMedia

/**
 * @createTime 2022/4/19 14:22
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
class EditActivity : BaseAVActivity<ActivityEditBinding>() {
    private val localMediaList: ArrayList<LocalMedia> by lazy {
        intent.getParcelableArrayListExtra<LocalMedia>(IntentKey.LOCAL_MEDIA) as ArrayList<LocalMedia>
    }

    /**
     * 获取底部选项Binding
     */
    override fun getBottomOptionsBinding(): BottomOptionsBinding? {
        return null
    }

    override fun getMaterialToolbar(): MaterialToolbar? {
        return null
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