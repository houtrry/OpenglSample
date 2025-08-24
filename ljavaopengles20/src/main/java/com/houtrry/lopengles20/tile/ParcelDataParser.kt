package com.houtrry.lopengles20.tile

import android.os.ParcelFileDescriptor

/**
 * ParcelFileDescriptor数据解析器接口
 * 
 * 定义从ParcelFileDescriptor解析地图数据的标准接口
 */
interface ParcelDataParser {
    /**
     * 解析ParcelFileDescriptor获取地图数据
     * 
     * @param parcelFileDescriptor 地图数据文件描述符
     * @return 解析后的地图数据
     * @throws IllegalArgumentException 如果数据格式无效
     */
    fun parseMapData(parcelFileDescriptor: ParcelFileDescriptor): ParcelMapData
}

/**
 * ParcelFileDescriptor解析后的地图数据
 * 
 * @param originX 纹理左下角世界坐标X（米）
 * @param originY 纹理左下角世界坐标Y（米）
 * @param resolution 像素分辨率（米/像素）
 * @param width 纹理宽度（像素）
 * @param height 纹理高度（像素）
 * @param area 地图面积（算法概念）
 * @param pixelData 纹理像素数据（灰度图或JPEG）
 */
data class ParcelMapData(
    val originX: Double,
    val originY: Double,
    val resolution: Double,
    val width: Int,
    val height: Int,
    val area: Int,
    val pixelData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParcelMapData

        if (originX != other.originX) return false
        if (originY != other.originY) return false
        if (resolution != other.resolution) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (area != other.area) return false
        if (!pixelData.contentEquals(other.pixelData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = originX.hashCode()
        result = 31 * result + originY.hashCode()
        result = 31 * result + resolution.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + area
        result = 31 * result + pixelData.contentHashCode()
        return result
    }
}
