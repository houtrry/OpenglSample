//
// Created by houtr on 2024-06-18.
//
#include "LImage.h"
#include <stdlib.h>
#include <string.h>
#include "stb_image.h"
#include "Tools.h"
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
