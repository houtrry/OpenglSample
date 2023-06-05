#include <jni.h>
#include <string>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#define TAG "opengl-sample"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WRAN, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_houtrry_openglsample_activity_NativeOpenglActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from Native opengl C++";
    return env->NewStringUTF(hello.c_str());
}

const jint ERROR_NO_DISPLAY = -1;
const jint ERROR_INITIALIZE = -2;
const jint ERROR_CHOOSE_CONFIG = -3;
const jint ERROR_CREATE_CONTEXT = -4;
const jint ERROR_GET_CONFIG_ATTRIB = -5;
const jint ERROR_CREATE_PBUFFER_SURFACE = -6;
const jint ERROR_CREATE_WINDOW_SURFACE = -7;

extern "C"
JNIEXPORT jint JNICALL
Java_com_houtrry_openglsample_activity_NativeOpenglActivity_initEgl(JNIEnv *env, jobject thiz,
                                                   jobject surface) {
    //https://www.jianshu.com/p/d5ff1ff4ee2a
    //1. 获取默认的EGLDisplay。
    //2. 对EGLDisplay进行初始化。
    //3. 输入预设置的参数获取EGL支持的EGLConfig。
    //4. 通过EGLDisplay和EGLConfig创建一个EGLContext上下文环境。
    //5. 创建一个EGLSurface来连接EGL和设备的屏幕。
    //6. 在渲染线程绑定EGLSurface和EGLContext。
    //7. 【进行OpenGL ES的API渲染步骤】(与EGL无关)
    //8. 调用SwapBuffer进行双缓冲切换显示渲染画面。
    //9. 释放EGL相关资源EGLSurface、EGLContext、EGLDisplay。

    EGLDisplay egl_display;
//    egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
//    if (egl_display == EGL_NO_DISPLAY) {
//        LOGE("egl is no display ");
//        return ERROR_NO_DISPLAY;
//    }
    if ((egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay() return error %d", eglGetError());
        return ERROR_NO_DISPLAY;
    }
    if (!eglInitialize(egl_display, 0, 0)) {
        LOGE("eglInitialize() return error %d", eglGetError());
        return ERROR_INITIALIZE;
    }

    const EGLint attrib_list[] = {
            EGL_BUFFER_SIZE, 32,//颜色缓冲区中所有组成颜色的位数
            EGL_ALPHA_SIZE, 8,//颜色缓冲区中透明度位数
            EGL_BLUE_SIZE, 8,//颜色缓冲区中蓝色位数
            EGL_GREEN_SIZE, 8,//颜色缓冲区中绿色位数
            EGL_RED_SIZE, 8,//颜色缓冲区中红色位数
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,//渲染窗口支持的布局组成
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,//EGL 窗口支持的类型
            EGL_NONE
    };
    EGLConfig config;
    EGLint num_config;
    if (!eglChooseConfig(egl_display, attrib_list, &config, 1, &num_config)) {
        LOGE("eglChooseConfig() return error %d", eglGetError());
        return ERROR_CHOOSE_CONFIG;
    }
    EGLint context_attributes[] = {
            EGL_CONTEXT_CLIENT_TYPE, 2, //指定opengl es的版本为2.0
            EGL_NONE
    };
    EGLContext egl_context;
    if (!(egl_context = eglCreateContext(egl_display, config, NULL, context_attributes))) {
        LOGE("eglCreateContext() return error %d", eglGetError());
        return ERROR_CREATE_CONTEXT;
    }

    //链接EGL和设备屏幕
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    EGLint format;
    if (!eglGetConfigAttrib(egl_display, config, EGL_NATIVE_VISUAL_ID, &format)) {
        LOGE("eglGetConfigAttrib() return error %d", eglGetError());
        return ERROR_GET_CONFIG_ATTRIB;
    }
    ANativeWindow_setBuffersGeometry(window, 0, 0, format);
    EGLSurface eglSurface;
    if (!(eglSurface = eglCreateWindowSurface(window, config, window, 0))) {
        LOGE("eglCreateWindowSurface() return error %d", eglGetError());
        return ERROR_CREATE_WINDOW_SURFACE;
    }

    return 0;
}