//
// Created by huangdejun on 2024/7/7.
//

#include "PolygonShape.h"
#include <GLES3/gl3.h>
#include "glm/mat4x4.hpp"
#include "glm/glm.hpp"
#include "glm/ext/matrix_transform.hpp"
#include "glm/ext/matrix_clip_space.hpp"
#include "glm/gtc/type_ptr.hpp"
#include "../landroidlog.h"

PolygonShape::PolygonShape() {
    vbo = new LGlBuffer(LGlBuffer::VertexBuffer, LGlBuffer::StaticDraw);
}

PolygonShape::~PolygonShape() {
    if (vbo) {
        delete vbo;
        vbo = nullptr;
    }
}

void PolygonShape::setVertex(int size, LFloat7 *vertexArray) {
    vbo->bind();
    vbo->setBuffer(vertexArray, size);
    vbo->unbind();
}

void PolygonShape::draw() {
//    glEnableClientState(GL_VERTEX_ARRAY);
//    glEnableClientState(GL_COLOR_ARRAY);
//
//    vbo->bind();
//    float* vertexAddress = (float*)0;
//    float* colorAddress = (float*)12;
//    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), vertexAddress);
//    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), colorAddress);
//
//    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
//
//    vbo->unbind();
//    glDisableClientState(GL_VERTEX_ARRAY);
//    glDisableClientState(GL_COLOR_ARRAY);
}

void PolygonShape::generateDefaultVertex() {
    LFloat7 arr[] = {
            {-0.5, 0.1, -0.1, 1.0, 0.0, 0.0, 1.0},
            {-0.5, 0.9, -0.1, 0.0, 1.0, 0.0, 1.0},
            {0.5,  0.1, -0.1, 0.0, 0.0, 1.0, 1.0},
            {0.5,  0.9, -0.1, 1.0, 0.0, 0.0, 1.0},
    };
    vbo->bind();
    vbo->setBuffer(arr, sizeof(arr));
    vbo->unbind();
}

