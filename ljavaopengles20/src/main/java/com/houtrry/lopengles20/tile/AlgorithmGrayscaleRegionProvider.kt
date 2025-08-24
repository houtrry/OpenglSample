package com.houtrry.lopengles20.tile

import android.opengl.GLES20
import java.nio.ByteBuffer

/**
 * 算法灰度 RegionProvider 模板：
 * - 直接从算法提供的整幅灰度 ByteBuffer（Y8，行优先）读取数据；
 * - 支持自定义行跨度 rowStrideBytes（存在行填充时使用真实 stride）；
 * - 线程安全交由调用方保证（建议提供快照或在拷贝期间加锁）。
 */
class AlgorithmGrayscaleRegionProvider(
    private val mapWidthPx: Int,
    private val mapHeightPx: Int,
    private val rowStrideBytes: Int = mapWidthPx,
    private val bufferSupplier: () -> ByteBuffer
) : RegionProvider {

    override fun obtainRegion(x: Int, y: Int, width: Int, height: Int): RegionBuffer? {
        if (x < 0 || y < 0 || width <= 0 || height <= 0) return null
        if (x + width > mapWidthPx || y + height > mapHeightPx) return null
        val src = try {
            bufferSupplier.invoke()
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
        val required = rowStrideBytes * mapHeightPx
        if (src.capacity() < required) return null

        val dst = ByteBuffer.allocateDirect(width * height)
        val srcDup = src.duplicate()
        val rowBuf = ByteArray(width)
        var row = 0
        while (row < height) {
            val base = (y + row) * rowStrideBytes + x
            if (base < 0 || base + width > srcDup.capacity()) return null
            srcDup.position(base)
            srcDup.get(rowBuf, 0, width)
            dst.put(rowBuf)
            row++
        }
        dst.position(0)
        return RegionBuffer(
            width = width,
            height = height,
            glFormat = GLES20.GL_LUMINANCE,
            pixelBuffer = dst
        )
    }
    
    /**
     * 释放Provider相关资源
     * 
     * AlgorithmGrayscaleRegionProvider 不持有需要显式释放的资源，
     * bufferSupplier 由外部管理，因此提供空实现
     */
    override fun close() {
        // 无需释放资源
    }
}


