//
// Created by houtr on 2024-06-18.
//

#ifndef OPENGLSAMPLE_LOPENGLRENDER_H
#define OPENGLSAMPLE_LOPENGLRENDER_H


#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>

class LOpenglRender {
public:
    LOpenglRender();
    ~LOpenglRender();
    void init();
    void resize(int width, int height);
    void draw();
    void setAssertManager(AAssetManager *assetManager);

private:
    int mWidth;
    int mHeight;
    float m_angle;
    AAssetManager *mAssetManager;
};


#endif //OPENGLSAMPLE_LOPENGLRENDER_H
