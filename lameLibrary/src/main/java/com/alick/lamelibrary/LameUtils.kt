package com.alick.lamelibrary

class LameUtils {

    interface Callback {
        fun onProgress(progress: Long, max: Long)
    }

    /**
     * A native method that is implemented by the 'lamelibrary' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
    external fun init(pcmPath: String?, audioChannels: Int, bitRate: Int, sampleRate: Int, mp3Path: String?)

    //    external fun encode(onProgress:(progress:Long,max:Long)->Unit)
    external fun encode(onProgress: Callback)
    external fun encodeChunk(readBufferSize: Int, leftBuffer: ShortArray, rightBuffer: ShortArray, mp3buf: ByteArray, mp3buf_size: Int)
    external fun destroy()

    companion object {
        // Used to load the 'lamelibrary' library on application startup.
        init {
            System.loadLibrary("lamelibrary")
        }
    }
}