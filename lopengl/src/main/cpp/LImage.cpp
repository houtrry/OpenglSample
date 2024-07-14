//
// Created by houtr on 2024-06-18.
//
#include "LImage.h"
#include <stdlib.h>
#include <string.h>
#include "stb_image.h"
#include "utils/Tools.h"
#include "landroidlog.h"

LImage::LImage() {

}

LImage::LImage(int w, int h, int t, U8_t *data) {
    width = w;
    height = h;
    type = t;
    int size = w * h * 4;
    imageData = data;
    if (size > 0 && data != nullptr) {
        imageData = (U8_t *) malloc(size);
        memcpy(imageData, data, size);
    } else {
        imageData = NULL;
    }
}

LImage::~LImage() {
    if (imageData) {
        free(imageData);
    }
}

int LImage::getWidth() const {
    return width;
}

int LImage::getHeight() const {
    return height;
}

int LImage::getType() const {
    return type;
}

U8_t *LImage::getImageData() const {
    return imageData;
}

void LImage::readFromBuffer(unsigned char *buff, int len) {
    int t, w, h = 0;
    long timeStart = Tools::getTimestamp();
    stbi_set_flip_vertically_on_load(true);
    imageData = stbi_load_from_memory(buff, len, &w, &h, &t, 0);
    width = w;
    height = h;
    type = t;
    LOGD("readFromBuffer cost time is %ld", Tools::getTimestamp() - timeStart);
}

void LImage::readFromFile(unsigned char *fileName) {
    int t, w, h = 0;
    long timeStart = Tools::getTimestamp();
    U8_t* picData = stbi_load((char const *)fileName, &w, &h, &t, STBI_rgb_alpha);
    int picDataSize = w * h * 4;
    if (picDataSize == 0 || picData == nullptr) {
        LOGD("readFromFile failure %s", fileName);
        stbi_image_free(picData);
        return;
    }
    imageData = (U8_t*)malloc(picDataSize);
    memcpy(imageData, picData, picDataSize);
    width = w;
    height = h;
    type = t;
    stbi_image_free(imageData);
    LOGD("readFromFile cost time is %ld", Tools::getTimestamp() - timeStart);
}
