package com.alick.avsdk.util;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * wuqingsen on 2020-05-28
 * Mailbox:1243411677@qq.com
 * annotation:pcm转aac
 */
@SuppressLint("NewApi")
public class PCMToAAC {

    private String encodeType = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int samples_per_frame = 2048;

    private MediaCodec mediaEncode;
    private MediaCodec.BufferInfo encodeBufferInfo;
    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;

    private byte[] chunkAudio = new byte[0];
    private BufferedOutputStream out;
    File aacFile;
    File pcmFile;

    public PCMToAAC(String aacPath, String pcmPath) {
        aacFile = new File(aacPath);
        pcmFile = new File(pcmPath);
        if (!aacFile.exists()) {
            try {
                aacFile.getParentFile().mkdirs();
                aacFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            out = new BufferedOutputStream(new FileOutputStream(aacFile, true));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        initAACMediaEncode();

    }

    /**
     * 初始化AAC编码器
     */
    private void initAACMediaEncode() {
        try {
            //参数对应-> mime type、采样率、声道数
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);//比特率
            encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//            encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, samples_per_frame);//作用于inputBuffer的大小
            mediaEncode = MediaCodec.createEncoderByType(encodeType);
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaEncode == null) {
            return;
        }
        mediaEncode.start();
        encodeInputBuffers = mediaEncode.getInputBuffers();
        encodeOutputBuffers = mediaEncode.getOutputBuffers();
        encodeBufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * 编码PCM数据 得到AAC格式的音频文件
     */
    private void dstAudioFormatFromPCM(byte[] pcmData) {

        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;

        int outBitSize;
        int outPacketSize;
        byte[] pcmAudio;
        pcmAudio = pcmData;

        encodeInputBuffers = mediaEncode.getInputBuffers();
        encodeOutputBuffers = mediaEncode.getOutputBuffers();
        encodeBufferInfo = new MediaCodec.BufferInfo();


        inputIndex = mediaEncode.dequeueInputBuffer(0);
        if (inputIndex != -1) {

            inputBuffer = encodeInputBuffers[inputIndex];
            inputBuffer.clear();
            inputBuffer.limit(pcmAudio.length);
            inputBuffer.put(pcmAudio);//PCM数据填充给inputBuffer
            mediaEncode.queueInputBuffer(inputIndex, 0, pcmAudio.length, 0, 0);//通知编码器 编码


            outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 0);
            while (outputIndex > 0) {

                outBitSize = encodeBufferInfo.size;
                outPacketSize = outBitSize + 7;//7为ADT头部的大小
                outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
                outputBuffer.position(encodeBufferInfo.offset);
                outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
                chunkAudio = new byte[outPacketSize];
                addADTStoPacket(chunkAudio, outPacketSize);//添加ADTS
                outputBuffer.get(chunkAudio, 7, outBitSize);//将编码得到的AAC数据 取出到byte[]中

                try {
                    //录制aac音频文件，保存在手机内存中
                    out.write(chunkAudio, 0, chunkAudio.length);
                    out.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                outputBuffer.position(encodeBufferInfo.offset);
                mediaEncode.releaseOutputBuffer(outputIndex, false);
                outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 0);

            }

        }
    }

    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 8; // 16KHz
        int chanCfg = 1; // CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;

    }

    public byte[] readInputStream(File file) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 1.建立通道对象
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // 2.定义存储空间
        byte[] buffer = new byte[1024];
        // 3.开始读文件
        int len = -1;
        try {
            if (inputStream != null) {
                while ((len = inputStream.read(buffer)) != -1) {
                    // 将Buffer中的数据写到outputStream对象中
//                    outputStream.write(buffer, 0, len);
                    dstAudioFormatFromPCM(buffer);
                    Log.e("wqs+readInputStream", "readInputStream: " + buffer);
                }
            }
            // 4.关闭流
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

}