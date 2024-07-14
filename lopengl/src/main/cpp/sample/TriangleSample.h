//
// Created by huangdejun on 2024/7/14.
//

#ifndef OPENGLSAMPLE_TRIANGLESAMPLE_H
#define OPENGLSAMPLE_TRIANGLESAMPLE_H

#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>
#include <GLES3/gl3.h>
#include "../LOpenglPrimitivesDef.h"
#include "../LGlBuffer.h"

class TriangleSample {

public:
    TriangleSample();

    ~TriangleSample();

    int init(const char *vertexShapeFileName, const char *fragmentShapeFileName);

    void setVertex(LFloat7 *vertexArray, int size);

    void setAssertManager(AAssetManager *assetManager);

    void draw();

private:
    GLuint vertexShape;
    GLuint fragmentShape;
    GLuint program;
    GLuint m_VBO;
    AAssetManager *aAssetManager;
    LGlBuffer *vbo;
    const char * TAG = "TriangleSample";
};


#endif //OPENGLSAMPLE_TRIANGLESAMPLE_H
