package com.houtrry.lopengles20.tile

/**
 * 瓦片坐标：level 表示金字塔层级（此版本固定为 0，后续可扩展），x/y 为网格索引。
 */
data class TileCoord(val level: Int, val x: Int, val y: Int)
