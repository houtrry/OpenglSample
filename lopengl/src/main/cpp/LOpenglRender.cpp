//
// Created by houtr on 2024-06-18.
//

#include <stdlib.h>
#include "LOpenglRender.h"
#include "landroidlog.h"
#include <GLES3/gl3.h>
#include <glm/vec2.hpp>
#include <glm/vec3.hpp>
#include <glm/mat4x4.hpp>
#include <glm/glm.hpp>
#include <glm/ext/matrix_transform.hpp>
#include <glm/ext/matrix_clip_space.hpp>
#include <glm/gtc/type_ptr.hpp>
#include "LOpenglPrimitivesDef.h"
#include "shape/CubeShape.h"
#include "shape/PolygonShape.h"
#include "sample/TriangleSample.h"

LOpenglRender::LOpenglRender() {

}
CubeShape cubeShape;
PolygonShape polygonShape;
TriangleSample triangleSample;

LOpenglRender::~LOpenglRender() {
    if (mAssetManager) {
        free(mAssetManager);
    }
}

void LOpenglRender::init() {
    LOGD("glCreate start");
    glClearColor(0.0, 0.0, 0.0, 1.0);
    glClearDepthf(1.0);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    LOGD("glCreate end");
//    cubeShape.generateDefaultVertex();
//    polygonShape.generateDefaultVertex();
    LFloat7 arr[] = {
            {-0.5, 0.1, -0.1, 1.0, 0.0, 0.0, 1.0},
            {-0.5, 0.9, -0.1, 0.0, 1.0, 0.0, 1.0},
            {0.5,  0.1, -0.1, 0.0, 0.0, 1.0, 1.0},
            {0.5,  0.9, -0.1, 1.0, 0.0, 0.0, 1.0},
    };
    triangleSample.setAssertManager(mAssetManager);
    triangleSample.init("triangle_vertex.glsl", "triangle_fragment.glsl");
    triangleSample.setVertex(arr, sizeof(arr));
}

void LOpenglRender::resize(int width, int height) {
    mWidth = width;
    mHeight = height;
    LOGD("glResize start");
    glViewport(0, 0, width, height);
//    glMatrixMode(GL_PROJECTION);
//    glLoadIdentity();
//    glOrthof(-1, 1, -1, 1, 0.1, 1000.0);
//    glLoadIdentity();
    LOGD("glResize end");
}

void LOpenglRender::draw() {
    LOGD("glDraw start");
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
//    glLoadIdentity();
//
//
//    glMatrixMode(GL_MODELVIEW);
//    glLoadIdentity();
//    m_angle += 0.01;
//    drawRotateCube(m_angle);
//    cubeShape.draw();
//    polygonShape.draw();
    triangleSample.draw();
}

void LOpenglRender::setAssertManager(AAssetManager *assetManager) {
    mAssetManager = assetManager;
}

