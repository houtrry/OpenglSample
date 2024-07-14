//
// Created by huangdejun on 2024/7/14.
//

#include "TriangleSample.h"
#include "../landroidlog.h"
#include "../utils/Tools.h"
#include "../utils/GlUtils.h"
#include <stdlib.h>
#include <string>

TriangleSample::TriangleSample() {
    vbo = new LGlBuffer(LGlBuffer::VertexBuffer, LGlBuffer::StaticDraw);
}

TriangleSample::~TriangleSample() {
    if (TAG) {
        free((void *) TAG);
        TAG = nullptr;
    }
    if (program > 0) {
        glDeleteProgram(program);
        program = -1;
    }
    if (vertexShape > 0) {
        glDeleteShader(vertexShape);
        vertexShape = -1;
    }
    if (fragmentShape > 0) {
        glDeleteShader(fragmentShape);
        fragmentShape = -1;
    }
    if (vbo) {
        delete vbo;
        vbo = nullptr;
    }
//    if (aAssetManager) {
//        delete aAssetManager;
//        aAssetManager = nullptr;
//    }
}

int TriangleSample::init(const char *vertexShapeFileName, const char *fragmentShapeFileName) {
    if (!aAssetManager) {
        LOGTE(TAG,
              "init failure, aAssetManager is null, you need call TriangleSample::setAssertManager first!!!");
        return -1;
    }
    const char *vertexShapeSourceText = Tools::readTextFromAssertManager(aAssetManager,
                                                                         vertexShapeFileName);
    if (vertexShapeSourceText == nullptr) {
        LOGTE(TAG, "init failure, load vertex shape result is null");
        return -2;
    }
    const char *fragmentShapeSourceText = Tools::readTextFromAssertManager(aAssetManager,
                                                                           fragmentShapeFileName);
    if (fragmentShapeSourceText == nullptr) {
        LOGTE(TAG, "init failure, load fragment shape result is null");
        free((void *)vertexShapeSourceText);
        return -3;
    }
    program = GlUtils::createProgram(vertexShapeSourceText,
                                     fragmentShapeSourceText,
                                     vertexShape,
                                     fragmentShape);
    free((void *)vertexShapeSourceText);
    free((void *)fragmentShapeSourceText);
    return 0;
}

void TriangleSample::setVertex(LFloat7 *vertexArray, int size) {
    LFloat7 arr[] = {
            {-0.5, 0.1, -0.1, 1.0, 0.0, 0.0, 1.0},
            {-0.5, 0.9, -0.1, 0.0, 1.0, 0.0, 1.0},
            {0.5,  0.1, -0.1, 0.0, 0.0, 1.0, 1.0},
            {0.5,  0.9, -0.1, 1.0, 0.0, 0.0, 1.0},
    };
    vbo->bind();
    vbo->setBuffer(arr, 4);
//    vbo->setBuffer(vertexArray, size);
    vbo->unbind();
    LOGTD(TAG, "setVertex end");
}

void TriangleSample::setAssertManager(AAssetManager *assetManager) {
    this->aAssetManager = assetManager;
}

void TriangleSample::draw() {
    LOGTD(TAG, "draw start");
    glUseProgram(program);
    vbo->bind();

    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, sizeof(LFloat7), (const void*)0);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 4, GL_FLOAT, GL_FALSE, sizeof(LFloat7), (const void*)(3 * sizeof(float)));
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    vbo->unbind();
    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glUseProgram(0);


//    LFloat7 arr[] = {
//            {-0.5, 0.1, -0.1, 1.0, 0.0, 0.0, 1.0},
//            {-0.5, 0.9, -0.1, 0.0, 1.0, 0.0, 1.0},
//            {0.5,  0.1, -0.1, 0.0, 0.0, 1.0, 1.0},
//            {0.5,  0.9, -0.1, 1.0, 0.0, 0.0, 1.0},
//    };
//    glUseProgram(program);
//
//    glEnableVertexAttribArray(0);
//    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, sizeof(LFloat7), &arr[0].x);
//    glEnableVertexAttribArray(1);
//    glVertexAttribPointer(1, 4, GL_FLOAT, GL_FALSE, sizeof(LFloat7), &arr[0].r);
//    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
//
//    glDisableVertexAttribArray(0);
//    glDisableVertexAttribArray(1);
//    glUseProgram(0);
    LOGTD(TAG, "draw end");
}
