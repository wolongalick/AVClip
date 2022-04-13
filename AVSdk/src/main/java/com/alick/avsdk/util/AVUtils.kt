package com.alick.avsdk.util

import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/24 1:06
 */
class AVUtils {
    companion object {
        fun clone(original: ByteBuffer): ByteBuffer {
            val clone = ByteBuffer.allocate(original.remaining())
            original.rewind() //copy from the beginning
            clone.put(original)
            original.rewind()
            clone.flip()
            return clone
        }

        /**
         * @author 崔兴旺
         * @description
         * @date 2022/3/24 23:58
         */
        fun appendAll(outputFile: File, bufferSize: Int = 4096, files: MutableList<File>) {
            if (!outputFile.exists()) {
                throw NoSuchFileException(outputFile, null, "File doesn't exist.")
            }
            require(!outputFile.isDirectory) { "The file is a directory." }
            FileOutputStream(outputFile, true).use { output ->
                for (file in files) {
                    if (file.isDirectory || !file.exists()) {
                        continue // Might want to log or throw
                    }
                    file.forEachBlock(bufferSize) { buffer, bytesRead -> output.write(buffer, 0, bytesRead) }
                }
            }
        }

        /**
         * 从MediaFormat中获取最大缓冲区尺寸
         */
        fun getMaxInputSize(mediaFormat:MediaFormat):Int{
            return if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                //使用从实际媒体格式中取出的实际值
                mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                //使用默认值
                100 * 1000
            }
        }


    }
}