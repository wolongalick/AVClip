package com.alick.avsdk.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.alick.avsdk.clip.BufferTask
import com.alick.utilslibrary.BLog
import com.alick.utilslibrary.FileUtils
import com.alick.utilslibrary.TimeFormatUtils
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.nio.ByteBuffer

/**
 * @author 崔兴旺
 * @description
 * @date 2022/3/24 1:06
 */
class AVUtils {
    companion object {


        fun clone(original: ByteBuffer): ByteBuffer {
            val clone = ByteBuffer.allocate(original.remaining())
            original.rewind() //copy from the beginning
            clone.put(original)
            original.rewind()
            clone.flip()
            return clone
        }

        /**
         * @author 崔兴旺
         * @description
         * @date 2022/3/24 23:58
         */
        fun appendAll(outputFile: File, bufferSize: Int = 4096, files: MutableList<File>) {
            if (!outputFile.exists()) {
                throw NoSuchFileException(outputFile, null, "File doesn't exist.")
            }
            require(!outputFile.isDirectory) { "The file is a directory." }
            FileOutputStream(outputFile, true).use { output ->
                for (file in files) {
                    if (file.isDirectory || !file.exists()) {
                        continue // Might want to log or throw
                    }
                    file.forEachBlock(bufferSize) { buffer, bytesRead -> output.write(buffer, 0, bytesRead) }
                }
            }
        }

        /**
         * 查找音频/视频轨道索引
         * @param 媒体提取器
         * @return  音频/视频轨道索引
         */
        fun findTrackIndex(mediaExtractor: MediaExtractor): Pair<Int, Int> {
            var audioTrackIndex = -1
            var videoTrackIndex = -1
            BLog.i("轨道总个数为:${mediaExtractor.trackCount}个")
            for (index in 0 until mediaExtractor.trackCount) {
                val trackFormat = mediaExtractor.getTrackFormat(index)
                val mime: String = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                BLog.i("正在寻找轨道索引,当前mime:${mime}")
                when {
                    mime.startsWith("audio/") -> {
                        audioTrackIndex = index
                    }
                    mime.startsWith("video/") -> {
                        videoTrackIndex = index
                    }
                }
            }
            return audioTrackIndex to videoTrackIndex
        }

        /**
         * 从音频或视频文件中提取pcm文件
         * @param inAudioOrVideoFile    原始的音频或视频文件
         * @param outPcmFile            输出的pcm文件
         * @param beginTimeUs           截取的开始时间(单位:微秒)
         * @param endTimeUs             截取的结束时间(单位:微秒)
         */
        fun extractPcm(
            inAudioOrVideoFile: File,
            outPcmFile: File,
            beginTimeUs: Long = 0,
            endTimeUs: Long? = null,
            timeOfSize: MutableMap<Long, Long>? = null,//pcm文件的时间与当时的文件字节映射关系,key为时间,单位:微秒,value为文件大小,单位:byte,例如第10000000微秒的字节数是1024byte
            onProgress: ((progress: Long, max: Long, percent: Float, bufferTask: BufferTask) -> Unit)? = null,
            onFinish: (() -> Unit)? = null,
        ) {

            val offsetTime: Long? = timeOfSize?.keys?.firstOrNull()

            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(inAudioOrVideoFile.absolutePath)

            BLog.i("正在提取pcm,准备寻找音频/视频轨道索引")
            val (audioTrackIndex, _) = findTrackIndex(mediaExtractor)

            if (audioTrackIndex == -1) {
                mediaExtractor.release()
                throw RuntimeException("音频轨道索引:${audioTrackIndex},文件:${inAudioOrVideoFile.absolutePath}")
            } else {
                BLog.i("音频轨道索引:${audioTrackIndex},文件:${inAudioOrVideoFile.absolutePath}")
            }

            mediaExtractor.selectTrack(audioTrackIndex)
            if (beginTimeUs > 0) {
                mediaExtractor.seekTo(beginTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }
            val audioFormat = mediaExtractor.getTrackFormat(audioTrackIndex)
            val duration = audioFormat.getLong(MediaFormat.KEY_DURATION)

            val decoder = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME) ?: "")
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            FileUtils.createFile(outPcmFile)
            val outFileChannel = FileOutputStream(outPcmFile).channel

            val allocatedBuffer = ByteBuffer.allocateDirect(getMaxInputSize(audioFormat))
            val outputBufferInfo = MediaCodec.BufferInfo()

            var isFinished = false
            //全部所需解码时长(单位:微秒)
            val totalDuration = (endTimeUs ?: duration) - beginTimeUs

            while (!isFinished) {
                val inputIndex = decoder.dequeueInputBuffer(AVConstant.TIMEOUT_US)
                if (inputIndex >= 0) {
                    val sampleTime = mediaExtractor.sampleTime
                    if(sampleTime<beginTimeUs){
                        mediaExtractor.advance()
                        continue
                    }

                    val inputByteBuffer = decoder.getInputBuffer(inputIndex)
                    val readSize = mediaExtractor.readSampleData(allocatedBuffer, 0)
                    //将从文件读取到的数据放到输入缓冲区中,目的是让解码器来解码
                    inputByteBuffer?.put(allocatedBuffer)
                    val sampleFlags = mediaExtractor.sampleFlags

                    if (sampleTime == -1L || (endTimeUs != null && sampleTime > endTimeUs)) {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, readSize, sampleTime, sampleFlags)
                    }
                    mediaExtractor.advance()
                }

                var outputIndex = decoder.dequeueOutputBuffer(outputBufferInfo, AVConstant.TIMEOUT_US)
                while (outputIndex >= 0) {
                    if (outputBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {//触发回调
                        onProgress?.invoke(totalDuration, totalDuration, 1f, BufferTask.createEmpty(outputBufferInfo.presentationTimeUs, beginTimeUs, (endTimeUs ?: duration)))
                        isFinished = true
                    } else {
                        val outputBuffer = decoder.getOutputBuffer(outputIndex)
                        outputBuffer?.let {
                            //不断地从解码器中取出解码完成的数据,并克隆一份,保存到文件
                            val newOutputBuffer = clone(it)
                            val wroteSize = outFileChannel.write(newOutputBuffer)

                            //已解码时长(单位:微秒)
                            val decodedDuration = outputBufferInfo.presentationTimeUs - beginTimeUs

                            if (offsetTime != null && decodedDuration >= offsetTime && timeOfSize[offsetTime]==0L) {
                                val size = outFileChannel.size()
                                timeOfSize[offsetTime] = size
                                BLog.i("第一个文件,时间与文件大小关系,${TimeFormatUtils.format((offsetTime / 1000_000L).toInt())}对应${size}字节")
                            }

                            //已解码的百分比
                            val percent = decodedDuration.toFloat() / totalDuration

                            //封装缓冲任务
                            val bufferTask = BufferTask(
                                newOutputBuffer,
                                wroteSize,
                                outputBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                outputBufferInfo.presentationTimeUs,
                                beginTimeUs,
                                (endTimeUs ?: duration)
                            )

                            //触发回调
                            onProgress?.invoke(decodedDuration, totalDuration, percent, bufferTask)
                        }
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    outputIndex = decoder.dequeueOutputBuffer(outputBufferInfo, AVConstant.TIMEOUT_US)
                }
            }

            decoder.stop()
            decoder.release()
            outFileChannel.close()
            BLog.i("提取pcm文件成功,输出pcm路径:${outPcmFile.absolutePath}")
            onFinish?.invoke()
        }


        fun getMaxInputSize(mediaFormat: MediaFormat): Int {
            return if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                //使用从实际媒体格式中取出的实际值
                mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).let {
                    if (it > 1024 * 1024) {
                        1024 * 1024
                    } else {
                        it
                    }
                }
            } else {
                //使用默认值
                1024 * 1024
            }
        }


    }
}