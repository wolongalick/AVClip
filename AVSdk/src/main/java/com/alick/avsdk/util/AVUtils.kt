package com.alick.avsdk.util

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


    }
}