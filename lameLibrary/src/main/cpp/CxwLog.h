//
// Created by Administrator on 2022/3/19.
//

#ifndef AVCLIP_CXWLOG_H
#define AVCLIP_CXWLOG_H

#include <android/log.h>

#define LOG_TAG "JNI_cxw"
#define LOGI(format, args...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, format, ##args);
#define LOGE(format, args...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, format, ##args);

#endif //AVCLIP_CXWLOG_H
