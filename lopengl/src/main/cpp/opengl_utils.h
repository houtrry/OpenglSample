//
// Created by huangdejun on 2024/6/16.
//

#ifndef OPENGLSAMPLE_OPENGL_UTILS_H
#define OPENGLSAMPLE_OPENGL_UTILS_H

#include "LImage.h"
#include <glm/glm.hpp>
#include <glm/ext/matrix_transform.hpp>
#include <glm/ext/matrix_clip_space.hpp>
#include <glm/gtc/type_ptr.hpp>
#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>

/**
 * 创建纹理ID
 * @return
 */
GLuint createTexture(LImage *image) {
    //声明纹理id
    GLuint textureId;

    glEnable(GL_TEXTURE_2D);
    //生成纹理id
    glGenTextures(1, &textureId);
    //绑定纹理id，之后的操作都针对当前的纹理索引
    glBindTexture(GL_TEXTURE_2D, textureId);
    //声明
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    //声明环绕方向
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    //指定参数， 生成纹理（向GPU提交纹理数据）
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
                 image->getWidth(), image->getHeight(),
                 0, GL_RGBA, GL_UNSIGNED_BYTE,
                 image->getImageData());
    return textureId;
}

/**
 * 使用AssertsManager读取图片，并生成纹理ID
 * @param mAssetManager
 * @param fileName
 * @return
 */
GLuint readSourceFromAssertManager(AAssetManager *mAssetManager, const char *fileName) {
    if (NULL == mAssetManager) {
        LOGF("mAssetManager is NULL");
        return -1;
    }
    if (NULL == fileName) {
        LOGF("fileName is NULL");
        return -1;
    }
    LOGD ("FileName is %s", fileName);
    AAsset *asset = AAssetManager_open(mAssetManager, fileName, AASSET_MODE_UNKNOWN);
    if (NULL == asset) {
        LOGF("asset is NULL");
        return -1;
    }
    off_t bufferSize = AAsset_getLength(asset);
    LOGD("  buffer size is %ld", bufferSize);

    U8_t *imgBuff = (U8_t *) malloc(bufferSize + 1);
    if (NULL == imgBuff) {
        LOGF("imgBuff alloc failed");
        return -1;
    }
    memset(imgBuff, 0, bufferSize + 1);
    int readLen = AAsset_read(asset, imgBuff, bufferSize);
    LOGD("  Picture read: %d", readLen);

    LImage *glImage = LImage::readFromBuffer(imgBuff, readLen);
    LOGD("  image: %d, %d", glImage->getWidth(), glImage->getHeight());
    GLuint textureId = createTexture(glImage);
    delete glImage;

    if (imgBuff) {
        free(imgBuff);
        imgBuff = NULL;
    }
    AAsset_close(asset);
    return textureId;
}

#endif //OPENGLSAMPLE_OPENGL_UTILS_H
