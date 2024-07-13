//
// Created by huangdejun on 2024/7/6.
//

#ifndef OPENGLSAMPLE_CUBESHAPE_H
#define OPENGLSAMPLE_CUBESHAPE_H


#include "../LOpenglPrimitivesDef.h"
#include "../LGlBuffer.h"

/**
 * 立方体
 */
class CubeShape {
public:
    CubeShape();
    ~CubeShape();
    void setVertex(int size, LFloat7* vertexArray);
    void draw();
    void generateDefaultVertex();
private:
    float angle;
    LGlBuffer* vbo;
};


#endif //OPENGLSAMPLE_CUBESHAPE_H
