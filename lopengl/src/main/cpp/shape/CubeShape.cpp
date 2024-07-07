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

}

CubeShape::~CubeShape() {
    if (cubeVertex) {
        free(cubeVertex);
        cubeVertex = nullptr;
    }
}

void CubeShape::setVertex(int size, LFloat7 *vertexArray) {
    int dataSize = size * sizeof(LFloat7);
    cubeVertex = (LFloat7*) malloc(dataSize);
    memcpy(cubeVertex, vertexArray, dataSize);
    vertexSize = size;
}

void CubeShape::draw() {
    LOGD("  drawVertex -- size is %d", vertexSize);
    glCullFace(GL_BACK);
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
    angle += 0.01;

    glDrawArrays(GL_TRIANGLES, 0, vertexSize);

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}

void CubeShape::generateDefaultVertex() {
    LFloat7* arr = new LFloat7[36];
    arr[0] = {-0.5f, -0.5f, -0.5f, 1.0, 0.0, 0.0, 1.0};
    arr[1] = {0.5f,  -0.5f, -0.5f, 1.0, 0.0, 0.5, 1.0};
    arr[2] = {0.5f,  0.5f,  -0.5f, 1.0, 0.6, 0.0, 1.0};
    arr[3] = {0.5f,  0.5f,  -0.5f, 1.0, 0.0, 4.0, 1.0};
    arr[4] = {-0.5f, 0.5f,  -0.5f, 1.0, 2.0, 0.0, 1.0};
    arr[5] = {-0.5f, -0.5f, -0.5f, 1.0, 0.0, 1.0, 1.0};

    arr[1] = {0.5f,  -0.5f, -0.5f, 1.0, 0.0, 0.5, 1.0};
    arr[2] = {0.5f,  0.5f,  -0.5f, 1.0, 0.6, 0.0, 1.0};
    arr[3] = {0.5f,  0.5f,  -0.5f, 1.0, 0.0, 4.0, 1.0};
    arr[4] = {-0.5f, 0.5f,  -0.5f, 1.0, 2.0, 0.0, 1.0};
    arr[5] = {-0.5f, -0.5f, -0.5f, 1.0, 0.0, 1.0, 1.0};
    arr[6] = {-0.5f, -0.5f, 0.5f,  0.9, 1.0, 0.5, 1.0};
    arr[7] = {0.5f,  -0.5f, 0.5f,  0.5, 1.0, 0.2, 1.0};
    arr[8] = {0.5f,  0.5f,  0.5f,  0.2, 1.0, 0.7, 1.0};
    arr[9] = {0.5f,  0.5f,  0.5f,  0.7, 1.0, 0.1, 1.0};
    arr[10] = {-0.5f, 0.5f,  0.5f,  0.3, 1.0, 0.5, 1.0};
    arr[11] = {-0.5f, -0.5f, 0.5f,  0.5, 1.0, 0.7, 1.0};
    arr[12] = {-0.5f, 0.5f,  0.5f,  0.7, 0.5, 1.0, 1.0};
    arr[13] = {-0.5f, 0.5f,  -0.5f, 0.0, 0.7, 1.0, 1.0};
    arr[14] = {-0.5f, -0.5f, -0.5f, 0.7, 0.1, 1.0, 1.0};
    arr[15] = {-0.5f, -0.5f, -0.5f, 0.2, 0.7, 1.0, 1.0};
    arr[16] = {-0.5f, -0.5f, 0.5f,  0.7, 0.0, 1.0, 1.0};
    arr[17] = {-0.5f, 0.5f,  0.5f,  0.3, 0.7, 1.0, 1.0};
    arr[18] = {0.5f,  0.5f,  0.5f,  0.2, 0.8, 1.0, 1.0};
    arr[19] = {0.5f,  0.5f,  -0.5f, 0.0, 0.2, 1.0, 1.0};
    arr[20] = {0.5f,  -0.5f, -0.5f, 0.2, 0.2, 1.0, 1.0};
    arr[21] = {0.5f,  -0.5f, -0.5f, 0.2, 0.5, 1.0, 1.0};
    arr[22] = {0.5f,  -0.5f, 0.5f,  0.2, 0.2, 1.0, 1.0};
    arr[23] = {0.5f,  0.5f,  0.5f,  0.2, 0.0, 1.0, 1.0};
    arr[24] = {-0.5f, -0.5f, -0.5f, 0.3, 1.0, 0.6, 1.0};
    arr[25] = {0.5f,  -0.5f, -0.5f, 0.8, 1.0, 0.3, 1.0};
    arr[26] = {0.5f,  -0.5f, 0.5f,  0.6, 1.0, 0.9, 1.0};
    arr[27] = {0.5f,  -0.5f, 0.5f,  0.3, 1.0, 0.5, 1.0};
    arr[28] = {-0.5f, -0.5f, 0.5f,  0.3, 1.0, 0.6, 1.0};
    arr[29] = {-0.5f, -0.5f, -0.5f, 0.7, 1.0, 0.3, 1.0};
    arr[30] = {-0.5f, 0.5f,  -0.5f, 1.0, 0.0, 0.6, 1.0};
    arr[31] = {0.5f,  0.5f,  -0.5f, 1.0, 0.6, 0.3, 1.0};
    arr[32] = {0.5f,  0.5f,  0.5f,  1.0, 0.0, 0.6, 1.0};
    arr[33] = {0.5f,  0.5f,  0.5f,  1.0, 0.6, 0.0, 1.0};
    arr[34] = {-0.5f, 0.5f,  0.5f,  1.0, 0.0, 0.6, 1.0};
    arr[35] = {-0.5f, 0.5f,  -0.5f, 1.0, 0.6, 0.0, 1.0};

    cubeVertex = arr;
    vertexSize = 36;
    LOGD("  drawVertex size is %d", vertexSize);
}
