//
// Created by Administrator on 2022/3/19.
//

#include "Mp3Encoder.h"

Mp3Encoder::Mp3Encoder() = default;

Mp3Encoder::~Mp3Encoder() = default;

int Mp3Encoder::Init(const char *pcmFilePath, int channels, int bitRate, int sampleRate,
                     const char *mp3FilePath, const char *tag) {
    Mp3Encoder::tag = tag;
    int ret = -1;
    pcmFile = fopen(pcmFilePath, "rb");
    if (pcmFile) {
        mp3File = fopen(mp3FilePath, "wb");
        if (mp3File) {
            lameClient = lame_init();
            lame_set_in_samplerate(lameClient, sampleRate);
            lame_set_out_samplerate(lameClient, sampleRate);
            lame_set_num_channels(lameClient, channels);
            lame_set_brate(lameClient, bitRate / 1000);
            lame_set_quality(lameClient, 7);//7的音质最好
            lame_init_params(lameClient);
            ret = 0;
        }
    }
    return ret;
}

long getFileSize(FILE *fp) {
    fseek(fp, 0L, SEEK_END);
    long size = ftell(fp);
    fseek(fp, 0L, SEEK_SET);
    return size;
}

long getUnreadFileSize(FILE *fp) {
    long currentPosition = ftell(fp);
    fseek(fp, 0L, SEEK_END);
    long endPosition = ftell(fp);
    long unreadFileSize = endPosition - currentPosition;
    //回到原位
    fseek(fp, currentPosition, SEEK_SET);
    return unreadFileSize;
}


int Mp3Encoder::Encode(bool end_of_stream) {

    size_t readBufferSize;
    long unreadFileSize = getUnreadFileSize(pcmFile);
    if (unreadFileSize < bufferSize) {
        if (end_of_stream) {
            LOGE("tag:%s,未读文件大小:%ld,不足%ld,但是由于到达了流末尾,因此继续编码,无需return", tag, unreadFileSize, bufferSize)
        } else {
            LOGE("tag:%s,未读文件大小:%ld,不足%ld,", tag, unreadFileSize, bufferSize)
            return -2;
        }
    } else {
        LOGI("tag:%s,未读文件大小:%ld,满足%ld", tag, unreadFileSize, bufferSize)
    }


//    LOGI("pcm编码前文件position:%ld", ftell(pcmFile))
    while ((readBufferSize = fread(buffer, 2, bufferSize / 2, pcmFile)) > 0) {
        for (int i = 0; i < readBufferSize; ++i) {
            if (i % 2 == 0) {
                leftBuffer[i / 2] = buffer[i];
            } else {
                rightBuffer[i / 2] = buffer[i];
            }
        }
        size_t wroteSize = lame_encode_buffer(lameClient, leftBuffer, rightBuffer, (int) readBufferSize / 2, mp3_buffer, bufferSize);

//        LOGI("pcm转码MP3,已写入文件大小:%ld", ftell(pcmFile))
        fwrite(mp3_buffer, 1, wroteSize, mp3File);
    }
//    LOGI("pcm编码后文件position:%ld", ftell(pcmFile))

    return 0;
}

void Mp3Encoder::Encode(JNIEnv *env, jobject on_progress) {
    int buffersize = 1024 * 256;
    short *buffer = new short[buffersize / 2];
    short *leftBuffer = new short[buffersize / 4];
    short *rightBuffer = new short[buffersize / 4];
    unsigned char *mp3_buffer = new unsigned char[buffersize];
    size_t readBufferSize = 0;

    long fileSize = getFileSize(pcmFile);
    LOGI("文件总大小:%ld", fileSize)
    jclass function2Jclass = env->FindClass("com/alick/lamelibrary/LameUtils$Callback");
    jmethodID invokeJmethodID = env->GetMethodID(function2Jclass, "onProgress", "(JJ)V");
    LOGI("invokeJmethodID:%d", invokeJmethodID != NULL)

    while ((readBufferSize = fread(buffer, 2, buffersize / 2, pcmFile)) > 0) {
        for (int i = 0; i < readBufferSize; ++i) {
            if (i % 2 == 0) {
                leftBuffer[i / 2] = buffer[i];
            } else {
                rightBuffer[i / 2] = buffer[i];
            }
        }
        size_t wroteSize = lame_encode_buffer(lameClient, leftBuffer, rightBuffer, readBufferSize / 2, mp3_buffer, buffersize);
        fwrite(mp3_buffer, 1, wroteSize, mp3File);
        //将读文件的进度,回调给应用层
        env->CallVoidMethod(on_progress, invokeJmethodID, (jlong)ftell(pcmFile),(jlong) fileSize);
    }

    delete[] buffer;
    delete[] leftBuffer;
    delete[] rightBuffer;
    delete[] mp3_buffer;

}

void Mp3Encoder::Destroy() {
    if (pcmFile) {
        fclose(pcmFile);
    }
    if (mp3File) {
        fclose(mp3File);
        lame_close(lameClient);
    }
    delete[] buffer;
    delete[] leftBuffer;
    delete[] rightBuffer;
    delete[] mp3_buffer;
}

