//
// Created by huangdejun on 2024/7/13.
//

#ifndef OPENGLSAMPLE_GLUTILS_H
#define OPENGLSAMPLE_GLUTILS_H

#include <GLES3/gl3.h>

class GlUtils {
public:
    static GLuint createProgram(const char *vertexShapeSource,
                                const char *fragmentShapeSource,
                                GLuint &vertexShape,
                                GLuint &fragmentShape);

private:
    enum OperationType {
        SHAPE,
        PROGRAM
    };

    static GLuint loadShape(GLenum shaderType, const char *pSource);

    static void checkGlErrorCode(const char *tag, const char *operation);

    static bool checkErrorInfoLog(OperationType operationType,
                                  GLuint targetId,
                                  const char *tag);
};


#endif //OPENGLSAMPLE_GLUTILS_H
