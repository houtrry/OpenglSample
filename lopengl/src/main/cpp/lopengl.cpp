#include <string>
#include <GLES/gl.h>
#include <GLES2/gl2.h>
#include "lopengl.h"
#include "landroidlog.h"
#include "LOpenglPrimitivesDef.h"
#include "Triangle.h"
#include "utils.h"
#include <glm/vec2.hpp>
#include <glm/vec3.hpp>
#include <glm/mat4x4.hpp>
#include "opengl_utils.h"

#ifdef __cplusplus

extern "C" {
#endif
float m_angle = 0.0f;
GLuint mTextureIds[6];

/**
 * 绘制旋转的纹理图片
 */
void drawRotateTextureCube() {
    glCullFace(GL_BACK);
    glFrontFace(GL_CW);
    LFloat5 cubeVertex[] = {
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},

            {-0.5f, -0.5f, 0.5f,  0.0, 0.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 0.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 0.0},

            {-0.5f, 0.5f,  0.5f,  0.0, 0.0},
            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 0.0},

            {0.5f,  0.5f,  0.5f,  0.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 0.0},

            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},

            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0}
    };
    LOGD("draw start");
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat5), cubeVertex);
    glTexCoordPointer(2, GL_FLOAT, sizeof(LFloat5), &cubeVertex[0].u);

    //注意这里，m_angle要想不停变化
    //需要不停的刷新
    //opengl的渲染模式，就不能是RENDERMODE_WHEN_DIRTY
    m_angle += 0.01f;

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), m_angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;

    glLoadMatrixf(glm::value_ptr(cubeMat));


    for (int i = 0; i < 6; ++i) {
        LOGD("draw %d -> %d", i, mTextureIds[i]);
        glBindTexture(GL_TEXTURE_2D, mTextureIds[i]);
        glDrawArrays(GL_TRIANGLES, 6 * i, 6);
    }

    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
}

GLuint mTextureId = 0;
/**
 * 绘制纹理图片
 */
void drawTexture() {
    glCullFace(GL_BACK);
    LFloat5 cubeVertex[] = {
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 1.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},

            {-0.5f, -0.5f, 0.5f,  0.0, 0.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 0.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 0.0},

            {-0.5f, 0.5f,  0.5f,  0.0, 0.0},
            {-0.5f, 0.5f,  -0.5f, 1.0, 0.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 0.0},

            {0.5f,  0.5f,  0.5f,  0.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 1.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  0.0, 1.0},
            {0.5f,  0.5f,  0.5f,  0.0, 0.0},

            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},
            {0.5f,  -0.5f, -0.5f, 1.0, 0.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 1.0},
            {0.5f,  -0.5f, 0.5f,  1.0, 1.0},
            {-0.5f, -0.5f, 0.5f,  0.0, 1.0},
            {-0.5f, -0.5f, -0.5f, 0.0, 0.0},

            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0},
            {0.5f,  0.5f,  -0.5f, 1.0, 0.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {0.5f,  0.5f,  0.5f,  1.0, 1.0},
            {-0.5f, 0.5f,  0.5f,  0.0, 1.0},
            {-0.5f, 0.5f,  -0.5f, 0.0, 0.0}
    };
    glBindTexture(GL_TEXTURE_2D, mTextureId);
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);

    glVertexPointer(3, GL_FLOAT, sizeof(LFloat5), cubeVertex);
    glTexCoordPointer(2, GL_FLOAT, sizeof(LFloat5), &cubeVertex[0].u);

    //注意这里，m_angle要想不停变化
    //需要不停的刷新
    //opengl的渲染模式，就不能是RENDERMODE_WHEN_DIRTY
    m_angle += 0.01f;

    glm::mat4x4 cubeMat;
    glm::mat4x4 cubeTransMat = glm::translate(glm::mat4(1.0f), glm::vec3(0.0f, 0.0f, -0.5));
    glm::mat4x4 cubeRotateMat = glm::rotate(glm::mat4(1.0f), m_angle, glm::vec3(0.5f, 0.5f, 1.0));
    glm::mat4x4 cubeScaleMat = glm::scale(glm::mat4(1.0f), glm::vec3(0.5f, 0.4f, 0.5));
    cubeMat = cubeTransMat * cubeRotateMat * cubeScaleMat;

    glLoadMatrixf(glm::value_ptr(cubeMat));


    glDrawArrays(GL_TRIANGLES, 0, 36);


    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
}
/**
 * 绘制旋转的立方体
 */
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
    m_angle += 0.01f;

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

    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
//    drawTriangle();
//    drawCube();
//    drawRotateCube();
//    drawTexture();
    drawRotateTextureCube();
}

jint Java_com_houtrry_lopengl_view_MapView_ndkReadAssertManager(JNIEnv *env, jobject thiz,
                                                                jobject asset_manager,
                                                                jstring name) {
    AAssetManager *mAssetManager = AAssetManager_fromJava(env, asset_manager);
    const char *fileName = env->GetStringUTFChars(name, 0);
    mTextureId = readSourceFromAssertManager(mAssetManager, fileName);
    env->ReleaseStringUTFChars(name, fileName);
    return 0;
}

jboolean Java_com_houtrry_lopengl_view_MapView_ndkReadAssertManagers(JNIEnv *env, jobject thiz,
                                                                     jobject asset_manager,
                                                                     jobjectArray names) {

    AAssetManager *mAssetManager = AAssetManager_fromJava(env, asset_manager);
    if (NULL == mAssetManager) {
        LOGF("assetManager is NULL");
        return false;
    }
//    int size = env->GetArrayLength(names);
    long startTimestamp = getTimestamp();
    for (int i = 0; i < 6; i++) {
        jobject jobject = env->GetObjectArrayElement(names, i);
        jstring jstr = static_cast<jstring>(jobject);
        const char *fileName = env->GetStringUTFChars(jstr, 0);
        mTextureIds[i] = readSourceFromAssertManager(mAssetManager, fileName);
        env->ReleaseStringUTFChars(jstr, fileName);
    }
    LOGD("ndkReadAssertManagers end, and cost time is %ld", getTimestamp() - startTimestamp);
    return true;
}

#ifdef __cplusplus
}
#endif
