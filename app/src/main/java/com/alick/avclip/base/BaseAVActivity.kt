package com.alick.avclip.base

import android.content.Intent
import android.net.Uri
import androidx.viewbinding.ViewBinding
import com.alick.commonlibrary.BaseActivity
import com.alick.commonlibrary.UriUtils
import com.alick.utilslibrary.T

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/22 22:43
 */
abstract class BaseAVActivity<Binding : ViewBinding> : BaseActivity<Binding>() {
    private val AUDIO_FILE_REQUEST_CODE = 1

    private var sourceCode = 0

    /**
     * 导入文件
     */
    fun importMP3(sourceCode: Int) {
        this.sourceCode=sourceCode
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        //任意类型文件
        intent.type = "audio/mp3"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, AUDIO_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AUDIO_FILE_REQUEST_CODE -> {
                val uri: Uri? = data?.data
                if (uri == null) {
                    T.show("选择的文件路径为空")
                } else {
                    val filePath = UriUtils.uri2FilePath(this@BaseAVActivity, uri)
                    if (filePath == null) {
                        T.show("文件路径为空")
                        return
                    }
                    onImportMP3(sourceCode,filePath)
                }
            }
        }
    }

    protected open fun onImportMP3(sourceCode:Int, filePath:String){

    }


}