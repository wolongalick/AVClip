package com.alick.avsdk

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import com.alick.utilslibrary.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * @author 崔兴旺
 * @description 音频裁剪工具类
 * @date 2022/3/11 22:05
 */
class AudioClipUtils {


    companion object {
        val dir = AppHolder.getApp().getExternalFilesDir("output")

        /**
         * 裁剪
         */
        @SuppressLint("WrongConstant")
        fun clip(inFile: File, outFile: File, beginMicroseconds: Long, endMicroseconds: Long, onProgress: (progress: Long, max: Long) -> Unit) {
            val pcmFile = File(outFile.parentFile, outFile.name.replace(".mp3", ".pcm"))

            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(inFile.absolutePath)
            val trackIndex = mediaExtractor.let {
                for (i in 0..it.trackCount) {
                    val trackFormat = mediaExtractor.getTrackFormat(i)
                    if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        return@let i
                    }
                }
                return@let 0
            }
            mediaExtractor.selectTrack(trackIndex)
            mediaExtractor.seekTo(beginMicroseconds, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
            val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val encoding = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }

            val maxBufferSize = if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                //使用从实际媒体格式中取出的实际值
                mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                //使用默认值
                100 * 1000
            }

            FileUtils.createFile(pcmFile)
            val writeChannel = FileOutputStream(pcmFile).channel


            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            //创建解码器
            val mediaCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME)!!)
            //配置解码器
            mediaCodec.configure(mediaFormat, null, null, 0)
            //启动解码器
            mediaCodec.start()

            val info = MediaCodec.BufferInfo()
            var outputBufferIndex = -1
            val fileName = TimeUtils.getCurrentTime() + ".txt"
            while (true) {
                val dequeueInputIndex = mediaCodec.dequeueInputBuffer(10 * 1000)
                BLog.i("dequeueInputIndex:${dequeueInputIndex}")

                if (dequeueInputIndex >= 0) {
                    val sampleTimeUs = mediaExtractor.sampleTime//时间戳,单位:微秒
                    BLog.i("sampleTimeUs:${sampleTimeUs}")

                    if (sampleTimeUs == -1L) {
                        //结束
                        break
                    }
                    if (sampleTimeUs < beginMicroseconds) {
                        //如果读取的时间戳小于设置的截取起始时间戳,则忽略,避免浪费时间
                        mediaExtractor.advance()//读取下一帧数据
                        continue
                    }

                    if (sampleTimeUs >= endMicroseconds) {
                        onProgress(endMicroseconds - beginMicroseconds, endMicroseconds - beginMicroseconds)
                        break
                    }
                    onProgress(sampleTimeUs - beginMicroseconds, endMicroseconds - beginMicroseconds)

                    info.size = mediaExtractor.readSampleData(buffer, 0)
                    info.presentationTimeUs = sampleTimeUs
                    info.flags = mediaExtractor.sampleFlags

                    val content = ByteArray(buffer.remaining())
                    buffer.get(content)
                    //将二进制数据转换为16进制,并保存到文件,方便查看
                    /*FileUtils.writeContent(
                        File(dir, fileName),
                        true,
                        content
                    )*/

                    val inputBuffer = mediaCodec.getInputBuffer(dequeueInputIndex)
                    inputBuffer!!.put(content)
                    mediaCodec.queueInputBuffer(dequeueInputIndex, 0, info.size, info.presentationTimeUs, info.flags)

                    //释放上一帧的压缩数据
                    mediaExtractor.advance()
                }

                outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 100_000)
                BLog.i("outputBufferIndex:${outputBufferIndex}")
                while (outputBufferIndex >= 0) {
                    BLog.i("outputBufferIndex:${outputBufferIndex}")
                    val decodeOutputBuffer: ByteBuffer? = mediaCodec.getOutputBuffer(outputBufferIndex)
                    writeChannel.write(decodeOutputBuffer)
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 100_000)
                }
            }
            BLog.i("关闭、释放资源")
            writeChannel.close()
            mediaExtractor.release()
            mediaCodec.stop()
            mediaCodec.release()

            BLog.i("准备将pcm转换为MP3");
            //将pcm数据转换为mp3封装格式
            val pcmToWavUtil = PcmToWavUtil(sampleRate, AudioFormat.CHANNEL_IN_STEREO, channelCount, encoding)
            pcmToWavUtil.pcmToWav(pcmFile.absolutePath, outFile.absolutePath)
            BLog.i("将pcm转换为MP3完毕")
        }
    }

}