#include <jni.h>
#include <string>

extern "C" {
#include <libswresample/swresample.h>
#include <libavutil/opt.h>


}


/**
 * 重采样
 * @param env
 * @param clazz
 * @param sourcePath 源PCM文件
 * @param targetPath 目标PCM文件
 * @param sourceSampleRate 源采样率
 * @param targetSampleRate 目标采样率
 * @param sourceChannels 源声道数
 * @param targetChannels 目标声道数
 * @return
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_alick_ffmpeglibrary_FFmpegUtils_resample(JNIEnv *env, jobject thiz, jstring sourcePath, jstring targetPath, jint sourceSampleRate,
                                                  jint targetSampleRate, jint sourceChannels, jint targetChannels) {
    int result = -1;
    FILE *source;
    FILE *target;
    SwrContext *context;
    int sourceChannelLayout;
    int targetChannelLayout;
    AVSampleFormat sampleFormat;
    int sourceLineSize;
    int sourceBufferSize;
    int sourceSamples;
    uint8_t **sourceData;
    int targetLineSize;
    int targetBufferSize;
    int targetSamples;
    int targetMaxSamples;
    uint8_t **targetData;
    int read;
    const char *_sourcePath = env->GetStringUTFChars(sourcePath, nullptr);
    const char *_targetPath = env->GetStringUTFChars(targetPath, nullptr);
    // 打开文件
    source = fopen(_sourcePath, "rb");
    if (!source) {
        result = -1;
        goto R2;
    }
    target = fopen(_targetPath, "wb");
    if (!target) {
        fclose(source);
        goto R2;
    }
    // 重采样上下文
    context = swr_alloc();
    if (!context) {
        goto R1;
    }
    // 声道类型
    sourceChannelLayout = AV_CH_LAYOUT_STEREO;
    targetChannelLayout = AV_CH_LAYOUT_STEREO;
    // 16BIT交叉存放PCM数据格式
    sampleFormat = AV_SAMPLE_FMT_S16;
    // 配置
    av_opt_set_int(context, "in_channel_layout", sourceChannelLayout, 0);
    av_opt_set_int(context, "in_sample_rate", sourceSampleRate, 0);
    av_opt_set_sample_fmt(context, "in_sample_fmt", sampleFormat, 0);
    av_opt_set_int(context, "out_channel_layout", targetChannelLayout, 0);
    av_opt_set_int(context, "out_sample_rate", targetSampleRate, 0);
    av_opt_set_sample_fmt(context, "out_sample_fmt", sampleFormat, 0);
    // 初始化
    if (swr_init(context) < 0) {
        result = -1;
        goto R1;
    }
    // 输入
    // 输入样品数 一帧1024样品数
    sourceSamples = 1024;
    // 输入大小 计算一帧样品数据量大小 = 声道数 * 样品数 * 每个样品所占字节
    sourceBufferSize = av_samples_get_buffer_size(&sourceLineSize, sourceChannels, sourceSamples, sampleFormat, 1);
    // 分配输入空间
    result = av_samples_alloc_array_and_samples(&sourceData, &sourceLineSize, sourceChannels,
                                                sourceSamples, sampleFormat, 0);
    if (result < 0) {
        result = -1;
        goto R1;
    }
    // 输出
    // 计算（最大）输出样品数
    targetMaxSamples = targetSamples = (int) av_rescale_rnd(sourceSamples, targetSampleRate, sourceSampleRate, AV_ROUND_UP);
    // 分配输出空间
    result = av_samples_alloc_array_and_samples(&targetData, &targetLineSize, targetChannels,
                                                targetSamples, sampleFormat, 0);
    if (result < 0) {
        result = -1;
        goto R1;
    }
    // 循环读取文件
    // 每次读取一帧数据量大小
    read = fread(sourceData[0], 1, sourceBufferSize, source);
    while (read > 0) {
        // 计算输出样品数
        targetSamples = (int) av_rescale_rnd(swr_get_delay(context, sourceSampleRate) + sourceSamples, targetSampleRate, sourceSampleRate,
                                             AV_ROUND_UP);
        if (targetSamples > targetMaxSamples) {
            av_freep(&targetData[0]);
            result = av_samples_alloc(targetData, &targetLineSize, targetChannels, targetSamples, sampleFormat, 1);
            if (result < 0) {
                break;
            }
            targetMaxSamples = targetSamples;
        }
        // 重采样
        result = swr_convert(context, targetData, targetSamples,
                             (const uint8_t **) sourceData, sourceSamples);
        if (result < 0) {
            break;
        }
        // 计算输出大小 result为一帧重采样数
        targetBufferSize = av_samples_get_buffer_size(&targetLineSize, targetChannels, result, sampleFormat, 1);
        if (targetBufferSize < 0) {
            break;
        }
        // 写入文件
        fwrite(targetData[0], 1, targetBufferSize, target);
        // 每次读取一帧数据量大小
        read = fread(sourceData[0], 1, sourceBufferSize, source);
    }
    R1:
    // 关闭文件
    fclose(source);
    fclose(target);
    R2:
    // 释放
    swr_free(&context);
    env->ReleaseStringUTFChars(sourcePath, _sourcePath);
    env->ReleaseStringUTFChars(targetPath, _targetPath);
    return result;
}