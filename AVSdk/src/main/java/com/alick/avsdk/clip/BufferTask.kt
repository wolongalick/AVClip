package com.alick.avsdk.clip

import java.nio.ByteBuffer

class BufferTask(
    val byteBuffer: ByteBuffer?,
    val wroteSize: Int,
    val isEndOfStream: Boolean,
    val presentationTimeUs: Long,
    val beginMicroseconds: Long,
    val endMicroseconds: Long,
) {
    companion object {
        fun createEmpty(
            presentationTimeUs: Long, beginMicroseconds: Long, endMicroseconds: Long,
        ): BufferTask {
            return BufferTask(null, 0,true,presentationTimeUs,beginMicroseconds, endMicroseconds)
        }
    }
}