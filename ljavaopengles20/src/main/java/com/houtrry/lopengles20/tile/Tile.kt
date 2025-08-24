package com.houtrry.lopengles20.tile

/**
 * 瓦片实体类：表示一个已分配的地图瓦片，包含其纹理资源和空间位置信息
 * 
 * 生命周期：
 * 1. 创建占位瓦片（textureId=0, isReady=false）
 * 2. 异步加载纹理数据并上传到GPU
 * 3. 更新为就绪状态（textureId>0, isReady=true）
 * 4. 通过LRU缓存管理回收
 * 
 * @param coord             瓦片坐标，唯一标识该瓦片在网格中的位置
 * @param textureId         OpenGL纹理ID，0表示尚未分配纹理（占位状态）
 * @param widthPx           瓦片像素宽度，边缘瓦片可能小于标准尺寸
 * @param heightPx          瓦片像素高度，边缘瓦片可能小于标准尺寸  
 * @param originXInMapPx    瓦片左上角在整个地图中的X像素坐标
 * @param originYInMapPx    瓦片左上角在整个地图中的Y像素坐标
 * @param isReady           瓦片是否已就绪可用于渲染，false时渲染器应跳过绘制
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
