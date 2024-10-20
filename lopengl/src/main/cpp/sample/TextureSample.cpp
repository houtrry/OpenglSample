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
    if (vao > 0) {
        glDeleteVertexArrays(1, &vao);
        vao = -1;
    }
}

void TextureSample::build() {
    glGenVertexArrays(1, &vao);
    texture = new LGlTexture();
//    vbo = new LGlBuffer(LGlBuffer::VertexBuffer, LGlBuffer::StaticDraw);
//    ebo = new LGlBuffer(LGlBuffer::IndexBuffer, LGlBuffer::StaticDraw);
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
    if (program) {
        m_SamplerLoc = glGetUniformLocation(program, "s_TextureMap");
    }
    free((void *)vertexShapeSourceText);
    free((void *)fragmentShapeSourceText);
    return 0;
}

void TextureSample::setData(LFloat5 *vertexArray, int size, const char *fileName, bool isSourceData) {
    LGlBuffer vbo(LGlBuffer::VertexBuffer, LGlBuffer::StaticDraw);
    LGlBuffer ebo(LGlBuffer::IndexBuffer, LGlBuffer::StaticDraw);
    glBindVertexArray(vao);

    vbo.bind();
    vbo.setBuffer(vertexArray, size);

    GLushort indices[] = { 0, 1, 2, 0, 2, 3 };
    ebo.bind();
    ebo.setBuffer(indices, sizeof(indices));

    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, sizeof(LFloat5),(const void*)0);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(LFloat5),(const void*)(3 * sizeof(float)));
    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);

    glBindVertexArray(0);
    vbo.unbind();
    ebo.unbind();


//    texture->createTextureFromAssertManager(aAssetManager, fileName);
    texture->createTextureFromAssertManager(aAssetManager, fileName, isSourceData);
}

void TextureSample::setAssertManager(AAssetManager *assetManager) {
    this->aAssetManager = assetManager;
}

void TextureSample::draw() {
    glUseProgram(program);
    glBindVertexArray(vao);
    glBindTexture(GL_TEXTURE_2D, texture->getTextureId());
//    glDrawArrays(GL_TRIANGLE_STRIP, 0, 3);
//    glBindTexture(GL_TEXTURE_2D, 0);

//    glUniform1i(m_SamplerLoc, 0);

    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, (const void *)0);
//    glBindTexture(GL_TEXTURE_2D, 0);
//    glBindVertexArray(0);
//    glUseProgram(0);


//===2.start=====
//    GLfloat verticesCoords[] = {
//            -1.0f,  0.5f, 0.0f,  // Position 0
//            -1.0f, -0.5f, 0.0f,  // Position 1
//            1.0f, -0.5f, 0.0f,   // Position 2
//            1.0f,  0.5f, 0.0f,   // Position 3
//    };
//
//    GLfloat textureCoords[] = {
//            0.0f,  0.0f,        // TexCoord 0
//            0.0f,  1.0f,        // TexCoord 1
//            1.0f,  1.0f,        // TexCoord 2
//            1.0f,  0.0f         // TexCoord 3
//    };
//
//    GLushort indices[] = { 0, 1, 2, 0, 2, 3 };
//
//    //upload RGBA image data
////    glActiveTexture(GL_TEXTURE0);
////    glBindTexture(GL_TEXTURE_2D, m_TextureId);
////    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, m_RenderImage.width, m_RenderImage.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, m_RenderImage.ppPlane[0]);
////    glBindTexture(GL_TEXTURE_2D, GL_NONE);
//
//    // Use the program object
//    glUseProgram (program);
//
//    // Load the vertex position
//    glVertexAttribPointer (0, 3, GL_FLOAT,
//                           GL_FALSE, 3 * sizeof (GLfloat), verticesCoords);
//    // Load the texture coordinate
//    glVertexAttribPointer (1, 2, GL_FLOAT,
//                           GL_FALSE, 2 * sizeof (GLfloat), textureCoords);
//
//    glEnableVertexAttribArray (0);
//    glEnableVertexAttribArray (1);
//
//    // Bind the RGBA map
//    glActiveTexture(GL_TEXTURE0);
//    glBindTexture(GL_TEXTURE_2D, texture->getTextureId());
//
//    // Set the RGBA map sampler to texture unit to 0
//    glUniform1i(m_SamplerLoc, 0);
//
//    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, indices);
//===2.end=====


//===3.start=====
//    glUseProgram(program);
//    GLushort indices[] = { 0, 1, 2, 1, 2, 3 };
//    LFloat5 arr[] = {
//            {-0.5, 0.1, -0.1, 0.0, 0.0},
//            {-0.5, 0.9, -0.1, 0.0, 1.0},
//            {0.5,  0.1, -0.1, 1.0, 0.0},
//            {0.5,  0.9, -0.1, 1.0, 1.0},
//    };
//    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, sizeof(LFloat5), &arr[0].x);
//    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(LFloat5), &arr[0].u);
//    glEnableVertexAttribArray(0);
//    glEnableVertexAttribArray(1);
//
//    glBindTexture(GL_TEXTURE_2D, texture->getTextureId());
//    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, indices);
//===3.end=====

}
