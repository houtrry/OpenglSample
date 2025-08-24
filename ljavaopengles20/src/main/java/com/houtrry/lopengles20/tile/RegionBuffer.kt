package com.houtrry.lopengles20.tile

import java.nio.ByteBuffer

/**
 * 区域像素数据缓冲区：封装待上传到GPU的像素数据及其格式信息
 * 
 * 用途：
 * - 作为RegionProvider的返回值，统一封装像素数据
 * - 提供GL上传所需的格式和尺寸信息
 * - 支持多种像素格式以平衡性能和带宽
 * 
 * 注意事项：
 * - pixelBuffer必须为direct buffer，且position()=0
 * - buffer大小应匹配 width * height * bytesPerPixel
 * - 调用方负责buffer的生命周期管理
 * 
 * @param width       像素宽度
 * @param height      像素高度  
 * @param glFormat    OpenGL像素格式：
 *                    - GLES20.GL_LUMINANCE (0x1909)：灰度格式，1字节/像素，节省带宽
 *                    - GLES20.GL_RGBA (0x1908)：RGBA格式，4字节/像素，兼容性好
 *                    - 0x8229 (GL_RED)：红色通道格式，1字节/像素，ES3.0支持
 * @param pixelBuffer 像素数据缓冲区，必须为direct ByteBuffer，position=0
 */
data class RegionBuffer(
    val width: Int,
    val height: Int,
    val glFormat: Int, // GLES20.GL_LUMINANCE or GLES20.GL_RGBA
    val pixelBuffer: ByteBuffer
)