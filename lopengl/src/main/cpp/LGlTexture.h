//
// Created by huangdejun on 2024/7/6.
//

#ifndef OPENGLSAMPLE_LGLTEXTURE_H
#define OPENGLSAMPLE_LGLTEXTURE_H

#include "LImage.h"
#include <GLES/gl.h>
#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>


class LGlTexture {
public:
    LGlTexture();

    ~LGlTexture();

    GLuint getTextureId();

    GLuint createTextureFromFile(char const *fileName);

    GLuint createTextureFromAssertManager(AAssetManager *assetManager, char const *fileName);

private:
    GLuint generateTextureFromFile(char const *fileName);

    GLuint generateTextureFromAssertManager(AAssetManager *assetManager, char const *fileName);

    GLuint createGlTexture(LImage *image);

    GLuint glTextureId;
};


#endif //OPENGLSAMPLE_LGLTEXTURE_H
