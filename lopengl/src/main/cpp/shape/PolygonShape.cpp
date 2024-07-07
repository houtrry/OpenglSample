//
// Created by huangdejun on 2024/7/7.
//

#include "PolygonShape.h"
#include <GLES/gl.h>
#include "glm/mat4x4.hpp"
#include "glm/glm.hpp"
#include "glm/ext/matrix_transform.hpp"
#include "glm/ext/matrix_clip_space.hpp"
#include "glm/gtc/type_ptr.hpp"
#include "../landroidlog.h"

PolygonShape::PolygonShape() {

}

PolygonShape::~PolygonShape() {
    if (vertexArray) {
        free(vertexArray);
        vertexArray = nullptr;
    }
}

void PolygonShape::setVertex(int size, LFloat7 *vertexArray) {
    int dataSize = size * sizeof(LFloat7);
    this->vertexArray = (LFloat7*) malloc(dataSize);
    memcpy(this->vertexArray, vertexArray, dataSize);
    vertexSize = size;
}

void PolygonShape::draw() {
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), vertexArray);
    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), &vertexArray[0].r);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, vertexSize);

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}

void PolygonShape::generateDefaultVertex() {
    LFloat7* arr = new LFloat7[4];
    arr[0] = {-0.5, 0.1, -0.1, 1.0, 0.0, 0.0, 1.0};
    arr[1] = {-0.5, 0.9, -0.1, 0.0, 1.0, 0.0, 1.0};
    arr[2] = {0.5,  0.1, -0.1, 0.0, 0.0, 1.0, 1.0};
    arr[3] = {0.5,  0.9, -0.1, 1.0, 0.0, 0.0, 1.0};
    vertexArray = arr;
    vertexSize = 4;
}

