//
// Created by huangdejun on 2024-06-15.
//

#ifndef OPENGLSAMPLE_LIMAGE_H
#define OPENGLSAMPLE_LIMAGE_H

#define STB_IMAGE_IMPLEMENTATION

//extern "C" {
//#include "stb_image.h"
//}
#ifndef U8_t
#define U8_t unsigned char
#endif

class LImage {
public:
    LImage();
    LImage(int w, int h, int t, U8_t *data);

    ~LImage();

    int getWidth() const;

    int getHeight() const;

    int getType() const;

    U8_t *getImageData() const;

    void readFromBuffer(U8_t *buff, int len);

    void readFromFile(U8_t *fileName);

    void readFromGrayBuffer(U8_t *buff);

private:
    int width = 0;
    int height = 0;
    int type = 0;
    U8_t *imageData;
};

#endif //OPENGLSAMPLE_LIMAGE_H
