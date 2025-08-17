package com.houtrry.lopengles20.tile

/**
 * 瓦片实体：包含纹理 id、像素尺寸、在整图中的原点（左上角像素坐标）以及是否就绪。
 */
data class Tile(
    val coord: TileCoord,
    val textureId: Int,
    val widthPx: Int,
    val heightPx: Int,
    val originXInMapPx: Int,
    val originYInMapPx: Int,
    val isReady: Boolean
)
