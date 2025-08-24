package com.houtrry.lopengles20.tile

import android.os.ParcelFileDescriptor
import com.houtrry.common_map.utils.lseekSafely
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ParcelFileDescriptor 地图数据解析器
 * 
 * 功能：
 * - 解析36字节固定头部（3个double + 3个int）
 * - 提取地图元数据（坐标原点、分辨率、尺寸）
 * - 返回纯像素数据供RegionProvider使用
 * 
 * 数据格式：
 * ```
 * [Header: 36 bytes固定]
 * - originX: 8字节 double，纹理左下角世界坐标X（米）
 * - originY: 8字节 double，纹理左下角世界坐标Y（米）  
 * - resolution: 8字节 double，像素分辨率（米/像素）
 * - width: 4字节 int，纹理宽度（像素）
 * - height: 4字节 int，纹理高度（像素）
 * - area: 4字节 int，地图面积（算法概念）
 * [Payload: 变长]
 * - pixelData: byte array，纹理像素数据（灰度图或JPEG）
 * ```
 * 
 * 线程安全：本类不维护状态，可多线程安全调用
 */
class ParcelMapDataParser : ParcelDataParser {
    
    companion object {
        private const val HEADER_SIZE = 36
        
        // 字节偏移常量
        private const val OFFSET_ORIGIN_X = 0      // 8 bytes double
        private const val OFFSET_ORIGIN_Y = 8      // 8 bytes double  
        private const val OFFSET_RESOLUTION = 16  // 8 bytes double
        private const val OFFSET_WIDTH = 24        // 4 bytes int
        private const val OFFSET_HEIGHT = 28       // 4 bytes int
        private const val OFFSET_AREA = 32         // 4 bytes int
    }
    
    override fun parseMapData(parcelFileDescriptor: ParcelFileDescriptor): ParcelMapData {
        // 🚨 修复文件指针问题：重置到文件开头
        parcelFileDescriptor.lseekSafely()
        return FileInputStream(parcelFileDescriptor.fileDescriptor).use { inputStream ->
            // 读取36字节固定头部
            val headerBytes = ByteArray(HEADER_SIZE)
            val headerRead = inputStream.read(headerBytes)
            
            if (headerRead != HEADER_SIZE) {
                throw IllegalArgumentException(
                    "Invalid ParcelFileDescriptor: expected $HEADER_SIZE header bytes, got $headerRead"
                )
            }
            
            // 解析头部数据（🚨 修复字节序问题：使用LITTLE_ENDIAN与ByteUtil兼容）
            val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
            
            val originX = headerBuffer.getDouble(OFFSET_ORIGIN_X)
            val originY = headerBuffer.getDouble(OFFSET_ORIGIN_Y)
            val resolution = headerBuffer.getDouble(OFFSET_RESOLUTION)
            val width = headerBuffer.getInt(OFFSET_WIDTH)
            val height = headerBuffer.getInt(OFFSET_HEIGHT)
            val area = headerBuffer.getInt(OFFSET_AREA)
            
            // 验证地图尺寸合理性
            if (width <= 0 || height <= 0) {
                throw IllegalArgumentException("Invalid map dimensions: ${width}x${height}")
            }
            
            if (resolution <= 0) {
                throw IllegalArgumentException("Invalid resolution: $resolution")
            }
            
            // 读取剩余像素数据
            val pixelData = inputStream.readBytes()
            val expectedPixelSize = width * height
            
            // 检查像素数据大小（灰度图应为 width*height，JPEG可能不同）
            if (pixelData.size < expectedPixelSize) {
                throw IllegalArgumentException(
                    "Insufficient pixel data: expected at least $expectedPixelSize bytes, got ${pixelData.size}"
                )
            }
            
            ParcelMapData(
                originX = originX,
                originY = originY,
                resolution = resolution,
                width = width,
                height = height,
                area = area,
                pixelData = pixelData
            )
        }
    }
}
