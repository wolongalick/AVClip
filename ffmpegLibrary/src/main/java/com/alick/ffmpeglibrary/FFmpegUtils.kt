package com.alick.ffmpeglibrary

class FFmpegUtils {



    /**
     * A native method that is implemented by the 'ffmpeglibrary' native library,
     * which is packaged with this application.
     */
    external fun resample(sourcePath:String,targetPath:String,sourceSampleRate:Int,targetSampleRate:Int,sourceChannels:Int,targetChannels:Int): Int

    companion object {
        // Used to load the 'ffmpeglibrary' library on application startup.
        init {
            System.loadLibrary("ffmpeglibrary")
        }
    }


}