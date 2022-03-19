#include <jni.h>
#include <string>

#include "lame/lame.h"
#include "Mp3Encoder.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_alick_lamelibrary_LameUtils_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


Mp3Encoder *encoder = NULL;

extern "C"
JNIEXPORT void JNICALL
Java_com_alick_lamelibrary_LameUtils_init(JNIEnv *env, jobject instance, jstring pcmPath_,
                                          jint audioChannels, jint bitRate, jint sampleRate,
                                          jstring mp3Path_) {
    const char *pcmPath = env->GetStringUTFChars(pcmPath_, 0);
    const char *mp3Path = env->GetStringUTFChars(mp3Path_, 0);

    encoder = new Mp3Encoder();
    encoder->Init(pcmPath, audioChannels, bitRate, sampleRate, mp3Path);


    env->ReleaseStringUTFChars(pcmPath_, pcmPath);
    env->ReleaseStringUTFChars(mp3Path_, mp3Path);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_alick_lamelibrary_LameUtils_encode(JNIEnv *env, jobject instance) {
    encoder->Encode();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alick_lamelibrary_LameUtils_destroy(JNIEnv *env, jobject instance) {
    encoder->Destroy();
}