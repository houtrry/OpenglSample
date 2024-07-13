//
// Created by huangdejun on 2024/7/6.
//

#include "CubeShape.h"
#include <GLES/gl.h>
#include "glm/mat4x4.hpp"
#include "glm/glm.hpp"
#include "glm/ext/matrix_transform.hpp"
#include "glm/ext/matrix_clip_space.hpp"
#include "glm/gtc/type_ptr.hpp"
#include "../landroidlog.h"

CubeShape::CubeShape() {
    vbo = new LGlBuffer(LGlBuffer::VertexBuffer, LGlBuffer::StaticDraw);
}

CubeShape::~CubeShape() {
    if (vbo) {
        delete vbo;
        vbo = nullptr;
    }
}

void CubeShape::setVertex(int size, LFloat7 *vertexArray) {
    vbo->bind();
    vbo->setBuffer(vertexArray, size);
    vbo->unbind();
}

void CubeShape::draw() {
    LOGD("  drawVertex -- size is");
    glCullFace(GL_BACK);
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);

    vbo->bind();
    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), (float *) 0);
    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), (float *) 12);

    //注意这里，m_angle要想不停变化
    //需要不停的刷新
    //opengl的渲染模式，就不能是RENDERMODE_WHEN_DIRTY

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;
    glLoadMatrixf(glm::value_ptr(cubeMat));
    angle += 0.01;

    glDrawArrays(GL_TRIANGLES, 0, 36);

    vbo->unbind();
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}

void CubeShape::generateDefaultVertex() {
    LFloat7 arr[] = {
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
    vbo->bind();
    vbo->setBuffer(arr, 36);
    vbo->unbind();
    LOGD("  drawVertex size is %d", 36);
}
