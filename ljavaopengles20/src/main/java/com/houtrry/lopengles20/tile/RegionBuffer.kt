package com.houtrry.lopengles20.tile

import java.nio.ByteBuffer

data class RegionBuffer(
    val width: Int,
    val height: Int,
    val glFormat: Int, // GLES20.GL_LUMINANCE or GLES20.GL_RGBA
    val pixelBuffer: ByteBuffer
)