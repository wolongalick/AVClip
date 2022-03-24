#include <jni.h>
#include <string>

#include "lame/lame.h"
#include "Mp3Encoder.h"
#include "CxwLog.h"


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
init(JNIEnv *env, jobject instance, jstring pcmPath_,
     jint audioChannels, jint bitRate, jint sampleRate,
     jstring mp3Path_, jstring tag_) {
    const char *pcmPath = env->GetStringUTFChars(pcmPath_, JNI_FALSE);
    const char *mp3Path = env->GetStringUTFChars(mp3Path_, JNI_FALSE);
    const char *tag = env->GetStringUTFChars(tag_, JNI_FALSE);

    encoder = new Mp3Encoder();
    encoder->Init(pcmPath, audioChannels, bitRate, sampleRate, mp3Path, tag);


    env->ReleaseStringUTFChars(pcmPath_, pcmPath);
    env->ReleaseStringUTFChars(mp3Path_, mp3Path);
    env->ReleaseStringUTFChars(tag_, tag);
}

extern "C"
JNIEXPORT jint JNICALL
encode(JNIEnv *env, jobject thiz, jboolean end_of_stream) {
    return encoder->Encode(end_of_stream);
}
extern "C"
JNIEXPORT void JNICALL
encodeByProgress(JNIEnv *env, jobject thiz, jobject on_progress) {
    encoder->Encode(env, on_progress);
}

extern "C"
JNIEXPORT void JNICALL
destroy(JNIEnv *env, jobject instance) {
    encoder->Destroy();
}

static JNINativeMethod methods[] = {
        {"init",    "(Ljava/lang/String;IIILjava/lang/String;Ljava/lang/String;)V", reinterpret_cast<void *>(init)},
        {"encode",  "(Z)I",                                                         reinterpret_cast<void *>(encode)},
        {"encode",  "(Lcom/alick/lamelibrary/LameUtils$Callback;)V",                reinterpret_cast<void *>(encodeByProgress)},
        {"destroy", "()V",                                                          reinterpret_cast<void *>(destroy)},
};


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (JNI_OK != vm->GetEnv(reinterpret_cast<void **> (&env), JNI_VERSION_1_4)) {
        LOGE("JNI_OnLoad could not get JNI env");
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/alick/lamelibrary/LameUtils");  //获取Java NativeLib类

    //注册Native方法
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof((methods)[0])) < 0) {
        LOGE("注册Natives方法失败");
        return JNI_ERR;
    }

    return JNI_VERSION_1_4;
}