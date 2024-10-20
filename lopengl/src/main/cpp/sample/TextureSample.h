//
// Created by houtr on 2024-07-15.
//

#ifndef OPENGLSAMPLE_TEXTURESAMPLE_H
#define OPENGLSAMPLE_TEXTURESAMPLE_H

#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>
#include <GLES3/gl3.h>
#include "../LOpenglPrimitivesDef.h"
#include "../LGlBuffer.h"
#include "../LGlTexture.h"

class TextureSample {
public:
    TextureSample();

    ~TextureSample();

    void build();

    int init(const char *vertexShapeFileName, const char *fragmentShapeFileName);

    void setData(LFloat5 *vertexArray, int size, const char *fileName, bool isSourceData);

    void setAssertManager(AAssetManager *assetManager);

    void draw();

private:
    GLuint vertexShape;
    GLuint fragmentShape;
    GLuint program;
    GLuint vao;
    AAssetManager *aAssetManager;
    LGlTexture *texture;
    GLint m_SamplerLoc;
    const char *TAG = "TextureSample";
};


#endif //OPENGLSAMPLE_TEXTURESAMPLE_H
