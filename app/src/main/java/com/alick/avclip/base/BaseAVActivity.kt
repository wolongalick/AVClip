package com.alick.avclip.base

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.viewbinding.ViewBinding
import com.alick.avclip.databinding.BottomOptionsBinding
import com.alick.avclip.uitl.IntentUtils
import com.alick.commonlibrary.BaseActivity
import com.alick.commonlibrary.UriTools
import com.alick.utilslibrary.*

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/22 22:43
 */
abstract class BaseAVActivity<Binding : ViewBinding> : BaseActivity<Binding>() {
    private val AV_FILE_REQUEST_CODE = 1

    private var sourceCode = 0

    companion object {
        const val SOURCE_CODE_1 = 1
        const val SOURCE_CODE_2 = 2
        const val SOURCE_CODE_3 = 3
        const val MAX_PROGRESS = 100
    }


    /**
     * 获取底部选项Binding
     */
    protected abstract fun getBottomOptionsBinding(): BottomOptionsBinding?

    protected val clipDialog: ProgressDialog by lazy {
        val progressDialog = ProgressDialog(this)
        progressDialog.progress = 0
        progressDialog.max = MAX_PROGRESS
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.setCancelable(true)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog
    }

    final override fun initListenerAfter() {
        super.initListenerAfter()
        val bottomOptionsBinding: BottomOptionsBinding? = getBottomOptionsBinding()
        bottomOptionsBinding?.let { binding ->
            binding.btnCopy.setOnClickListener {
                val path = binding.tvOutputPathValue.text.toString()
                if (path.isBlank()) {
                    T.show("路径为空")
                    return@setOnClickListener
                }
                EditTextUtils.copy2Clipboard(AppHolder.getApp(), path)
                T.show("复制成功")
            }

            binding.btnPlay.setOnClickListener {
                if (binding.tvOutputPathValue.text.toString().isBlank()) {
                    T.show("输出路径不能为空")
                    return@setOnClickListener
                }
                startActivity(IntentUtils.getAudioFileIntent(binding.tvOutputPathValue.text.toString()))
            }
        }

    }

    /**
     * 导入文件
     */
    fun importMP3(sourceCode: Int, mimeTypes: Array<String>) {
        this.sourceCode = sourceCode
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        //任意类型文件
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, AV_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AV_FILE_REQUEST_CODE -> {
                val uri: Uri? = data?.data
                if (uri == null) {
                    T.show("选择的文件路径为空")
                } else {
//                    val filePath = UriUtils.uri2FilePath(this@BaseAVActivity, uri)
                    val filePath = UriTools.getFileAbsolutePath(this@BaseAVActivity, uri)
                    if (filePath == null) {
                        T.show("文件路径为空")
                        return
                    }
                    BLog.i("选择的文件:${filePath}")
                    onImportMP3(sourceCode, filePath)
                }
            }
        }
    }

    protected open fun onImportMP3(sourceCode: Int, filePath: String) {

    }

    override fun onDestroy() {
        super.onDestroy()
        clipDialog.dismiss()
    }
}