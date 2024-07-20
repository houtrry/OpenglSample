//
// Created by houtr on 2024-07-15.
//

#include "TextureSample.h"
#include "../landroidlog.h"
#include "../utils/Tools.h"
#include "../utils/GlUtils.h"
#include <stdlib.h>
#include <string>

TextureSample::TextureSample() {
    vbo = new LGlBuffer(LGlBuffer::IndexBuffer, LGlBuffer::StaticDraw);
    glGenVertexArrays(1, &vao);
    texture = new LGlTexture();
}

TextureSample::~TextureSample() {
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
    if (texture) {
        delete texture;
        texture = nullptr;
    }
    if (vbo) {
        delete vbo;
        vbo = nullptr;
    }
    if (vao > 0) {
        glDeleteVertexArrays(1, &vao);
        vao = -1;
    }
}

int TextureSample::init(const char *vertexShapeFileName, const char *fragmentShapeFileName) {
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

void TextureSample::setData(LFloat5 *vertexArray, int size, const char *fileName) {
    glBindVertexArray(vao);
    vbo->bind();
    vbo->setBuffer(vertexArray, size);

    texture->createTextureFromAssertManager(aAssetManager, fileName);

    glBindVertexArray(0);
}

void TextureSample::setAssertManager(AAssetManager *assetManager) {
    this->aAssetManager = assetManager;
}

void TextureSample::draw() {
    glUseProgram(program);

    glBindVertexArray(vao);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glBindVertexArray(0);
    glUseProgram(0);
}
