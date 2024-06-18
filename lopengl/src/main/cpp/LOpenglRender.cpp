//
// Created by houtr on 2024-06-18.
//

#include <stdlib.h>
#include "LOpenglRender.h"
#include "landroidlog.h"
#include <GLES/gl.h>
#include <glm/vec2.hpp>
#include <glm/vec3.hpp>
#include <glm/mat4x4.hpp>
#include <glm/glm.hpp>
#include <glm/ext/matrix_transform.hpp>
#include <glm/ext/matrix_clip_space.hpp>
#include <glm/gtc/type_ptr.hpp>
#include "LOpenglPrimitivesDef.h"

/**
 * 绘制旋转的立方体
 */
void drawRotateCube(float angle) {
    glCullFace(GL_BACK);
    LFloat7 cubeVertex[] = {
            {-0.5f, -0.5f, -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 0.0, 0.0, 1.0},

            {-0.5f, -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 1.0, 0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 1.0, 0.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0, 0.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},

            {-0.5f, 0.5f,  0.5f,  0.0, 0.0, 1.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0, 1.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0, 1.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0, 1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 0.0, 1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 0.0, 1.0, 1.0},

            {0.5f,  0.5f,  0.5f,  0.0, 0.0, 1.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 0.0, 0.0, 1.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 0.0, 0.0, 1.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 0.0, 0.0, 1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 0.0, 1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 0.0, 1.0, 1.0},

            {-0.5f, -0.5f, -0.5f, 0.0, 1.0, 0.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 0.0, 1.0, 0.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0, 0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 1.0, 0.0, 1.0},

            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 0.0, 0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 0.0, 0.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  1.0, 0.0, 0.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0, 0.0, 1.0}
    };
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), cubeVertex);
    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), &cubeVertex[0].r);

    //注意这里，m_angle要想不停变化
    //需要不停的刷新
    //opengl的渲染模式，就不能是RENDERMODE_WHEN_DIRTY

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;

    glLoadMatrixf(glm::value_ptr(cubeMat));


    glDrawArrays(GL_TRIANGLES, 0, 36);


    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}
LOpenglRender::LOpenglRender() {

}

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
}

void LOpenglRender::resize(int width, int height) {
    mWidth = width;
    mHeight = height;
    LOGD("glResize start");
    glViewport(0, 0, width, height);
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glOrthof(-1, 1, -1, 1, 0.1, 1000.0);
    glLoadIdentity();
    LOGD("glResize end");
}

void LOpenglRender::draw() {
    LOGD("glDraw start");
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glLoadIdentity();


    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    m_angle += 0.01;
    drawRotateCube(m_angle);
}

void LOpenglRender::setAssertManager(AAssetManager *assetManager) {
    mAssetManager = assetManager;
}

