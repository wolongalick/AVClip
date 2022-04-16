package com.alick.avsdk.util

import androidx.annotation.IntRange
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * @author 崔兴旺
 * @description 音频混音工具类
 * @date 2022/4/1 23:00
 */
class AudioMix {

    companion object {

        /**
         * 将两个pcm文件混音
         * @param pcm1Path  第一个pcm文件路径
         * @param pcm2Path  第二个pcm文件路径
         * @param toPath    输出的pcm文件路径
         * @param volume1   设置第一个pcm音量(最大值100)
         * @param volume2   设置第二个pcm音量(最大值100)
         */
        fun mixPcm(
            pcm1Path: String,
            pcm2Path: String,
            toPath: String,
            @IntRange(from = 0, to = 100) volume1: Int,
            @IntRange(from = 0, to = 100) volume2: Int
        ) {
            val vol1 = normalizeVolume(volume1)
            val vol2 = normalizeVolume(volume2)

            //一次读取多一点 2k
            val buffer1 = ByteArray(AVConstant.MAX_SIZE)
            val buffer2 = ByteArray(AVConstant.MAX_SIZE)
            //待输出数据
            val targetBuffer = ByteArray(AVConstant.MAX_SIZE)
            val is1 = FileInputStream(pcm1Path)
            val is2 = FileInputStream(pcm2Path)

            //输出PCM 的
            val fileOutputStream = FileOutputStream(toPath)
            var temp2: Short
            var temp1: Short //   两个short变量相加 会大于short   声音
            var temp: Int
            var end1 = false
            var end2 = false
            while (!(end1 && end2)) {
                val readSize1 = is1.read(buffer1)
                end1 = readSize1 == -1
                val readSize2 = is2.read(buffer2)
                end2 = readSize2 == -1
                if (!end1 && !end2) {
                    //当两个音频都没读取到末尾时,则需要混音处理
                    var i = 0
                    while (i < readSize1.coerceAtLeast(readSize2)) {
                        if (i < readSize1.coerceAtMost(readSize2)) {
                            //只有在两个音频重合的部分需要混音
                            //由于的kotlin语音的位运算只支持int和long,因此将所有数据都转成了long类型
                            temp1 = ((buffer1[i].toLong() and 0xff.toLong() or (buffer1[i + 1].toLong() and 0xff.toLong() shl 8)).toShort())
                            temp2 = ((buffer2[i].toLong() and 0xff.toLong() or (buffer2[i + 1].toLong() and 0xff.toLong() shl 8)).toShort())
                            temp = (temp1 * vol1 + temp2 * vol2).toInt() //音乐和 视频声音 各占一半
                            if (temp > Short.MAX_VALUE) {
                                temp = Short.MAX_VALUE.toInt()
                            } else if (temp < Short.MIN_VALUE) {
                                temp = Short.MIN_VALUE.toInt()
                            }
                            targetBuffer[i] = (temp and 0xFF).toByte()
                            targetBuffer[i + 1] = (temp ushr 8 and 0xFF).toByte()
                        } else {
                            //如果第一个音频较长,则将第1个音频多余的部分直接拷贝到目标数组
                            if (readSize1 > readSize2) {
                                targetBuffer[i] = buffer1[i]
                            } else if (readSize1 < readSize2) {
                                //否则将第2个音频多余的部分直接拷贝到目标数组
                                targetBuffer[i] = buffer2[i]
                            }
                        }
                        i += 2
                    }
                } else if (end1 && !end2) {
                    //如果第1个音频已结束,第2个音频没结束,则将第2个音频多余的部分直接拷贝到目标数组
                    System.arraycopy(buffer2, 0, targetBuffer, 0, readSize2)
                } else if (!end1 && end2) {
                    //否则将第1个音频多余的部分直接拷贝到目标数组
                    System.arraycopy(buffer1, 0, targetBuffer, 0, readSize1)
                }
                fileOutputStream.write(targetBuffer)
            }
            is1.close()
            is2.close()
            fileOutputStream.close()
        }

        /**
         * 计算出音量百分比
         */
        private fun normalizeVolume(volume: Int): Float {
            return volume / 100f
        }
    }


}