package com.houtrry.lopengles20.tile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.opengl.GLES20
import android.util.Log
import com.houtrry.lopengles20.tile.StreamingGrayscaleRegionProvider.Companion
import java.nio.ByteBuffer

/**
 * JPEG/PNG 区域解码型 RegionProvider：
 * - 通过 BitmapRegionDecoder.decodeRegion(rect, opts) 按需解码 Tile 对应区域；
 * - 两种上传路径：
 *   1) RGBA 上传（glFormat=GL_RGBA）：CPU 低、实现简单；
 *   2) CPU 转灰度（glFormat=GL_LUMINANCE）：带宽更低（1B/px），以 CPU 换带宽；
 * - decoder 非线程安全，使用内部锁串行解码；
 * - decoder 通过传入的 factory 延迟创建并复用。
 */
class JpegPngRegionProvider(
    private val decoderFactory: () -> BitmapRegionDecoder,
    private val useCpuGray: Boolean = false
) : RegionProvider {

    companion object {
        private const val TAG = "JpegPngRegionProvider"
        // GL_BGRA_EXT 常量未在 Android GLES* 类中暴露，使用其实际值 0x80E1
        private const val GL_BGRA_EXT = 0x80E1
    }

    @Volatile
    private var decoder: BitmapRegionDecoder? = null
    private val decodeLock = Any()

    private fun requireDecoder(): BitmapRegionDecoder {
        val existing = decoder
        if (existing != null) return existing
        synchronized(decodeLock) {
            val again = decoder
            if (again != null) return again
            val created = decoderFactory.invoke()
            decoder = created
            return created
        }
    }

    override fun obtainRegion(x: Int, y: Int, width: Int, height: Int): RegionBuffer? {
        Log.d(TAG, "obtainRegion leftTop: ($x, $y), size: $width * $height, useCpuGray: $useCpuGray")
        if (width <= 0 || height <= 0) return null
        val dec = try { requireDecoder() } catch (_: Throwable) { return null }
        val imgW = dec.width
        val imgH = dec.height
        val rx = x.coerceIn(0, imgW)
        val ry = y.coerceIn(0, imgH)
        val rw = if (rx + width > imgW) (imgW - rx) else width
        val rh = if (ry + height > imgH) (imgH - ry) else height
        if (rw <= 0 || rh <= 0) return null

        val rect = Rect(rx, ry, rx + rw, ry + rh)
        Log.d(TAG, "obtainRegion rect: (${rect.left}, ${rect.top}), (${rect.right}, ${rect.bottom})")
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bmp: Bitmap = synchronized(decodeLock) {
            dec.decodeRegion(rect, opts)
        } ?: return null

        return try {
            if (!useCpuGray) {
                val format = if (supportsBGRA()) GL_BGRA_EXT else GLES20.GL_RGBA
                Log.d(TAG, "format: $format")
                return if (format == GL_BGRA_EXT) {
                    val buf = ByteBuffer.allocateDirect(rw * rh * 4)
                    bmp.copyPixelsToBuffer(buf)
                    buf.position(0)
                    RegionBuffer(rw, rh, format, buf)
                } else {
                    // 设备不支持 BGRA 扩展时，退回原先的通道重排（保持正确性）
                    val pixels = IntArray(rw * rh)
                    bmp.getPixels(pixels, 0, rw, 0, 0, rw, rh)
                    val reordered = ByteBuffer.allocateDirect(rw * rh * 4)
                    var i = 0
                    while (i < pixels.size) {
                        val c = pixels[i]
                        reordered.put(((c ushr 16) and 0xFF).toByte()) // R
                        reordered.put(((c ushr 8) and 0xFF).toByte())  // G
                        reordered.put((c and 0xFF).toByte())           // B
                        reordered.put(((c ushr 24) and 0xFF).toByte()) // A
                        i++
                    }
                    reordered.position(0)
                    RegionBuffer(rw, rh, GLES20.GL_RGBA, reordered)
                }
            } else {
                // CPU 转灰度（BT.601 近似）→ LUMINANCE 上传
                val pixels = IntArray(rw * rh)
                bmp.getPixels(pixels, 0, rw, 0, 0, rw, rh)
                val buf = ByteBuffer.allocateDirect(rw * rh)
                var i = 0
                while (i < pixels.size) {
                    val c = pixels[i]
                    val r = (c ushr 16) and 0xFF
                    val g = (c ushr 8) and 0xFF
                    val b = c and 0xFF
                    val y8 = ((299 * r + 587 * g + 114 * b) / 1000).coerceIn(0, 255)
                    buf.put(y8.toByte())
                    i++
                }
                buf.position(0)
                RegionBuffer(
                    width = rw,
                    height = rh,
                    glFormat = GLES20.GL_LUMINANCE,
                    pixelBuffer = buf
                )
            }
        } catch (_: Throwable) {
            null
        } finally {
            // 及时回收临时 Bitmap
            if (!bmp.isRecycled) bmp.recycle()
        }
    }
    
    /**
     * 释放Provider相关资源
     * 
     * 关闭BitmapRegionDecoder，释放内存资源
     */
    override fun close() {
        try {
            decoder?.recycle()
        } catch (e: Exception) {
            // BitmapRegionDecoder.recycle() 可能抛出异常，忽略
        }
    }

    private fun supportsBGRA(): Boolean {
        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: return false
        Log.d(TAG, "supportsBGRA extensions: $extensions")
        return extensions.contains("GL_EXT_texture_format_BGRA8888")
    }
}


