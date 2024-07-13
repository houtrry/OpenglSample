//
// Created by huangdejun on 2024-06-09.
//
#include "Triangle.h"
#include "landroidlog.h"
#include <GLES3/gl3.h>
#include <cstdlib>

Triangle::Triangle(LFloat7 *vertex, const int vertexSize) {
    LOGD("vertex %d, %s, %d, %d", vertexSize, *vertex, vertex->r, vertexTriangle);
    this->vertexTriangle = vertex;
    this->vertex_Size = vertexSize;
    LOGD("vertex %d, %f", vertex_Size, vertexTriangle->r);
}

Triangle::~Triangle() {
    if (vertexTriangle) {
        delete vertexTriangle;
//        free(vertexTriangle);
        vertexTriangle = nullptr;
    }
}

void Triangle::draw() {
//    glEnableClientState(GL_VERTEX_ARRAY);
//    glEnableClientState(GL_COLOR_ARRAY);
//
//    LOGD("<<<---draw vertex %d, %d", vertex_Size, this->vertexTriangle);
//    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), (const void *)(vertexTriangle));
////    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), &(vertexTriangle[0].r));
//
//    glDrawArrays(GL_TRIANGLE_STRIP, 0, this->vertex_Size);
//
//    glDisableClientState(GL_VERTEX_ARRAY);
//    glDisableClientState(GL_COLOR_ARRAY);
}