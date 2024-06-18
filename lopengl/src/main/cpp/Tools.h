//
// Created by houtr on 2024-06-18.
//

#ifndef OPENGLSAMPLE_TOOLS_H
#define OPENGLSAMPLE_TOOLS_H
#include "LImage.h"
#include <GLES/gl.h>
#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>

class Tools {
public:
    static long getTimestamp();
    static GLuint createTexture(LImage *image);
    static GLuint readSourceFromAssertManager(AAssetManager *mAssetManager, const char *fileName);
};


#endif //OPENGLSAMPLE_TOOLS_H
