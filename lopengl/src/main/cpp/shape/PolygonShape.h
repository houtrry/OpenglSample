//
// Created by huangdejun on 2024/7/7.
//

#ifndef OPENGLSAMPLE_POLYGONSHAPE_H
#define OPENGLSAMPLE_POLYGONSHAPE_H

#include "../LOpenglPrimitivesDef.h"

/**
 * 平面多边形
 */
class PolygonShape {
public:
    PolygonShape();
    ~PolygonShape();

    void draw();
    void setVertex(int size, LFloat7* vertexArray);
    void generateDefaultVertex();

private:
    LFloat7* vertexArray;
    int vertexSize;
};


#endif //OPENGLSAMPLE_POLYGONSHAPE_H
