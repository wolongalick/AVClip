package com.alick.avsdk.util

import java.io.File
import java.io.FileOutputStream

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/24 23:58
 */
fun File.appendAll(bufferSize: Int = 4096,  files: MutableList<File>) {
    if (!exists()) {
        throw NoSuchFileException(this, null, "File doesn't exist.")
    }
    require(!isDirectory) { "The file is a directory." }
    FileOutputStream(this, true).use { output ->
        for (file in files) {
            if (file.isDirectory || !file.exists()) {
                continue // Might want to log or throw
            }
            file.forEachBlock(bufferSize) { buffer, bytesRead -> output.write(buffer, 0, bytesRead) }
        }
    }
}