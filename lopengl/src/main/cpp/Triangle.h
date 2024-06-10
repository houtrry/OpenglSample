//
// Created by huangdejun on 2024-06-09.
//

#ifndef OPENGLSAMPLE_TRIANGLE_H
#define OPENGLSAMPLE_TRIANGLE_H

#include "LOpenglPrimitivesDef.h"

class Triangle {
public:
    Triangle(LFloat7 vertex[], const int vertexSize);
    ~Triangle();
    void draw();

private:
    LFloat7 *vertexTriangle;
     int vertex_Size;
};
#endif //OPENGLSAMPLE_TRIANGLE_H
