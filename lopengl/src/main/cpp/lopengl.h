//
// Created by huangdejun on 2024-06-09.
//
#include <jni.h>
#ifndef OPENGLSAMPLE_LOPENGL_H
#define OPENGLSAMPLE_LOPENGL_H
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_houtrry_lopengl_view_MapView_ndkCreate(JNIEnv *, jobject);

JNIEXPORT void JNICALL Java_com_houtrry_lopengl_view_MapView_ndkResize(JNIEnv *, jobject , jint , jint );

JNIEXPORT void JNICALL Java_com_houtrry_lopengl_view_MapView_ndkDraw(JNIEnv *, jobject );

#ifdef __cplusplus
}
#endif


#endif //OPENGLSAMPLE_LOPENGL_H
