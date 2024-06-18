#include <string>
#include <GLES/gl.h>
#include <GLES2/gl2.h>
#include "lopengl.h"
#include "landroidlog.h"
#include "LOpenglPrimitivesDef.h"
#include "Triangle.h"
#include "Tools.h"
#include <glm/vec2.hpp>
#include <glm/vec3.hpp>
#include <glm/mat4x4.hpp>
#include <glm/glm.hpp>
#include <glm/ext/matrix_transform.hpp>
#include <glm/ext/matrix_clip_space.hpp>
#include <glm/gtc/type_ptr.hpp>
#include "LOpenglRender.h"

#ifdef __cplusplus

extern "C" {
#endif

float m_angle = 0.0f;
GLuint mTextureIds[6];

/**
 * 绘制纹理混合（同一个面绘制多个问题）
 */
void drawCombineTexture() {
    glCullFace(GL_BACK);
    LFloat5 cubeVertex[] = {
            {-0.8f, -0.8f, -0.5f, 0.0, 0.0},
            {0.8f,  -0.8f, -0.5f, 1.0, 0.0},
            {0.8f,  0.8f,  -0.5f, 1.0, 1.0},
            {0.8f,  0.8f,  -0.5f, 1.0, 1.0},
            {-0.8f, 0.8f,  -0.5f, 0.0, 1.0},
            {-0.8f, -0.8f, -0.5f, 0.0, 0.0},
    };
    //激活纹理0， 绑定纹理0的数据
    glActiveTexture(GL_TEXTURE0);
    glEnable(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, mTextureIds[0]);

    //激活纹理1， 绑定纹理1的数据
    glActiveTexture(GL_TEXTURE1);
    glEnable(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, mTextureIds[5]);

    //提交顶点数据
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, sizeof(LFloat5), cubeVertex);

    //提交纹理0的数据
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glClientActiveTexture(GL_TEXTURE0);
    glTexCoordPointer(2, GL_FLOAT, sizeof(LFloat5), &cubeVertex[0].u);

    //提交纹理1的数据
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glClientActiveTexture(GL_TEXTURE1);
    glTexCoordPointer(2, GL_FLOAT, sizeof(LFloat5), &cubeVertex[0].u);

    //设置纹理混合方式
    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_ADD);

    //注意这里，m_angle要想不停变化
    //需要不停的刷新
    //opengl的渲染模式，就不能是RENDERMODE_WHEN_DIRTY
//    m_angle += 0.01f;

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), m_angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;

    glLoadMatrixf(glm::value_ptr(cubeMat));


    glDrawArrays(GL_TRIANGLES, 0, 6);


    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
}
/**
 * 绘制旋转的纹理图片
 */
void drawRotateTextureCube() {
    glCullFace(GL_BACK);
    glFrontFace(GL_CW);
    LFloat5 cubeVertex[] = {
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},

            {-0.5f, -0.5f, 0.5f,  0.0, 0.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 0.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 0.0},

            {-0.5f, 0.5f,  0.5f,  0.0, 0.0},
            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 0.0},

            {0.5f,  0.5f,  0.5f,  0.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 0.0},

            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},

            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0}
    };
    LOGD("draw start");
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat5), cubeVertex);
    glTexCoordPointer(2, GL_FLOAT, sizeof(LFloat5), &cubeVertex[0].u);

    //注意这里，m_angle要想不停变化
    //需要不停的刷新
    //opengl的渲染模式，就不能是RENDERMODE_WHEN_DIRTY
    m_angle += 0.01f;

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), m_angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;

    glLoadMatrixf(glm::value_ptr(cubeMat));


    for (int i = 0; i < 6; ++i) {
        LOGD("draw %d -> %d", i, mTextureIds[i]);
        glBindTexture(GL_TEXTURE_2D, mTextureIds[i]);
        glDrawArrays(GL_TRIANGLES, 6 * i, 6);
    }

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
}

GLuint mTextureId = 0;
/**
 * 绘制纹理图片
 */
void drawTexture() {
    glCullFace(GL_BACK);
    LFloat5 cubeVertex[] = {
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},

            {-0.5f, -0.5f, 0.5f,  0.0, 0.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 0.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 0.0},

            {-0.5f, 0.5f,  0.5f,  0.0, 0.0},
            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 0.0},

            {0.5f,  0.5f,  0.5f,  0.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 0.0},

            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},

            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0}
    };
    glBindTexture(GL_TEXTURE_2D, mTextureId);
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat5), cubeVertex);
    glTexCoordPointer(2, GL_FLOAT, sizeof(LFloat5), &cubeVertex[0].u);

    //注意这里，m_angle要想不停变化
    //需要不停的刷新
    //opengl的渲染模式，就不能是RENDERMODE_WHEN_DIRTY
    m_angle += 0.01f;

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), m_angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;

    glLoadMatrixf(glm::value_ptr(cubeMat));


    glDrawArrays(GL_TRIANGLES, 0, 36);


    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
}

/**
 * 绘制立方体
 */
