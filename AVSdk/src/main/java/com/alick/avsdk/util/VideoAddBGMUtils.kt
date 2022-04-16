package com.alick.avsdk.util

import android.annotation.SuppressLint
import android.media.*
import androidx.annotation.IntRange
import com.alick.avsdk.MediaParser
import com.alick.ffmpeglibrary.FFmpegUtils
import com.alick.utilslibrary.AppHolder
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.FileUtils
import java.io.File
import java.nio.ByteBuffer

/**
 * @createTime 2022/4/14 12:36
 * @author 崔兴旺  1607009565@qq.com
 * @description
 */
class VideoAddBGMUtils(
    val inVideoFile: File,
    val inAudioFile: File,
    val outVideoFile: File,
    @IntRange(from = 0, to = 100)
    val videoVolume: Int,
    @IntRange(from = 0, to = 100)
    val audioVolume: Int
) {

    fun mix() {
        BLog.i("开始为视频添加BGM")
        BLog.i("原视频文件路径:${inVideoFile.absolutePath}")
        BLog.i("原音频文件路径:${inAudioFile.absolutePath}")

        val dir = AppHolder.getApp().getExternalFilesDir(AVConstant.OUTPUT_DIR)


        val videoPcmFile = File(dir, inVideoFile.name.replaceAfterLast(".", "pcm"))
        AVUtils.extractPcm(inVideoFile, videoPcmFile)
        AVUtils.extractPcm(inVideoFile, videoPcmFile)
            /*onFail = {
                BLog.e("从视频提取pcm文件路径失败:${it}")
                return@extractPcm
            }*/
        BLog.i("从视频提取pcm文件路径:${videoPcmFile.absolutePath}")

        val audioPcmFile = File(dir, inAudioFile.name.replaceAfterLast(".", "pcm"))
        AVUtils.extractPcm(inAudioFile, audioPcmFile)
        /*onFail = {
            BLog.e("从音频提取pcm文件路径失败:${it}")
        }*/

        BLog.i("从音提取pcm文件路径:${audioPcmFile.absolutePath}")

        val mediaParser = MediaParser()
        val video = mediaParser.parseAudio(inVideoFile.absolutePath)
        val audio = mediaParser.parseAudio(inAudioFile.absolutePath)
        val videoPcmSampleRate = video.sampleRate
        val audioPcmSampleRate = audio.sampleRate

        BLog.i("视频文件的音频流采样率:${videoPcmSampleRate}")
        BLog.i("音频文件的音频流采样率:${audioPcmSampleRate}")

        val selectedSampleRate = if (videoPcmSampleRate >= audioPcmSampleRate) {
            videoPcmSampleRate
        } else {
            audioPcmSampleRate
        }

        //比特率
        val selectedBitRate = if (videoPcmSampleRate >= audioPcmSampleRate) {
            video.bitrate
        } else {
            audio.bitrate
        }

        BLog.i("最高重采样:${selectedSampleRate}")

        val targetChannelCount = 2

        val videoPcmResampleFile: File
        if (selectedSampleRate != videoPcmSampleRate) {
            videoPcmResampleFile = File(dir, "重采样-" + videoPcmFile.name)
            FFmpegUtils().resample(videoPcmFile.absolutePath, videoPcmResampleFile.absolutePath, video.sampleRate, selectedSampleRate, video.channelCount, targetChannelCount)
        } else {
            //若视频文件的音频流不需要重采样,则直接赋值
            videoPcmResampleFile = videoPcmFile
        }

        val audioPcmResampleFile: File
        if (selectedSampleRate != audioPcmSampleRate) {
            audioPcmResampleFile = File(dir, "重采样-" + audioPcmFile.name)
            FFmpegUtils().resample(audioPcmFile.absolutePath, audioPcmResampleFile.absolutePath, audio.sampleRate, selectedSampleRate, audio.channelCount, targetChannelCount)
        } else {
            //若音频文件的音频流不需要重采样,则直接赋值
            audioPcmResampleFile = audioPcmFile
        }

        val mixPcmFile = File(dir, "混音-" + inVideoFile.name.replaceAfterLast(".", "pcm"))
        AudioMix.mixPcm(videoPcmResampleFile.absolutePath, audioPcmResampleFile.absolutePath, mixPcmFile.absolutePath, videoVolume, audioVolume)
        BLog.i("混音完成,文件路径:${mixPcmFile.absolutePath}")


//        val wavFile = File(dir, videoPcmFile.name.replaceAfterLast(".", "wav"))
//        BLog.i("准备转换为wav")
//        PcmToWavUtil(maxSampleRate, AudioFormat.CHANNEL_IN_STEREO, targetChannelCount, video.pcmEncoding).pcmToWav(mixPcmFile.absolutePath, wavFile.absolutePath)
//        BLog.i("转换为wav文件完成,文件路径:${wavFile.absolutePath}")


        val aacFile = File(dir, videoPcmFile.name.replaceAfterLast(".", "aac"))
        Pcm2AACUtils(mixPcmFile, aacFile, selectedSampleRate, selectedBitRate, targetChannelCount).convert()


        BLog.i("准备音视频封装")
        FileUtils.createFile(outVideoFile)

        val mediaMuxer = MediaMuxer(outVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()

        videoExtractor.setDataSource(inVideoFile.absolutePath)
        audioExtractor.setDataSource(aacFile.absolutePath)

        val (_, videoTrackIndex) = AVUtils.findTrackIndex(videoExtractor)
        val (audioTrackIndex, _) = AVUtils.findTrackIndex(audioExtractor)

        BLog.i("videoTrackIndex:${videoTrackIndex}")
        BLog.i("audioTrackIndex:${audioTrackIndex}")

        //添加视频轨道
        val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)
        val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)
        val videoTrack = mediaMuxer.addTrack(videoFormat)
        val audioTrack = mediaMuxer.addTrack(audioFormat)
        BLog.i("mediaMuxer视频轨道索引:${videoTrack}")
        BLog.i("mediaMuxer音频轨道索引:${audioTrack}")

        mediaMuxer.start()

        BLog.i("开始音视频封装")
        videoExtractor.selectTrack(videoTrackIndex)
        clip(videoExtractor, mediaMuxer, videoFormat, videoTrack)
        audioExtractor.selectTrack(audioTrackIndex)
        clip(audioExtractor, mediaMuxer, audioFormat, audioTrack)
        BLog.i("完成音视频封装,文件路径:${outVideoFile.absolutePath}")

        BLog.i("释放资源(mediaMuxer、videoExtractor、audioExtractor）")
        mediaMuxer.stop()
        mediaMuxer.release()
        videoExtractor.release()
        audioExtractor.release()
    }

    @SuppressLint("WrongConstant")
    private fun clip(mediaExtractor: MediaExtractor, mediaMuxer: MediaMuxer, mediaFormat: MediaFormat, trackIndex: Int, beginMicroseconds: Long = 0, endMicroseconds: Long = Long.MAX_VALUE) {
        val videoByteBuffer = ByteBuffer.allocateDirect(AVUtils.getMaxInputSize(mediaFormat))
        val videoInfo = MediaCodec.BufferInfo()
        while (true) {
            val readSize = mediaExtractor.readSampleData(videoByteBuffer, 0)
            if (readSize < 0) {
                return
            }

            var isInRange = true

            val sampleTimeUs = mediaExtractor.sampleTime
            when {
                sampleTimeUs != -1L && sampleTimeUs < beginMicroseconds -> {
                    //如果读取的时间戳小于设置的截取起始时间戳,则忽略,避免浪费时间
                    mediaExtractor.advance()//读取下一帧数据
                    isInRange = false
                }
                sampleTimeUs == -1L || sampleTimeUs >= endMicroseconds -> {
                    //结束
                    return
                }
                else -> {
                    //正常执行
                }
            }

            if (isInRange) {
                videoInfo.size = readSize
                videoInfo.presentationTimeUs = sampleTimeUs
                videoInfo.flags = mediaExtractor.sampleFlags

                mediaMuxer.writeSampleData(trackIndex, videoByteBuffer, videoInfo)
                mediaExtractor.advance()

//                withContext(Dispatchers.Main) {
//                    if (trackIndex == videoIndex) {
//                        onProgress(((sampleTimeUs - beginMicroseconds) * 0.5).toLong(), endMicroseconds - beginMicroseconds)
//                    } else if (trackIndex == audioIndex) {
//                        onProgress(((endMicroseconds - beginMicroseconds) * 0.5 + (sampleTimeUs - beginMicroseconds) * 0.5).toLong(), endMicroseconds - beginMicroseconds)
//                    }
//                }
            }
        }
    }
}