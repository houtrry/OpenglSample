//
// Created by huangdejun on 2024/7/6.
//

#include "LGlTexture.h"
#include "landroidlog.h"
#include <malloc.h>
#include <string.h>
#include "utils/Tools.h"


LGlTexture::LGlTexture() {

}

LGlTexture::~LGlTexture() {
    if (glTextureId >= 0) {
        glDeleteTextures(1, &glTextureId);
        glTextureId = -1;
    }
}

GLuint LGlTexture::getTextureId() {
    return glTextureId;
}

GLuint LGlTexture::createTextureFromFile(char const *fileName) {
    glTextureId = generateTextureFromFile(fileName);
    return glTextureId;
}

GLuint LGlTexture::generateTextureFromFile(char const *fileName) {
    LImage *glImage = new LImage();
    long startTimestamp = Tools::getTimestamp();
    glImage->readFromFile((unsigned char *)fileName);
    LOGD("  image: %d, %d, and read image file %s cost time is %ld", glImage->getWidth(), glImage->getHeight(), fileName, Tools::getTimestamp() - startTimestamp);
    GLuint textureId = createGlTexture(glImage);
    delete glImage;
    return textureId;
}

GLuint LGlTexture::createGlTexture(LImage *image) {
    GLuint textureId;

//    glEnable(GL_TEXTURE_2D);
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

GLuint
LGlTexture::createTextureFromAssertManager(AAssetManager *assetManager, const char *fileName) {
    glTextureId = generateTextureFromAssertManager(assetManager, fileName);
    return glTextureId;
}

GLuint
LGlTexture::generateTextureFromAssertManager(AAssetManager *assetManager, const char *fileName) {
    if (nullptr == assetManager) {
        LOGF("assetManager is NULL");
        return -1;
    }
    if (nullptr == fileName) {
        LOGF("fileName is NULL");
        return -1;
    }
    long startTimestamp = Tools::getTimestamp();
    LOGD ("FileName is %s", fileName);
    AAsset *asset = AAssetManager_open(assetManager, fileName, AASSET_MODE_UNKNOWN);
    if (nullptr == asset) {
        LOGF("asset is NULL");
        return -1;
    }
    off_t bufferSize = AAsset_getLength(asset);
    LOGD("  buffer size is %ld", bufferSize);

    U8_t *imgBuff = (U8_t *) malloc(bufferSize + 1);
    if (nullptr == imgBuff) {
        LOGF("imgBuff alloc failed");
        return -1;
    }
    memset(imgBuff, 0, bufferSize + 1);
    int readLen = AAsset_read(asset, imgBuff, bufferSize);
    LOGD("  Picture read: %d", readLen);

    LImage *glImage = new LImage();
    glImage->readFromBuffer(imgBuff, readLen);
    LOGD("  image: %d, %d, and read image file %s cost time is %ld", glImage->getWidth(), glImage->getHeight(), fileName, Tools::getTimestamp() - startTimestamp);
    GLuint textureId = createGlTexture(glImage);
    delete glImage;

    if (imgBuff) {
        free(imgBuff);
        imgBuff = nullptr;
    }
    AAsset_close(asset);
    return textureId;
}
