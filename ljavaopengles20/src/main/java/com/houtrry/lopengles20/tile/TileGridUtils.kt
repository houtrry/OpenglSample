package com.houtrry.lopengles20.tile

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Tile 网格与动态区域工具函数：
 * - 支持负索引（原点锚定的无限网格）
 * - 统一 floor 规则
 * - AABB → 覆盖的 Tile 索引区间
 * - 计算边缘 Tile 的局部子矩形与对应 UV
 */
object TileGridUtils {

	data class PixelAabb(
		val minX: Int,
		val minY: Int,
		val maxX: Int,
		val maxY: Int
	) {
		init {
			require(maxX > minX && maxY > minY) { "Invalid AABB: ($minX,$minY)-($maxX,$maxY)" }
		}
	}

	data class TileIndex(val x: Int, val y: Int)

	data class UvRect(val u0: Float, val v0: Float, val u1: Float, val v1: Float)

	data class TileSubRequest(
		val tileIndex: TileIndex,
		val tileOriginX: Int,
		val tileOriginY: Int,
		val subMinX: Int,
		val subMinY: Int,
		val subWidth: Int,
		val subHeight: Int,
		val xOffsetInTile: Int,
		val yOffsetInTile: Int,
		val uv: UvRect
	)

	fun floorDivExact(a: Int, b: Int): Int {
		require(b != 0) { "Divisor must not be 0" }
		// 数学意义的 floor(a/b)，适配负数
		val q = a / b
		val r = a % b
		return if (r == 0 || (a xor b) >= 0) q else q - 1
	}

	fun pixelToTileIndex(x: Int, y: Int, tileSize: Int): TileIndex {
		val tx = floorDivExact(x, tileSize)
		val ty = floorDivExact(y, tileSize)
		return TileIndex(tx, ty)
	}

	/**
     * 计算像素 AABB 覆盖到的 Tile 索引闭区间 [txMin..txMax], [tyMin..tyMax]
     * 使用 (max-1)/floor 避免边界多算一列/行
     */
	fun computeTileRangeForAabb(aabb: PixelAabb, tileSize: Int): Pair<IntRange, IntRange> {
		val txMin = floorDivExact(aabb.minX, tileSize)
		val txMax = floorDivExact(aabb.maxX - 1, tileSize)
		val tyMin = floorDivExact(aabb.minY, tileSize)
		val tyMax = floorDivExact(aabb.maxY - 1, tileSize)
		return (txMin..txMax) to (tyMin..tyMax)
	}

	/**
     * 计算指定 Tile 与像素 AABB 的相交子矩形及其在 Tile 内部的偏移与 UV。
     * 若无相交返回 null。
     */
	fun computeTileSubRequestForAabb(
		tileIndex: TileIndex,
		aabb: PixelAabb,
		tileSize: Int
	): TileSubRequest? {
		val tileOriginX = tileIndex.x * tileSize
		val tileOriginY = tileIndex.y * tileSize
		val tileMaxX = tileOriginX + tileSize
		val tileMaxY = tileOriginY + tileSize

		val subMinX = max(tileOriginX, aabb.minX)
		val subMinY = max(tileOriginY, aabb.minY)
		val subMaxX = min(tileMaxX, aabb.maxX)
		val subMaxY = min(tileMaxY, aabb.maxY)
		if (subMinX >= subMaxX || subMinY >= subMaxY) return null

		val subWidth = subMaxX - subMinX
		val subHeight = subMaxY - subMinY
		val xOffsetInTile = subMinX - tileOriginX
		val yOffsetInTile = subMinY - tileOriginY

		val u0 = (xOffsetInTile.toFloat()) / tileSize.toFloat()
		val v0 = (yOffsetInTile.toFloat()) / tileSize.toFloat()
		val u1 = (xOffsetInTile + subWidth).toFloat() / tileSize.toFloat()
		val v1 = (yOffsetInTile + subHeight).toFloat() / tileSize.toFloat()

		return TileSubRequest(
			tileIndex = tileIndex,
			tileOriginX = tileOriginX,
			tileOriginY = tileOriginY,
			subMinX = subMinX,
			subMinY = subMinY,
			subWidth = subWidth,
			subHeight = subHeight,
			xOffsetInTile = xOffsetInTile,
			yOffsetInTile = yOffsetInTile,
			uv = UvRect(u0, v0, u1, v1)
		)
	}

	/**
     * 将动态 AABB 转换为一组 TileSubRequest（可用于 glTexSubImage2D 的局部上传）。
     */
	fun buildSubRequestsForAabb(
		aabb: PixelAabb,
		tileSize: Int
	): List<TileSubRequest> {
		val result = ArrayList<TileSubRequest>()
		val (rx, ry) = computeTileRangeForAabb(aabb, tileSize)
		for (ty in ry) {
			for (tx in rx) {
				val req = computeTileSubRequestForAabb(TileIndex(tx, ty), aabb, tileSize)
				if (req != null) result.add(req)
			}
		}
		return result
	}
}


