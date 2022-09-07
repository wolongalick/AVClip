package com.alick.lamelibrary

class LameUtils {
    var isInitialized = false

    /**
     * A native method that is implemented by the 'lamelibrary' native library,
     * which is packaged with this application.
     */
    interface Callback {
        fun onProgress(progress: Long, max: Long)
    }

    fun initialized(pcmPath: String?, audioChannels: Int, bitRate: Int, sampleRate: Int, mp3Path: String?, tag: String) {
        if (!isInitialized) {
            isInitialized = true
            init(pcmPath, audioChannels, bitRate, sampleRate, mp3Path, tag)
        }
    }

    fun release() {
        isInitialized = false
        destroy()
    }


    external fun stringFromJNI(): String
    external fun encode(endOfStream: Boolean): Int
    external fun encode(onProgress: Callback)
    private external fun init(pcmPath: String?, audioChannels: Int, bitRate: Int, sampleRate: Int, mp3Path: String?, tag: String)
    private external fun destroy()

    companion object {
        // Used to load the 'lamelibrary' library on application startup.
        init {
            System.loadLibrary("lamelibrary")
        }
    }
}