#include <string>
#include <GLES/gl.h>
#include <GLES2/gl2.h>
#include "lopengl.h"
#include "landroidlog.h"
#include "LOpenglPrimitivesDef.h"
#include "Triangle.h"
#include <glm/vec2.hpp>
#include <glm/vec3.hpp>
#include <glm/mat4x4.hpp>
#include <glm/glm.hpp>
#include <glm/ext/matrix_transform.hpp>
#include <glm/ext/matrix_clip_space.hpp>
#include <glm/gtc/type_ptr.hpp>

#ifdef __cplusplus

extern "C" {
#endif

    float m_angle = 0.0f;
void Java_com_houtrry_lopengl_view_MapView_ndkCreate(JNIEnv *env, jobject thiz) {
    LOGD("glCreate start");
    glClearColor(0.0, 0.0, 0.0, 1.0);
    glClearDepthf(1.0);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    LOGD("glCreate end");
}

void Java_com_houtrry_lopengl_view_MapView_ndkResize(JNIEnv *env, jobject thiz, jint width,
                                                     jint height) {
    LOGD("glResize start");
    glViewport(0, 0, width, height);
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glOrthof(-1, 1, -1, 1, 0.1, 1000.0);
    glLoadIdentity();
    LOGD("glResize end");
}

void Java_com_houtrry_lopengl_view_MapView_ndkDraw(JNIEnv *env, jobject thiz) {
    LOGD("glDraw start");
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glLoadIdentity();

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

    m_angle+=0.01f;

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), m_angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;

    glLoadMatrixf(glm::value_ptr(cubeMat));


    glDrawArrays(GL_TRIANGLES, 0, 36);


    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
    LOGD("glDraw end");


//    LFloat7 vertexTriangle[] = {
//            {-0.5, 0.1, -0.1, 1.0, 0.0, 0.0, 1.0},
//            {-0.5, 0.9, -0.1, 0.0, 1.0, 0.0, 1.0},
//            {0.5,  0.1, -0.1, 0.0, 0.0, 1.0, 1.0},
//            {0.5,  0.9, -0.1, 1.0, 0.0, 0.0, 1.0},
//    };
//    LOGD("vertex->>>, %d", vertexTriangle);
//    Triangle triangle(vertexTriangle, 4);
//    triangle.draw();
}

void drawRotateCube() {
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
    m_angle+=0.01f;

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), m_angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;

    glLoadMatrixf(glm::value_ptr(cubeMat));


    glDrawArrays(GL_TRIANGLES, 0, 36);


    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}

/**
 * 绘制立方体
 */
void drawCube() {
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

    glDrawArrays(GL_TRIANGLES, 0, 36);


    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}

/**
 * 绘制正方形
 * 绘制三角形
 */
void drawTriangle() {
    LFloat7 vertexTriangle[] = {
            {-0.5, 0.1, -0.1, 1.0, 0.0, 0.0, 1.0},
            {-0.5, 0.9, -0.1, 0.0, 1.0, 0.0, 1.0},
            {0.5,  0.1, -0.1, 0.0, 0.0, 1.0, 1.0},
            {0.5,  0.9, -0.1, 1.0, 0.0, 0.0, 1.0},
    };
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), vertexTriangle);
    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), &vertexTriangle[0].r);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);

    LFloat7 vertexTriangle2[] = {
            {-0.0, 0.0,  -0.1, 1.0, 0.0, 0.0, 1.0},
            {-0.5, -0.5, -0.1, 0.0, 1.0, 0.0, 1.0},
            {0.5,  -0.5, -0.1, 0.0, 0.0, 1.0, 1.0},
    };
    //向GPU提交顶点坐标和颜色数据
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat7), vertexTriangle2);
    glColorPointer(4, GL_FLOAT, sizeof(LFloat7), &vertexTriangle2[0].r);

    glDrawArrays(GL_TRIANGLES, 0, 3);

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}
#ifdef __cplusplus
}
#endif