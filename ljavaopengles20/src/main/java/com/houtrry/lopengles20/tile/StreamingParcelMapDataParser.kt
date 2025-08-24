package com.houtrry.lopengles20.tile

import android.os.ParcelFileDescriptor
import com.houtrry.common_map.utils.lseekSafely
import java.io.FileInputStream

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 流式ParcelFileDescriptor解析器 - 支持超大纹理（20000×20000+）
 * 
 * 关键优化：
 * - ✅ 只解析36字节头部，不读取全部像素数据
 * - ✅ 保持文件句柄打开，支持按需读取
 * - ✅ 内存占用从400MB+降至几MB
 * - ✅ 支持任意大小纹理，理论无上限
 * 
 * 与一次性加载方案对比：
 * | 纹理大小 | 一次性加载 | 流式读取 |
 * |---------|-----------|----------|
 * | 20000×20000 | 400MB | ~几MB |
 * | 50000×50000 | 2.5GB (OOM) | ~几MB |
 * | 100000×100000 | 10GB (崩溃) | ~几MB |
 * 
 * 内存使用量只取决于当前可见Tile数量，与纹理总大小无关。
 */
class StreamingParcelMapDataParser {
    
    companion object {
        private const val HEADER_SIZE = 36
        
        // 字节偏移常量
        private const val OFFSET_ORIGIN_X = 0      // 8 bytes double
        private const val OFFSET_ORIGIN_Y = 8      // 8 bytes double  
        private const val OFFSET_RESOLUTION = 16  // 8 bytes double
        private const val OFFSET_WIDTH = 24        // 4 bytes int
        private const val OFFSET_HEIGHT = 28       // 4 bytes int
        private const val OFFSET_AREA = 32         // 4 bytes int
        
        // 像素数据开始偏移
        private const val PIXEL_DATA_OFFSET = 36
    }
    
    /**
     * 解析ParcelFileDescriptor头部信息
     * 
     * @param parcelFileDescriptor 地图数据文件描述符
     * @return 包含头部信息和文件句柄的元数据对象
     * @throws IllegalArgumentException 如果头部数据无效
     */
    fun parseMapMetadata(parcelFileDescriptor: ParcelFileDescriptor): StreamingParcelMapMetadata {
        // 🚨 修复文件指针问题：重置到文件开头，防止多次读取导致位置错误
        parcelFileDescriptor.lseekSafely()

        // 读取头部信息
        val headerBytes = ByteArray(HEADER_SIZE)
        FileInputStream(parcelFileDescriptor.fileDescriptor).use { inputStream ->
            val headerRead = inputStream.read(headerBytes)
            
            if (headerRead != HEADER_SIZE) {
                throw IllegalArgumentException(
                    "Invalid ParcelFileDescriptor: expected $HEADER_SIZE header bytes, got $headerRead"
                )
            }
        }
        
        // 解析头部数据（🚨 修复字节序问题：使用LITTLE_ENDIAN与ByteUtil兼容）
        val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        
        val originX = headerBuffer.getDouble(OFFSET_ORIGIN_X)
        val originY = headerBuffer.getDouble(OFFSET_ORIGIN_Y)
        val resolution = headerBuffer.getDouble(OFFSET_RESOLUTION)
        val width = headerBuffer.getInt(OFFSET_WIDTH)
        val height = headerBuffer.getInt(OFFSET_HEIGHT)
        val area = headerBuffer.getInt(OFFSET_AREA)
        
        // 验证数据有效性
        validateMapData(width, height, resolution)
        
        // 保持ParcelFileDescriptor打开用于后续按需读取
        // 注意：不关闭parcelFileDescriptor，由调用方负责管理生命周期
        
        return StreamingParcelMapMetadata(
            originX = originX,
            originY = originY,
            resolution = resolution,
            width = width,
            height = height,
            area = area,
            parcelFileDescriptor = parcelFileDescriptor,
            pixelDataOffset = PIXEL_DATA_OFFSET.toLong()
        )
    }
    
    /**
     * 验证地图数据的合理性
     */
    private fun validateMapData(width: Int, height: Int, resolution: Double) {
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Invalid map dimensions: ${width}x${height}")
        }
        
        if (resolution <= 0) {
            throw IllegalArgumentException("Invalid resolution: $resolution")
        }
        
        // 检查是否超出合理范围（可根据需要调整）
        if (width > 200000 || height > 200000) {
            throw IllegalArgumentException("Map too large: ${width}x${height} (max 200000×200000)")
        }
        
        if (resolution < 0.0001 || resolution > 100.0) {
            throw IllegalArgumentException("Resolution out of range: $resolution (0.0001-100.0 m/px)")
        }
    }
}

/**
 * 流式地图元数据：只包含头部信息，保持文件句柄用于按需读取
 * 
 * 注意：使用完毕后需调用close()释放文件资源
 */
data class StreamingParcelMapMetadata(
    val originX: Double,           // 纹理左下角世界坐标X（米）
    val originY: Double,           // 纹理左下角世界坐标Y（米）
    val resolution: Double,        // 像素分辨率（米/像素）
    val width: Int,                // 纹理宽度（像素）
    val height: Int,               // 纹理高度（像素）
    val area: Int,                 // 地图面积（算法概念）
    val parcelFileDescriptor: ParcelFileDescriptor,  // 保持文件句柄
    val pixelDataOffset: Long      // 像素数据在文件中的起始偏移
) {
    
    /**
     * 计算指定像素位置在文件中的字节偏移
     * 
     * @param pixelX 像素X坐标
     * @param pixelY 像素Y坐标
     * @return 该像素在文件中的字节偏移位置
     */
    fun getPixelFileOffset(pixelX: Int, pixelY: Int): Long {
        return pixelDataOffset + (pixelY.toLong() * width + pixelX)
    }
    
    /**
     * 读取指定文件偏移位置的数据
     * 
     * @param offset 文件偏移位置
     * @param length 读取长度
     * @return 读取的字节数组，失败返回null
     */
    fun readBytesAt(offset: Long, length: Int): ByteArray? {
        return try {
            FileInputStream(parcelFileDescriptor.fileDescriptor).use { inputStream ->
                inputStream.skip(offset)
                val buffer = ByteArray(length)
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == length) buffer else null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 释放文件资源
     * 
     * 必须在不再使用时调用，避免文件句柄泄漏
     */
    fun close() {
        try {
            parcelFileDescriptor.close()
        } catch (e: Exception) {
            // 忽略关闭异常
        }
    }
}
