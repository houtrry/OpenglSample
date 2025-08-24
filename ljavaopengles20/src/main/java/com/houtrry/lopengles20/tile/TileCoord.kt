package com.houtrry.lopengles20.tile

/**
 * 瓦片坐标类：用于唯一标识一个瓦片在多级别金字塔中的位置
 * 
 * 坐标体系说明：
 * - 支持负索引的无限网格系统，以适应地图动态扩展需求
 * - 左下角为原点，X轴向右为正，Y轴向上为正
 * - 使用floor除法规则保证负数索引的正确性
 * 
 * @param level 金字塔层级，0为最高分辨率（原始层），正数表示逐级降采样
 *              当前版本固定为0，后续可扩展支持多级LOD
 * @param x     瓦片在网格中的X坐标（列索引），可以为负数以支持地图向左扩展
 * @param y     瓦片在网格中的Y坐标（行索引），可以为负数以支持地图向下扩展
 */
data class TileCoord(val level: Int, val x: Int, val y: Int)
