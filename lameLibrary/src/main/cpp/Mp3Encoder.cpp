//
// Created by Administrator on 2022/3/19.
//

#include "Mp3Encoder.h"
#include "CxwLog.h"

Mp3Encoder::Mp3Encoder() {


}

Mp3Encoder::~Mp3Encoder() {

}

int Mp3Encoder::Init(const char *pcmFilePath, int channels, int bitRate, int sampleRate,
                     const char *mp3FilePath) {
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


void Mp3Encoder::Encode() {
    int buffersize = 1024 * 256;
    short *buffer = new short[buffersize / 2];
    short *leftBuffer = new short[buffersize / 4];
    short *rightBuffer = new short[buffersize / 4];
    unsigned char *mp3_buffer = new unsigned char[buffersize];
    size_t readBufferSize = 0;

    long fileSize=getFileSize(pcmFile);
    LOGI("文件总大小:%ld",fileSize)

    while ((readBufferSize = fread(buffer, 2, buffersize / 2, pcmFile)) > 0) {
        LOGI("读取的readBufferSize:%d,ftell:%ld", readBufferSize,ftell(pcmFile))
        for (int i = 0; i < readBufferSize; ++i) {
            if (i % 2 == 0) {
                leftBuffer[i / 2] = buffer[i];
            } else {
                rightBuffer[i / 2] = buffer[i];
            }
        }
        size_t wroteSize = lame_encode_buffer(lameClient, leftBuffer, rightBuffer, readBufferSize / 2, mp3_buffer, buffersize);
        fwrite(mp3_buffer, 1, wroteSize, mp3File);
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
}