void drawCube() {
    glCullFace(GL_BACK);
    LFloat7 cubeVertex[] = {
            {-0.5f, -0.5f, -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 0.0, 0.0, 1.0},

            {-0.5f, -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 1.0, 0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 1.0, 0.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0, 0.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},

            {-0.5f, 0.5f,  0.5f,  0.0, 0.0, 1.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0, 1.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0, 1.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0, 1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 0.0, 1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 0.0, 1.0, 1.0},

            {0.5f,  0.5f,  0.5f,  0.0, 0.0, 1.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 0.0, 0.0, 1.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 0.0, 0.0, 1.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 0.0, 0.0, 1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 0.0, 1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 0.0, 1.0, 1.0},

            {-0.5f, -0.5f, -0.5f, 0.0, 1.0, 0.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 0.0, 1.0, 0.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 1.0, 0.0, 1.0},

            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 0.0, 0.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  1.0, 0.0, 0.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0}
    };
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), cubeVertex);
    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), &cubeVertex[0].r);

    glDrawArrays(GL_TRIANGLES, 0, 36);


    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}

/**
 * 绘制正方形
 * 绘制三角形
 */
void drawTriangle() {
    LFloat7 vertexTriangle[] = {
            {-0.5, 0.1, -0.1, 1.0, 0.0, 0.0, 1.0},
            {-0.5, 0.9, -0.1, 0.0, 1.0, 0.0, 1.0},
            {0.5,  0.1, -0.1, 0.0, 0.0, 1.0, 1.0},
            {0.5,  0.9, -0.1, 1.0, 0.0, 0.0, 1.0},
    };
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), vertexTriangle);
    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), &vertexTriangle[0].r);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);

    LFloat7 vertexTriangle2[] = {
            {-0.0, 0.0,  -0.1, 1.0, 0.0, 0.0, 1.0},
            {-0.5, -0.5, -0.1, 0.0, 1.0, 0.0, 1.0},
            {0.5,  -0.5, -0.1, 0.0, 0.0, 1.0, 1.0},
    };
    //向GPU提交顶点坐标和颜色数据
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), vertexTriangle2);
    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), &vertexTriangle2[0].r);

    glDrawArrays(GL_TRIANGLES, 0, 3);

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}

LOpenglRender openglRender;

void ndkCreate(JNIEnv *env, jobject thiz) {
    openglRender.init();
}

void ndkResize(JNIEnv *env, jobject thiz, jint width,
                                                     jint height) {
    openglRender.resize(width, height);
}

void ndkDraw(JNIEnv *env, jobject thiz) {
    openglRender.draw();

//    drawTriangle();
//    drawCube();
//    drawRotateCube();
//    drawTexture();
//    drawRotateTextureCube();
//    drawCombineTexture();
}

jint ndkReadAssertManager(JNIEnv *env, jobject thiz,
                                                                jobject asset_manager,
                                                                jstring name) {
    AAssetManager *mAssetManager = AAssetManager_fromJava(env, asset_manager);
    openglRender.setAssertManager(mAssetManager);
//    const char *fileName = env->GetStringUTFChars(name, 0);
//    mTextureId = Tools::readSourceFromAssertManager(mAssetManager, fileName);
//    env->ReleaseStringUTFChars(name, fileName);
    return 0;
}

jboolean ndkReadAssertManagers(JNIEnv *env, jobject thiz,
                                                                     jobject asset_manager,
                                                                     jobjectArray names) {

    AAssetManager *mAssetManager = AAssetManager_fromJava(env, asset_manager);
    openglRender.setAssertManager(mAssetManager);
//    if (NULL == mAssetManager) {
//        LOGF("assetManager is NULL");
//        return false;
//    }
////    int size = env->GetArrayLength(names);
//    long startTimestamp = Tools::getTimestamp();
//    for (int i = 0; i < 6; i++) {
//        jobject jobject = env->GetObjectArrayElement(names, i);
//        jstring jstr = static_cast<jstring>(jobject);
//        const char *fileName = env->GetStringUTFChars(jstr, 0);
//        mTextureIds[i] = Tools::readSourceFromAssertManager(mAssetManager, fileName);
//        env->ReleaseStringUTFChars(jstr, fileName);
//    }
//    LOGD("ndkReadAssertManagers end, and cost time is %ld", Tools::getTimestamp() - startTimestamp);
    return true;
}


static const JNINativeMethod nativeMethod[] = {
        // Java中的函数名    // 函数签名信息    // native的函数指针
        {"ndkCreate", "()V", (void *) (ndkCreate)},
        {"ndkResize", "(II)V", (void *) (ndkResize)},
        {"ndkDraw", "()V", (void *) (ndkDraw)},
        {"ndkReadAssertManager", "(Landroid/content/res/AssetManager;Ljava/lang/String;)I", (void *) (ndkReadAssertManager)},
        {"ndkReadAssertManagers", "(Landroid/content/res/AssetManager;[Ljava/lang/String;)Z", (void *) (ndkReadAssertManagers)},
};
const char *target_class_name = "com/houtrry/lopengl/view/MapView";
// 类库加载时自动调用
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reversed)
{
    JNIEnv *env = NULL;
    // 初始化JNIEnv
    if(vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK){
        return JNI_FALSE;
    }
    // 找到需要动态动态注册的Jni类
    jclass jniClass = env->FindClass(target_class_name);
    if(nullptr == jniClass){
        return JNI_FALSE;
    }
    // 动态注册
    jint registerResult = env->RegisterNatives(jniClass, nativeMethod, sizeof(nativeMethod)/sizeof(JNINativeMethod));
    if (registerResult) {
        LOGD("register method successfully.");
    } else {
        LOGE("register method failure!!!");
    }
    // 返回JNI使用的版本
    return JNI_VERSION_1_6;
}
#ifdef __cplusplus
}
#endif
