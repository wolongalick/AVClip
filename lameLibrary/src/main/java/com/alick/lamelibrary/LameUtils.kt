package com.alick.lamelibrary

class LameUtils {

    /**
     * A native method that is implemented by the 'lamelibrary' native library,
     * which is packaged with this application.
     */
    interface Callback{
        fun onProgress(progress: Long, max: Long)
    }

    external fun stringFromJNI(): String
    external fun init(pcmPath: String?, audioChannels: Int, bitRate: Int, sampleRate: Int, mp3Path: String?,tag:String)
    external fun encode(endOfStream: Boolean):Int
    external fun encode(onProgress: Callback)
    external fun destroy()

    companion object {
        // Used to load the 'lamelibrary' library on application startup.
        init {
            System.loadLibrary("lamelibrary")
        }
    }
}