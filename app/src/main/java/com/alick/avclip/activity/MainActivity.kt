package com.alick.avclip.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.alick.avclip.base.BaseAVActivity
import com.alick.avclip.constant.IntentKey
import com.alick.avclip.databinding.ActivityMainBinding
import com.alick.avclip.databinding.BottomOptionsBinding
import com.alick.avclip.uitl.GlideEngine
import com.alick.utilslibrary.T
import com.google.android.material.appbar.MaterialToolbar
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener


class MainActivity : BaseAVActivity<ActivityMainBinding>() {

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PermissionChecker.PERMISSION_GRANTED
                ) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ), 1000
                        )
                    }
                }

            } else {
                startActivity(Intent().apply { this.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION })
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), 1000
                )
            }
        }
    }

    override fun initListener() {
        viewBinding.tvAudioClip.setOnClickListener {
            startActivity(Intent(this@MainActivity, AudioClipActivity::class.java))
        }

        viewBinding.tvAudioSplice.setOnClickListener {
            startActivity(Intent(this@MainActivity, AudioSpliceActivity::class.java))
        }
        viewBinding.tvAudioMix.setOnClickListener {
            startActivity(Intent(this@MainActivity, AudioMixActivity::class.java))
        }
        viewBinding.tvVideoClip.setOnClickListener {
            startActivity(Intent(this@MainActivity, VideoClipActivity::class.java))
        }
        viewBinding.tvVideoAddBGM.setOnClickListener {
            startActivity(Intent(this@MainActivity, VideoAddBGMActivity::class.java))
        }
        viewBinding.tvExtractPcm.setOnClickListener {
            startActivity(Intent(this@MainActivity, ExtractPcmActivity::class.java))
        }
        viewBinding.tvBeginCreation.setOnClickListener {
            T.show("敬请期待")
            return@setOnClickListener
            PictureSelector.create(this)
                .openGallery(SelectMimeType.ofVideo())
                .setImageSpanCount(3)
                .setImageEngine(GlideEngine.createGlideEngine())
                .forResult(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: ArrayList<LocalMedia>) {
                        startActivity(
                            Intent(this@MainActivity, EditActivity::class.java).putParcelableArrayListExtra(
                                IntentKey.LOCAL_MEDIA, result
                            )
                        )
                    }

                    override fun onCancel() {

                    }
                })
        }
    }

    override fun initData() {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        grantResults.forEach {
            if (it == PermissionChecker.PERMISSION_DENIED) {
                Toast.makeText(this, "请授予读取SD卡权限", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    override fun getMaterialToolbar(): MaterialToolbar? = null
    override fun getBottomOptionsBinding(): BottomOptionsBinding? = null
}