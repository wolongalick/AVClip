package com.alick.avclip.uitl

import android.content.Intent
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import com.alick.utilslibrary.AppHolder


/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/29 20:56
 */
class IntentUtils {
    companion object {
        //android获取一个用于打开音频文件的intent
        fun getAudioFileIntent(filePath: String): Intent {
            val intent = Intent("android.intent.action.VIEW")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("oneshot", 0)
            intent.putExtra("configchange", 0)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION) //允许临时的读和写

            val uri = UriUtils.getUriCompatibleN(AppHolder.getApp(), filePath)
            intent.setDataAndType(
                uri, when {
                    filePath.endsWith(".mp3",true) -> {
                        "audio/*"
                    }
                    filePath.endsWith(".mp4",true) -> {
                        "video/*"
                    }
                    else -> {
                        "*/*"
                    }
                }
            )
            return intent
        }
    }
}