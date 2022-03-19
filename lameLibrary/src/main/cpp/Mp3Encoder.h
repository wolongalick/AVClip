//
// Created by Administrator on 2022/3/19.
//

#ifndef AVCLIP_MP3ENCODER_H
#define AVCLIP_MP3ENCODER_H

#include <cstdio>
#include "lame/lame.h"

class Mp3Encoder {
private:
    FILE* pcmFile;
    FILE* mp3File;
    lame_t lameClient;
public:
    Mp3Encoder();
    ~Mp3Encoder();
    int Init(const char* pcmFilePath,int channels,int bitRate,int sampleRate,const char* mp3FilePath);
    void Encode();
    void Destroy();
};

#endif //AVCLIP_MP3ENCODER_H
