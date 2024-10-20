//
// Created by houtr on 2024-06-18.
//

#ifndef OPENGLSAMPLE_TOOLS_H
#define OPENGLSAMPLE_TOOLS_H

#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>

class Tools {
public:
    static long getTimestamp();

    static const char *readTextFromAssertManager(AAssetManager *assetManager, const char *fileName);

    static int bytesToInt(const unsigned char * bytes, int offset);
};


#endif //OPENGLSAMPLE_TOOLS_H
