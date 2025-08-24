package com.houtrry.lopengles20.tile

import android.opengl.GLES20
import java.nio.ByteBuffer

/**
 * 基于ParcelFileDescriptor解析数据的灰度RegionProvider
 * 
 * 特性：
 * - 动态尺寸：地图宽高从ParcelMapData动态获取，无需构造时预知
 * - 元数据访问：提供世界坐标原点、分辨率等信息，供坐标换算使用
 * - 线程安全：内部同步保护，可在多线程环境安全使用
 * - 内存优化：按需分配区域缓冲，避免不必要的内存占用
 * 
 * 与AlgorithmGrayscaleRegionProvider的区别：
 * - ✅ 支持动态地图尺寸（从PFD解析）
 * - ✅ 提供完整的地图元数据访问
 * - ✅ 处理36字节头部格式（非32字节）
 * - ✅ 更好的错误处理和边界检查
 * 
 * 用法：
 * ```kotlin
 * val parser = ParcelMapDataParser()
 * val mapData = parser.parseMapData(parcelFileDescriptor)
 * val regionProvider = ParcelGrayscaleRegionProvider(mapData)
 * 
 * // 获取区域数据
 * val region = regionProvider.obtainRegion(x=100, y=100, width=256, height=256)
 * 
 * // 访问元数据
 * val worldX = regionProvider.worldOriginX
 * val resolution = regionProvider.resolution
 * ```
 */
class ParcelGrayscaleRegionProvider(
    private val parcelData: ParcelMapData,
    private val isFromObjectPool: Boolean = false,
    private val objectPool: ByteArrayPool? = null
) : RegionProvider {

    companion object {
        private const val TAG = "ParcelRegionProvider"
    }
    
    private val pixelBuffer: ByteBuffer = ByteBuffer.wrap(parcelData.pixelData)
    private val mapWidth = parcelData.width
    private val mapHeight = parcelData.height
    
    // 地图元数据访问器（供坐标换算使用）
    val worldOriginX: Double get() = parcelData.originX
    val worldOriginY: Double get() = parcelData.originY
    val resolution: Double get() = parcelData.resolution
    val mapWidthPx: Int get() = mapWidth
    val mapHeightPx: Int get() = mapHeight
    val mapArea: Int get() = parcelData.area
    
    /**
     * 获取指定矩形区域的灰度像素数据
     * 
     * @param x      区域左上角X坐标（地图像素坐标系，左上角为原点）
     * @param y      区域左上角Y坐标（地图像素坐标系，左上角为原点）
     * @param width  区域宽度（像素）
     * @param height 区域高度（像素）
     * @return 区域像素数据缓冲区，失败时返回null
     */
    override fun obtainRegion(x: Int, y: Int, width: Int, height: Int): RegionBuffer? {
        // 参数有效性检查
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            return null
        }
        
        // 边界检查：确保请求区域在地图范围内
        if (x + width > mapWidth || y + height > mapHeight) {
            return null
        }
        
        try {
            // 分配区域数据缓冲区（direct buffer，便于GL上传）
            val regionBuffer = ByteBuffer.allocateDirect(width * height)
            
            // 同步保护：避免并发访问pixelBuffer时的数据竞争
            synchronized(pixelBuffer) {
                val tempRowBuffer = ByteArray(width)
                
                // 按行复制数据（行优先存储）
                for (row in 0 until height) {
                    val srcOffset = (y + row) * mapWidth + x
                    
                    // 检查源偏移是否超出范围
                    if (srcOffset < 0 || srcOffset + width > pixelBuffer.capacity()) {
                        return null
                    }
                    
                    // 复制一行数据
                    pixelBuffer.position(srcOffset)
                    pixelBuffer.get(tempRowBuffer, 0, width)
                    regionBuffer.put(tempRowBuffer)
                }
            }
            
            // 重置buffer位置，便于GL使用
            regionBuffer.position(0)
            
            return RegionBuffer(
                width = width,
                height = height,
                glFormat = GLES20.GL_LUMINANCE, // 灰度格式，1字节/像素
                pixelBuffer = regionBuffer
            )
            
        } catch (e: Exception) {
            // 内存分配或数据读取失败
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 世界坐标转地图像素坐标
     * 
     * @param worldX 世界坐标X（米）
     * @param worldY 世界坐标Y（米）
     * @return 地图像素坐标 Pair(pixelX, pixelY)，超出范围时返回null
     */
    fun worldToMapPixel(worldX: Double, worldY: Double): Pair<Int, Int>? {
        val pixelX = ((worldX - worldOriginX) / resolution).toInt()
        val pixelY = ((worldY - worldOriginY) / resolution).toInt()
        
        // 检查是否在地图范围内
        return if (pixelX >= 0 && pixelX < mapWidth && pixelY >= 0 && pixelY < mapHeight) {
            Pair(pixelX, pixelY)
        } else {
            null
        }
    }
    
    /**
     * 地图像素坐标转世界坐标
     * 
     * @param pixelX 地图像素坐标X
     * @param pixelY 地图像素坐标Y
     * @return 世界坐标 Pair(worldX, worldY)
     */
    fun mapPixelToWorld(pixelX: Int, pixelY: Int): Pair<Double, Double> {
        val worldX = worldOriginX + pixelX * resolution
        val worldY = worldOriginY + pixelY * resolution
        return Pair(worldX, worldY)
    }
    
    // =================== 资源管理实现 ===================
    
    /**
     * 释放Provider相关资源
     * 
     * 主要处理对象池的资源回收：
     * - 如果像素数据来自对象池，则归还给对象池
     * - 清理内部引用，避免内存泄漏
     */
    override fun close() {
        if (isFromObjectPool && objectPool != null) {
            try {
                objectPool.returnArray(parcelData.pixelData)
                android.util.Log.v(TAG, "像素数据已返回对象池")
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.w(TAG, "返回对象池失败", e)
            }
        }
    }
}
