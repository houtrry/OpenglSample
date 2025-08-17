package com.houtrry.lopengles20.tile

import android.graphics.Bitmap
import android.opengl.GLES20
import java.nio.ByteBuffer

/**
 * 示例 RegionProvider：
 * - 从 Bitmap 源提取局部区域，转换为单通道灰度（Y8）ByteBuffer；
 * - 上传时使用 GL_LUMINANCE，较 RGBA 可降低 4 倍带宽；
 * - 灰度计算使用 BT.601 近似（0.299R + 0.587G + 0.114B）。
 */
class GrayscaleRegionProviderFromBitmap(private val source: Bitmap) : TileManager.RegionProvider {

    override fun obtainRegion(x: Int, y: Int, width: Int, height: Int): TileManager.RegionBuffer? {
        return try {
            val pixels = IntArray(width * height)
            source.getPixels(pixels, 0, width, x, y, width, height)
            val buffer = ByteBuffer.allocateDirect(width * height)
            var i = 0
            while (i < pixels.size) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                // ITU-R BT.601 luma approximation
                val y8 = ((299 * r + 587 * g + 114 * b) / 1000).coerceIn(0, 255)
                buffer.put(y8.toByte())
                i++
            }
            buffer.position(0)
            TileManager.RegionBuffer(
                width = width,
                height = height,
                glFormat = GLES20.GL_LUMINANCE,
                pixelBuffer = buffer
            )
        } catch (e: Throwable) {
            null
        }
    }
}


