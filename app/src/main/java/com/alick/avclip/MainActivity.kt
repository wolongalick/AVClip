package com.alick.avclip

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.alick.avclip.databinding.ActivityMainBinding
import com.alick.commonlibrary.BaseActivity


class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
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
        viewBinding.tvAudio.setOnClickListener {
            startActivity(Intent(this@MainActivity, AudioClipActivity::class.java))
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
}