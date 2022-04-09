package com.alick.avsdk.clip

import java.nio.ByteBuffer

class BufferTask(val byteBuffer: ByteBuffer, val isEndOfStream: Boolean, val presentationTimeUs: Long)