package com.alick.avsdk.util

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import com.alick.utilslibrary.BLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * @createTime 2022/4/15 16:35
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
class Pcm2AACUtils(
    private val inPcmFile: File,
    private val outAACFile: File,
    private val sampleRate: Int,
    private val bitRate: Int,
    private val channelCount: Int
) {


    fun convert() {
        BLog.i("开始将pcm转成aac,,采样率:${sampleRate},比特率:${bitRate},通道数:${channelCount}")
        val mediaCodec: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO)
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        }

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()

        var isFinished = false

        val fin = FileInputStream(inPcmFile)
        val fOut = FileOutputStream(outAACFile)
        val inByteArray = ByteArray(AVConstant.MAX_SIZE)
        val bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()

        while (!isFinished) {
            val inputIndex = mediaCodec.dequeueInputBuffer(AVConstant.TIMEOUT_US)
            if (inputIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputIndex)
                if (inputBuffer != null) {
                    val readSize = fin.read(inByteArray)
                    if (readSize > 0) {
                        inputBuffer.put(inByteArray, 0, readSize)
                        mediaCodec.queueInputBuffer(inputIndex, 0, readSize, 0, 0)
                    } else {
                        mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                } else {
                    mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, 0)
                }
            }

            var outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, AVConstant.TIMEOUT_US)

            while (outputIndex >= 0) {
                val outputBuffer = mediaCodec.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    val chunkAudio = ByteArray(bufferInfo.size + 7) // 7 is ADTS size
                    addADTStoPacket(chunkAudio, chunkAudio.size,sampleRate,channelCount)
                    outputBuffer.get(chunkAudio, 7, bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    fOut.write(chunkAudio)
                }
                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    isFinished = true
                }
                mediaCodec.releaseOutputBuffer(outputIndex, false)
                outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, AVConstant.TIMEOUT_US)
            }
        }

        BLog.i("Pcm转AAC完成,文件路径:${outAACFile.absolutePath}")

        fin.close()
        fOut.close()
        mediaCodec.stop()
        mediaCodec.release()
    }

    private fun addADTStoPacket(packet: ByteArray, packetLen: Int, sampleRate: Int, channelCount: Int) {
        val profile = 2 //AAC LC
        val freqIdx = when (sampleRate) {
            96000 -> {
                0
            }
            88200-> {
                1
            }
            64000-> {
                2
            }
            48000-> {
                3
            }
            44100-> {
                4
            }
            32000-> {
                5
            }
            24000-> {
                5
            }
            22050 -> {
                7
            }
            16000 -> {
                8
            }
            12000 -> {
                9
            }
            else -> {
                4 //44.1KHz
            }
        }

        val chanCfg = channelCount //CPE
        // fill in ADTS data
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }

}