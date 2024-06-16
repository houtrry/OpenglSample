//
// Created by huangdejun on 2024-06-15.
//

#ifndef OPENGLSAMPLE_LIMAGE_H
#define OPENGLSAMPLE_LIMAGE_H

#define STB_IMAGE_IMPLEMENTATION

#include <stdlib.h>
#include <string.h>
#include "stb_image.h"
#include "utils.h"

//extern "C" {
//#include "stb_image.h"
//}
#ifndef U8_t
#define U8_t unsigned char
#endif

class LImage {
public:
    LImage(int w, int h, int t, U8_t *data) {
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

    ~LImage() {
        if (imageData) {
            free(imageData);
        }
    }

    int getWidth() const { return width; }

    int getHeight() const { return height; }

    int getType() const { return type; }

    U8_t *getImageData() const { return imageData; }

    static LImage *readFromBuffer(U8_t *buff, int len) {
        int type, width, height = 0;
        long timeStart = getTimestamp();
        stbi_set_flip_vertically_on_load(true);
        U8_t* picData = stbi_load_from_memory(buff, len, &width, &height, &type, 0);
        LImage* image = new LImage(width, height, type, picData);
        stbi_image_free(picData);
        LOGD("readFromBuffer cost time is %ld", getTimestamp() - timeStart);
        return image;
    }

private:
    int width = 0;
    int height = 0;
    int type = 0;
    U8_t *imageData = NULL;
};

#endif //OPENGLSAMPLE_LIMAGE_H
