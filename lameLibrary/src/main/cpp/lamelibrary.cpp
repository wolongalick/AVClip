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


Mp3Encoder *encoder = nullptr;

extern "C"
JNIEXPORT void JNICALL
Java_com_alick_lamelibrary_LameUtils_init(JNIEnv *env, jobject instance, jstring pcmPath_,
                                          jint audioChannels, jint bitRate, jint sampleRate,
                                          jstring mp3Path_) {
    const char *pcmPath = env->GetStringUTFChars(pcmPath_, nullptr);
    const char *mp3Path = env->GetStringUTFChars(mp3Path_, nullptr);

    encoder = new Mp3Encoder();
    encoder->Init(pcmPath, audioChannels, bitRate, sampleRate, mp3Path);


    env->ReleaseStringUTFChars(pcmPath_, pcmPath);
    env->ReleaseStringUTFChars(mp3Path_, mp3Path);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_alick_lamelibrary_LameUtils_encode(JNIEnv *env, jobject instance, jobject on_progress) {
    encoder->Encode(env, on_progress);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_alick_lamelibrary_LameUtils_destroy(JNIEnv *env, jobject instance) {
    encoder->Destroy();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_alick_lamelibrary_LameUtils_encodeChunk(JNIEnv *env, jobject thiz, jint read_buffer_size, jshortArray left_buffer, jshortArray right_buffer,
                                                 jbyteArray mp3buf, jint mp3buf_size) {
    jshort *leftJShort = env->GetShortArrayElements(left_buffer, JNI_FALSE);
    jshort *rightJShort = env->GetShortArrayElements(right_buffer, JNI_FALSE);

    jbyte *mp3bufByte = env->GetByteArrayElements(mp3buf, JNI_FALSE);
    encoder->Encode(read_buffer_size, leftJShort, rightJShort, reinterpret_cast<unsigned char *>(mp3bufByte), mp3buf_size);

    env->ReleaseShortArrayElements(left_buffer, leftJShort, 0);
    env->ReleaseShortArrayElements(right_buffer, rightJShort, 0);
    env->ReleaseByteArrayElements(mp3buf, mp3bufByte, 0);


}