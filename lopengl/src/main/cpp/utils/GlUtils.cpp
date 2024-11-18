//
// Created by huangdejun on 2024/7/13.
//

#include "GlUtils.h"
#include <stdlib.h>
#include "../landroidlog.h"

GLuint GlUtils::loadShape(GLenum shaderType, const char *pSource) {
    LOGTD("loadShape", "glCreateShader called, shaderType: %d, and compile shape source is \n%s ", shaderType, pSource);
    GLuint shapeId;
    shapeId = glCreateShader(shaderType);
    if (!shapeId) {
        LOGTE("loadShape", "glCreateShader failure for type: %d", shaderType);
        return -1;
    }
    glShaderSource(shapeId, 1, &pSource, NULL);
    glCompileShader(shapeId);
    //检查编译状态
    GLint compileStatus = 0;
    glGetShaderiv(shapeId, GL_COMPILE_STATUS, &compileStatus);
    if (compileStatus) {
        //如果编译成功，直接返回
        LOGTD("loadShape", "glCreateShader successfully for type: %d, and then shape is %d ", shaderType, shapeId);
        return shapeId;
    }
    //如果编译失败，检查编译失败的原因
    bool printErrorInfo = checkErrorInfoLog(OperationType::SHAPE, shapeId, "loadShape");
    if (!printErrorInfo) {
        glDeleteShader(shapeId);
        LOGTE("loadShape", "glCreateShader failure for type: %d, and don`t get error info", shaderType);
        return -2;
    }
    glDeleteShader(shapeId);
    return -3;
}

GLuint GlUtils::createProgram(const char *vertexShapeSource, const char *fragmentShapeSource,
                              GLuint &vertexShape, GLuint &fragmentShape) {
    LOGTD("createProgram", "createProgram called, vertexShape: %d, fragmentShape %d ", vertexShape, fragmentShape);
    vertexShape = loadShape(GL_VERTEX_SHADER, vertexShapeSource);
    if (vertexShape <= 0) {
        return -1;
    }
    fragmentShape = loadShape(GL_FRAGMENT_SHADER, fragmentShapeSource);
    if (fragmentShape <= 0) {
        return -1;
    }
    GLuint program = 0;
    program = glCreateProgram();
    if (!program) {
        return -2;
    }
    glAttachShader(program, vertexShape);
    checkGlErrorCode("createProgram", "glAttachShader vertex shape");
    glAttachShader(program, fragmentShape);
    checkGlErrorCode("createProgram", "glAttachShader fragment shape");
    glLinkProgram(program);
    GLint linkStatus = GL_FALSE;
    glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);

    glDetachShader(program, vertexShape);
    glDeleteShader(vertexShape);
    vertexShape = 0;

    glDetachShader(program, fragmentShape);
    glDeleteShader(fragmentShape);
    fragmentShape = 0;

    if (linkStatus == GL_TRUE) {
        //link成功，返回结果
        return program;
    }
    //link失败，查询失败原因，并删除program
    bool printErrorInfo = checkErrorInfoLog(OperationType::PROGRAM, program, "createProgram");
    glDeleteProgram(program);
    if (!printErrorInfo) {
        LOGTE("createProgram", "glLinkProgram failure, and don`t get error info");
        return -2;
    }
    return -3;
}

void GlUtils::checkGlErrorCode(const char * tag, const char *operation) {
    for (GLenum error = glGetError(); error; error = glGetError()) {
        LOGTE(tag, "gl error for %s, error is 0x%x", operation, error);
    }
}

bool GlUtils::checkErrorInfoLog(GlUtils::OperationType operationType, GLuint targetId, const char *tag) {
    GLint infoLength;
    switch (operationType) {
        case SHAPE: {
            glGetShaderiv(targetId, GL_INFO_LOG_LENGTH, &infoLength);
        }
        case PROGRAM :{
            glGetProgramiv(targetId, GL_INFO_LOG_LENGTH, &infoLength);
        }
        default:{

        }
    };
    if (!infoLength) {
        return false;
    }
    char *buf = (char *) malloc(infoLength);
    if (!buf) {
        return false;
    }
    switch (operationType) {
        case SHAPE: {
            glGetShaderInfoLog(targetId, infoLength, NULL, buf);
        }
        case PROGRAM :{
            glGetProgramInfoLog(targetId, infoLength, NULL, buf);
        }
        default:{

        }
    };
    LOGTE(tag, "load %d error reason is %s", operationType, buf);
    free(buf);
    return true;
}

