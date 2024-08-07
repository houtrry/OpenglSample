//
// Created by huangdejun on 2024-06-09.
//

#ifndef OPENGLSAMPLE_LANDROIDLOG_H
#define OPENGLSAMPLE_LANDROIDLOG_H

#ifdef __cplusplus
extern "C" {
#endif

#include <android/log.h>

#define LTAG "L-OpenglNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LTAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LTAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LTAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LTAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, LTAG, __VA_ARGS__)

#define LOGTD(tag, ...) __android_log_print(ANDROID_LOG_DEBUG, tag, __VA_ARGS__)
#define LOGTI(tag, ...) __android_log_print(ANDROID_LOG_INFO, tag, __VA_ARGS__)
#define LOGTW(tag, ...) __android_log_print(ANDROID_LOG_WARN, tag, __VA_ARGS__)
#define LOGTE(tag, ...) __android_log_print(ANDROID_LOG_ERROR, tag, __VA_ARGS__)
#define LOGTF(tag, ...) __android_log_print(ANDROID_LOG_FATAL, tag, __VA_ARGS__)

#ifdef __cplusplus
}
#endif
#endif //OPENGLSAMPLE_LANDROIDLOG_H
