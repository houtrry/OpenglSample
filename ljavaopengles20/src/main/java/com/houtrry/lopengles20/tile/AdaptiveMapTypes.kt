package com.houtrry.lopengles20.tile

import android.util.Log

/**
 * 自适应地图加载器相关的类型定义
 */

/**
 * 加载策略枚举
 */
enum class LoadingStrategy {
    /** 一次性内存加载：适用于小地图，简单高效 */
    IN_MEMORY,
    
    /** 内存优化的一次性加载：适用于小地图，对象池复用 */
    IN_MEMORY_OPTIMIZED,
    
    /** 流式读取：适用于大地图，内存可控 */
    STREAMING
}

/**
 * 地图元数据
 */
data class MapMetadata(
    val originX: Double,
    val originY: Double,
    val resolution: Double,
    val width: Int,
    val height: Int,
    val area: Int
)

/**
 * 自适应RegionProvider
 */
open class AdaptiveRegionProvider(
    private val delegate: Any,
    val strategy: LoadingStrategy,
    val metadata: AdaptiveMetadata,
    private val objectPool: ByteArrayPool?
) : RegionProvider {
    
    override fun obtainRegion(x: Int, y: Int, width: Int, height: Int): RegionBuffer? {
        return when (delegate) {
            is RegionProvider -> delegate.obtainRegion(x, y, width, height)
            else -> null
        }
    }
    
    open fun worldToMapPixel(worldX: Double, worldY: Double): Pair<Int, Int>? {
        return when (delegate) {
            is ParcelGrayscaleRegionProvider -> delegate.worldToMapPixel(worldX, worldY)
            is StreamingGrayscaleRegionProvider -> delegate.worldToMapPixel(worldX, worldY)
            else -> null
        }
    }
    
    fun mapPixelToWorld(pixelX: Int, pixelY: Int): Pair<Double, Double> {
        return when (delegate) {
            is ParcelGrayscaleRegionProvider -> delegate.mapPixelToWorld(pixelX, pixelY)
            is StreamingGrayscaleRegionProvider -> delegate.mapPixelToWorld(pixelX, pixelY)
            else -> Pair(0.0, 0.0)
        }
    }
    
    fun getCacheStats(): StreamingGrayscaleRegionProvider.CacheStats? {
        return (delegate as? StreamingGrayscaleRegionProvider)?.getCacheStats()
    }
    
    /**
     * 关闭Provider并清理资源
     * 
     * 直接委托给底层RegionProvider的close()方法
     * 简化了实现，利用了RegionProvider接口的统一资源管理
     */
    override fun close() {
        when (delegate) {
            is RegionProvider -> delegate.close()
            else -> {
                // 兼容非RegionProvider类型的delegate（应该很少见）
                Log.w("AdaptiveRegionProvider", "未知的delegate类型: ${delegate::class.java}")
            }
        }
    }
}

/**
 * 自适应地图元数据
 */
data class AdaptiveMetadata(
    val originX: Double,
    val originY: Double,
    val resolution: Double,
    val width: Int,
    val height: Int,
    val area: Int,
    val memoryUsage: Long,
    val fromPool: Boolean  // 是否来自对象池
)

/**
 * 性能统计
 */
data class PerformanceStats(
    val totalUpdates: Int,
    val poolHits: Int,
    val poolHitRate: Float,
    val incrementalSkips: Int,
    val incrementalSkipRate: Float
)

/**
 * 对象池统计
 */
data class PoolStats(
    val poolSize: Int,
    val maxPoolSize: Int
)

/**
 * 优化统计信息
 */
data class OptimizationStats(
    val strategy: LoadingStrategy,
    val mapSize: String,
    val memoryUsage: Long,
    val fromPool: Boolean,
    val poolHitRate: Float,
    val incrementalSkipRate: Float,
    val totalUpdates: Int,
    val cacheStats: StreamingGrayscaleRegionProvider.CacheStats?
)

/**
 * 优化分析结果
 */
data class OptimizationAnalysis(
    val memoryReductionPercent: Int,        // 内存分配减少百分比
    val poolEfficiency: Int,                // 对象池效率
    val incrementalEfficiency: Int,         // 增量更新效率  
    val recommendation: String,             // 优化建议
    val gcImpactReduction: Int             // GC影响减少估算
) {
    companion object {
        fun empty() = OptimizationAnalysis(0, 0, 0, "无数据", 0)
    }
}
