//
// Created by huangdejun on 2024/7/7.
//

#include <GLES2/gl2.h>
#include "LGlBuffer.h"

LGlBuffer::LGlBuffer(LGlBuffer::BufferType type,
                     LGlBuffer::UsagePattern pattern) : bufferType(type), usagePattern(pattern),
                                                        bufferSize(0) {
    glGenBuffers(1, &bufferId);
}

LGlBuffer::~LGlBuffer() {
    glDeleteBuffers(1, &bufferId);
}

void LGlBuffer::bind() {
    glBindBuffer(bufferType, bufferId);
}

void LGlBuffer::unbind() {
    glBindBuffer(bufferType, GL_NONE);
}

void LGlBuffer::setBuffer(const GLvoid *data, GLsizeiptr size) {
    bufferSize = size;
    glBufferData(bufferType, size, data, usagePattern);
}

void LGlBuffer::updateBuffer(const GLvoid *data, GLsizeiptr offset, GLsizeiptr size) {
    glBufferSubData(bufferType, offset, size, data);
}
