//
// Created by Administrator on 2022/3/19.
//

#ifndef AVCLIP_MP3ENCODER_H
#define AVCLIP_MP3ENCODER_H

#include <cstdio>
#include <jni.h>
#include "lame/lame.h"
#include "CxwLog.h"

class Mp3Encoder {
private:
    const char* tag= nullptr;
    FILE *pcmFile{};
    FILE *mp3File{};
    lame_t lameClient{};

    long bufferSize = 1024 * 256;
    short *buffer = new short[bufferSize / 2];
    short *leftBuffer = new short[bufferSize / 4];
    short *rightBuffer = new short[bufferSize / 4];
    unsigned char *mp3_buffer = new unsigned char[bufferSize];
public:
    Mp3Encoder();

    ~Mp3Encoder();

    int Init(const char *pcmFilePath, int channels, int bitRate, int sampleRate, const char *mp3FilePath,const char* tag);

    int Encode(bool end_of_stream);

    void Encode(JNIEnv *env, jobject on_progress);

    void Destroy();
};

#endif //AVCLIP_MP3ENCODER_H
