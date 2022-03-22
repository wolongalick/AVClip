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
    external fun encode(endOfStream: Boolean):Int
    external fun destroy()

    companion object {
        // Used to load the 'lamelibrary' library on application startup.
        init {
            System.loadLibrary("lamelibrary")
        }
    }
}