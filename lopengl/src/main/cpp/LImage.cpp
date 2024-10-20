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
    channelSize = t;
    int size = w * h * channelSize;
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

int LImage::getChannelSize() const {
    return channelSize;
}

U8_t *LImage::getImageData() const {
    return imageData;
}

void LImage::readFromBuffer(unsigned char *buff, int len) {
    int t, w, h = 0;
    long timeStart = Tools::getTimestamp();
    ///翻转图片，解析出来的图片数据从左下角开始，这是因为OpenGL的纹理坐标起始点为左下角。
    stbi_set_flip_vertically_on_load(true);
    imageData = stbi_load_from_memory(buff, len, &w, &h, &t, STBI_default);
    width = w;
    height = h;
    channelSize = t;

    LOGD("readFromBuffer width=%d, height=%d, channel=%d", width, height, t);
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
    channelSize = t;


    stbi_image_free(imageData);
    LOGD("readFromFile cost time is %ld", Tools::getTimestamp() - timeStart);
}

void LImage::readFromGrayBuffer(unsigned char *buff) {
    long timeStart = Tools::getTimestamp();

//    int t, w, h = 0;
    /**
     * origin_x: double
     * origin_y: double
     * scale: double
     * width: int
     * height: int
     */

//    width = Tools::bytesToInt(buff, 24);
//    height = Tools::bytesToInt(buff, 28);
    width = Tools::bytesToInt(buff, 24);
    height = Tools::bytesToInt(buff, 28);
    long picDataSize = width * height;

//    stbi_set_flip_vertically_on_load(true);
//    imageData = stbi_load_from_memory((unsigned char *)(buff + 32), picDataSize, &w, &h, &t, STBI_grey);

    imageData = (U8_t*)malloc(picDataSize);
    memcpy(imageData, (unsigned char *)(buff + 32), picDataSize);

//    int line = height / 2;
////    int line = 0;
//    for (int i = 0; i < width; ++i) {
//        LOGD("readFromBuffer imageData[%d, %d] = %d == %d", i, line, imageData[line * width + i], buff[line * width + i + 32]);
//    }

    channelSize = 1;
//    LOGD("readFromBuffer w=%d, h=%d, t=%d ", w, h, t);
    LOGD("readFromBuffer width=%d, height=%d ", width, height);
    LOGD("readFromBuffer cost time is %ld,  width: %d,  height: %d", Tools::getTimestamp() - timeStart, width, height);
}

