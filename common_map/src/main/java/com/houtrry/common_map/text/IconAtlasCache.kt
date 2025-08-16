package com.houtrry.common_map.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils

/**
 * 简化版图标图集缓存：将多张小图标打包到单张纹理中，返回每个图标的UV。
 * 为最小改动，这里使用等格子排布的简化实现。
 */
class IconAtlasCache(private val context: Context) {

	data class IconEntry(val u1: Float, val v1: Float, val u2: Float, val v2: Float)

	data class IconEntryWithTex(val textureId: Int, val u1: Float, val v1: Float, val u2: Float, val v2: Float)

	private var atlasTextureId: Int = 0
	private var atlasWidth = 0
	private var atlasHeight = 0

	// 多图集支持
	private val atlasTextureIds = mutableListOf<Int>()
	private val atlasSizes = mutableListOf<Pair<Int, Int>>()
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	private val resIdToEntry = mutableMapOf<Int, IconEntry>()
	private val resIdToEntryWithTex = mutableMapOf<Int, IconEntryWithTex>()

	fun ensureAtlas(icons: List<Pair<Bitmap, Int>>, cellSize: Int = 64, cols: Int = 16) {
		if (icons.isEmpty()) return
		if (atlasTextureId != 0 || atlasTextureIds.isNotEmpty()) return

		// 查询设备最大纹理尺寸，失败兜底 2048
		val maxSizeArr = IntArray(1)
		kotlin.runCatching { GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSizeArr, 0) }
		val maxTextureSize = (maxSizeArr.getOrNull(0) ?: 0).let { if (it <= 0) 2048 else it }

		val maxCols = cols.coerceAtMost(maxTextureSize / cellSize)
		val maxRows = (maxTextureSize / cellSize).coerceAtLeast(1)
		val capacityPerAtlas = (maxCols.coerceAtLeast(1)) * maxRows

		var indexGlobal = 0
		var atlasIndex = 0
		while (indexGlobal < icons.size) {
			val endExclusive = (indexGlobal + capacityPerAtlas).coerceAtMost(icons.size)
			val chunk = icons.subList(indexGlobal, endExclusive)

			val rows = ((chunk.size + maxCols - 1) / maxCols).coerceAtLeast(1)
			val atlasW = maxCols * cellSize
			val atlasH = rows * cellSize
			val atlas = Bitmap.createBitmap(atlasW, atlasH, Bitmap.Config.ARGB_8888)
			val canvas = Canvas(atlas)

			chunk.forEachIndexed { localIdx, (bmp, resId) ->
				val row = localIdx / maxCols
				val col = localIdx % maxCols
				val left = col * cellSize
				val top = row * cellSize
				canvas.drawBitmap(bmp, left.toFloat(), top.toFloat(), paint)
				val u1 = left.toFloat() / atlasW
				val v1 = top.toFloat() / atlasH
				val u2 = (left + bmp.width).toFloat() / atlasW
				val v2 = (top + bmp.height).toFloat() / atlasH
				resIdToEntry[resId] = IconEntry(u1, v1, u2, v2)
				// 纹理ID稍后生成后再补充 textureId 映射
			}

			val texIds = IntArray(1)
			GLES20.glGenTextures(1, texIds, 0)
			val textureId = texIds[0]
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, atlas, 0)
			atlas.recycle()

			// 记录 atlas 列表
			atlasTextureIds.add(textureId)
			atlasSizes.add(Pair(atlasW, atlasH))
			if (atlasIndex == 0) {
				// 保持旧API兼容，记录第一个图集为 "atlasTextureId"
				atlasTextureId = textureId
				atlasWidth = atlasW
				atlasHeight = atlasH
			}

			// 为该atlas内的资源补充 textureId -> UV 的映射
			chunk.forEachIndexed { localIdx, (_, resId) ->
				val entry = resIdToEntry[resId]
				if (entry != null) {
					resIdToEntryWithTex[resId] = IconEntryWithTex(textureId, entry.u1, entry.v1, entry.u2, entry.v2)
				}
			}

			indexGlobal = endExclusive
			atlasIndex++
		}
	}

	fun getTextureId(): Int = atlasTextureId

	fun getUvFor(resId: Int): IconEntry? = resIdToEntry[resId]

	fun getEntryWithTex(resId: Int): IconEntryWithTex? = resIdToEntryWithTex[resId]
}


