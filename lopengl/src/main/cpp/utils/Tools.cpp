//
// Created by houtr on 2024-06-18.
//

#include "Tools.h"
#include "../landroidlog.h"
#include "../LImage.h"
#include <time.h>
#include <stdlib.h>
#include <cstring>


long Tools::getTimestamp() {
    struct timespec ts{};
    clock_gettime(CLOCK_REALTIME, &ts);
    return ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

const char *Tools::readTextFromAssertManager(AAssetManager *assetManager, const char *fileName) {
    if (assetManager == nullptr) {
        LOGF("assetManager is NULL");
        return nullptr;
    }
    if (fileName == nullptr) {
        LOGF("fileName is NULL");
        return nullptr;
    }
    long startTimestamp = Tools::getTimestamp();
    LOGD ("FileName is %s", fileName);
    AAsset *asset = AAssetManager_open(assetManager, fileName, AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        LOGF("asset is NULL");
        return nullptr;
    }
    size_t bufferSize = AAsset_getLength(asset);
    LOGD("buffer size is %d", bufferSize);
    char *dataBuffer = (char *)  malloc(bufferSize);
    if (dataBuffer == nullptr) {
        LOGF("dataBuff alloc failed");
        return nullptr;
    }
    int readLen = AAsset_read(asset, dataBuffer, bufferSize);
    LOGD("read %s from assertManager cost time is %ld, and content(%d) is \n%s", fileName, Tools::getTimestamp() - startTimestamp, readLen, dataBuffer);
    return dataBuffer;

//    AAsset* file = AAssetManager_open(assetManager,fileName, AASSET_MODE_BUFFER);
//    size_t shaderSize = AAsset_getLength(file);
//
//    char* sContentBuff = (char*)malloc(shaderSize);
//    AAsset_read(file, sContentBuff, shaderSize);
//    LOGD("SHADERS: %s",sContentBuff);
//    return sContentBuff;
}
