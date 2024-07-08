//
// Created by huangdejun on 2024/7/7.
//

#ifndef OPENGLSAMPLE_LGLBUFFER_H
#define OPENGLSAMPLE_LGLBUFFER_H

#include <GLES/gl.h>

class LGlBuffer {

public:
    enum BufferType {
        VertexBuffer = 0x8892, //VBO
        IndexBuffer = 0x8893, //EBO
        PixelPackBuffer = 0x88EB, //PBO
        PixelUnpackBuffer = 0x88EC, //PBO
    };
    enum UsagePattern {
        StreamDraw = 0x88E0,
        StreamRead = 0x88E1,
        StreamCopy = 0x88E2,
        StaticDraw = 0x88E4,
        StaticRead = 0x88E5,
        StaticCopy = 0x88E6,
        DynamicDraw = 0x88E8,
        DynamicRead = 0x88E9,
        DynamicCopy = 0x88EA,
    };

    LGlBuffer(BufferType type, UsagePattern pattern);

    ~LGlBuffer();

    void bind();

    void unbind();

    void setBuffer(const GLvoid *data, GLsizeiptr size);

    void updateBuffer(const GLvoid *data, GLsizeiptr offset, GLsizeiptr size);

private:
    BufferType bufferType;
    UsagePattern usagePattern;
    GLuint bufferId;
    GLsizeiptr bufferSize;
};


#endif //OPENGLSAMPLE_LGLBUFFER_H
