package com.houtrry.lopengles20.layer

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * 地图数据源抽象：统一处理fd/文件/bitmap等多种数据源
 * 
 * 核心思想：
 * - 提供统一的数据源接口，简化MapLayer的数据源切换
 * - 支持运行时动态切换数据源类型
 * - 集成内存优化和性能监控
 * - 向下兼容现有的bitmap渲染方式
 * 
 * 支持的数据源类型：
 * - ParcelFileDescriptor：动态更新的fd数据（10fps），支持内存优化
 * - File：静态地图文件（PNG/JPEG），低频更新
 * - Bitmap：内存中的位图数据，兼容现有代码
 */
sealed class MapDataSource {
    
    /**
     * ParcelFileDescriptor数据源
     * 
     * 特点：
     * - 高频更新（10fps典型）
     * - 内存优化：对象池复用、增量更新检测
     * - 自适应策略：小地图内存加载，大地图流式读取
     * - 适用场景：实时建图、动态地图更新
     */
    data class ParcelFd(
        val parcelFileDescriptor: ParcelFileDescriptor,
        val enableOptimization: Boolean = true
    ) : MapDataSource()
    
    /**
     * 文件数据源（PNG/JPEG）
     * 
     * 特点：
     * - 静态文件，更新频率低
     * - 支持区域解码，节省内存
     * - 适合大尺寸静态地图
     * - 适用场景：离线地图、静态底图
     */
    data class ImageFile(
        val file: File,
        val useCpuGrayscale: Boolean = false // CPU转灰度以节省GPU带宽
    ) : MapDataSource()
    
    /**
     * Bitmap数据源
     * 
     * 特点：
     * - 兼容现有代码，无需大幅修改
     * - 内存占用较高，适合小地图
     * - 处理简单，性能稳定
     * - 适用场景：小尺寸地图、原型验证
     */
    data class BitmapData(
        val bitmap: Bitmap
    ) : MapDataSource()
    
    /**
     * 多数据源混合（高级用法）
     * 
     * 特点：
     * - 支持基底图+动态更新层的混合渲染
     * - 静态底图 + 实时更新区域
     * - 复杂但灵活，适合高级应用场景
     */
    data class Hybrid(
        val baseSource: MapDataSource,
        val overlaySource: MapDataSource? = null
    ) : MapDataSource()
}

/**
 * 数据源变化事件
 */
data class DataSourceChangeEvent(
    val oldSource: MapDataSource?,
    val newSource: MapDataSource,
    val changeReason: String
)

/**
 * 数据源性能统计
 */
data class DataSourceStats(
    val dataSourceType: String,
    val updateCount: Int,
    val memoryUsageMB: Float,
    val avgUpdateTimeMs: Float,
    val optimizationEnabled: Boolean,
    val cacheHitRate: Float? = null
)

/**
 * 数据源管理器：负责数据源的创建、切换和生命周期管理
 */
interface MapDataSourceManager {
    
    /**
     * 设置数据源
     * 
     * @param dataSource 新的数据源
     * @return 是否设置成功
     */
    fun setDataSource(dataSource: MapDataSource): Boolean
    
    /**
     * 获取当前数据源
     */
    fun getCurrentDataSource(): MapDataSource?
    
    /**
     * 获取性能统计
     */
    fun getStats(): DataSourceStats?
    
    /**
     * 数据源变化监听器
     */
    fun setDataSourceChangeListener(listener: (DataSourceChangeEvent) -> Unit)
    
    /**
     * 释放资源
     */
    fun release()
}
