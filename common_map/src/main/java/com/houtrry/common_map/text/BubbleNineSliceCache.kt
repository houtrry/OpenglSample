package com.houtrry.common_map.text

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils

/**
 * 气泡九宫格纹理缓存：主体（不含箭头）使用九宫格避免变形；箭头独立绘制。
 * 简化实现：以尺寸桶(8px 阶梯)缓存，key = 颜色+描边+阴影+corner+bucketSize。
 */
class BubbleNineSliceCache(private val context: Context) {

	data class NineSlice(val textureId: Int, val corner: Int, val slice: Rect)

	private val cache = mutableMapOf<String, NineSlice>()

	fun getOrCreate(
		bgColor: Int,
		borderColor: Int,
		borderWidthPx: Int,
		shadowColor: Int?,
		shadowRadiusPx: Int,
		cornerPx: Int,
		bucketW: Int,
		bucketH: Int
	): NineSlice {
		val key = "$bgColor-$borderColor-$borderWidthPx-${shadowColor ?: 0}-$shadowRadiusPx-$cornerPx-$bucketW-$bucketH"
		cache[key]?.let { return it }

		val bmp = Bitmap.createBitmap(bucketW, bucketH, Bitmap.Config.ARGB_8888)
		val c = Canvas(bmp)
		val paint = Paint(Paint.ANTI_ALIAS_FLAG)

		if (shadowColor != null && shadowRadiusPx > 0) {
			paint.color = shadowColor
			paint.maskFilter = BlurMaskFilter(shadowRadiusPx.toFloat(), BlurMaskFilter.Blur.NORMAL)
			val r = RectF(borderWidthPx.toFloat(), borderWidthPx.toFloat(), (bucketW - borderWidthPx).toFloat(), (bucketH - borderWidthPx).toFloat())
			c.drawRoundRect(r, cornerPx.toFloat(), cornerPx.toFloat(), paint)
			paint.maskFilter = null
		}

		paint.style = Paint.Style.FILL
		paint.color = bgColor
		val fillR = RectF(borderWidthPx.toFloat(), borderWidthPx.toFloat(), (bucketW - borderWidthPx).toFloat(), (bucketH - borderWidthPx).toFloat())
		c.drawRoundRect(fillR, cornerPx.toFloat(), cornerPx.toFloat(), paint)

		if (borderWidthPx > 0) {
			paint.style = Paint.Style.STROKE
			paint.strokeWidth = borderWidthPx.toFloat()
			paint.color = borderColor
			c.drawRoundRect(fillR, cornerPx.toFloat(), cornerPx.toFloat(), paint)
		}

		val texIds = IntArray(1)
		GLES20.glGenTextures(1, texIds, 0)
		val textureId = texIds[0]
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
		bmp.recycle()

		// 切片矩形（以 corner 为固定区域）
		val slice = Rect(cornerPx, cornerPx, bucketW - cornerPx, bucketH - cornerPx)
		return NineSlice(textureId, cornerPx, slice).also { cache[key] = it }
	}
}


