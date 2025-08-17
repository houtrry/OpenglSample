package com.houtrry.lopengles20.tile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.opengl.GLES20
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
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bmp: Bitmap = synchronized(decodeLock) {
            dec.decodeRegion(rect, opts)
        } ?: return null

        return try {
            if (!useCpuGray) {
                // RGBA 路线：将 ARGB_8888 像素转为 RGBA 顺序的字节缓冲
                val pixels = IntArray(rw * rh)
                bmp.getPixels(pixels, 0, rw, 0, 0, rw, rh)
                val buf = ByteBuffer.allocateDirect(rw * rh * 4)
                var i = 0
                while (i < pixels.size) {
                    val c = pixels[i]
                    val a = (c ushr 24) and 0xFF
                    val r = (c ushr 16) and 0xFF
                    val g = (c ushr 8) and 0xFF
                    val b = c and 0xFF
                    buf.put(r.toByte())
                    buf.put(g.toByte())
                    buf.put(b.toByte())
                    buf.put(a.toByte())
                    i++
                }
                buf.position(0)
                RegionBuffer(
                    width = rw,
                    height = rh,
                    glFormat = GLES20.GL_RGBA,
                    pixelBuffer = buf
                )
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
}


