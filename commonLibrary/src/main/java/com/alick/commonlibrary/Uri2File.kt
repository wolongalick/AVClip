package com.alick.commonlibrary

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import java.io.File
import java.lang.reflect.Method

/**
 * @author 崔兴旺
 * @description
 * @date 2022/4/9 18:49
 */
class Uri2File {
    companion object {

        fun uriToFile(context: Context, uri: Uri): File? {
            try {
                val docId = try {
                    DocumentsContract.getDocumentId(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        DocumentsContract.getTreeDocumentId(uri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                }
                val split = docId.split(":")
                val type = split[0]
                if (type.equals("primary", true)) {
                    return runCatching { File("${Environment.getExternalStorageDirectory()}/${split[1]}") }.getOrNull()
                } else {
                    val manager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                    val getVolumeList: Method = manager.javaClass.getMethod("getVolumeList")
                    val storageVolumeArray: Array<StorageVolume> = getVolumeList.invoke(manager) as Array<StorageVolume>
                    val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
                    val getUuidMethod = storageVolumeClazz.getMethod("getUuid")
                    val getPathMethod = try {
                        storageVolumeClazz.getMethod("getPath")
                    } catch (e: Exception) {
                        null
                    }

                    storageVolumeArray.forEach { storageVolume: StorageVolume ->
                        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
                            val uuid = getUuidMethod.invoke(storageVolume) as? String
                            if (uuid == type) {
                                val path :String= when {
                                    getPathMethod != null -> {
                                        getPathMethod.invoke(storageVolume)?.toString()?:""
                                    }
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                                        storageVolume.directory?.absolutePath?:""
                                    }
                                    else -> {
                                        ""
                                    }
                                }
                                return runCatching { File("${path}/${split[1]}") }.getOrNull()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}