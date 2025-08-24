package com.houtrry.lopengles20.tile

import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * 渐进式地图管理器
 * 
 * 核心特性：
 * ✅ 地图渐进变大时自动选择最优策略
 * ✅ 平滑切换：从一次性加载→流式读取
 * ✅ 性能监控：记录策略切换和性能指标
 * ✅ 内存安全：避免OOM和内存泄漏
 * 
 * 使用场景：
 * - 新建地图：0×0 → 20000×20000
 * - 扩展地图：200×200 → 10000×10000
 * - 算法实时输出：10fps更新频率
 */
class ProgressiveMapManager(
    // 根据设备配置选择合适的加载器
    private val adaptiveLoader: AdaptiveMapLoader = AdaptiveMapLoader.Companion.Preset.MID_RANGE
) {
    
    companion object {
        private const val TAG = "ProgressiveMapManager"
    }
    
    private var currentProvider: AdaptiveRegionProvider? = null
    private var lastStrategy: LoadingStrategy? = null
    private var updateCount = 0
    
    /**
     * 更新地图数据（支持渐进变大）
     * 
     * 每次调用都会重新评估策略：
     * - 小地图保持一次性加载的高效性
     * - 地图变大时自动切换到流式读取
     * - 避免不必要的策略切换开销
     */
    fun updateMapData(parcelFd: ParcelFileDescriptor): Boolean {
        return try {
            updateCount++
            
            // 1. 清理旧provider，避免内存泄漏
            cleanupCurrentProvider()
            
            // 2. 自适应创建新provider
            val newProvider = adaptiveLoader.createRegionProvider(parcelFd)
            val newStrategy = newProvider.strategy
            
            // 3. 检测策略变化
            if (lastStrategy != null && lastStrategy != newStrategy) {
                logStrategyTransition(lastStrategy!!, newStrategy, newProvider.metadata)
            }
            
            // 4. 更新状态
            currentProvider = newProvider
            lastStrategy = newStrategy
            
            Log.d(TAG, "地图更新成功 #$updateCount: ${newStrategy}")
            Log.d(TAG, "地图信息: ${newProvider.metadata.width}×${newProvider.metadata.height}, " +
                      "内存使用: ${formatSize(newProvider.metadata.memoryUsage)}")
            
            true
            
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "地图更新失败 #$updateCount", e)
            false
        }
    }
    
    /**
     * 获取区域数据（统一接口）
     */
    fun obtainTileRegion(tileX: Int, tileY: Int, tileSize: Int = 256): RegionBuffer? {
        val provider = currentProvider ?: run {
            Log.w(TAG, "Provider未初始化")
            return null
        }
        
        val pixelX = tileX * tileSize
        val pixelY = tileY * tileSize
        
        return provider.obtainRegion(pixelX, pixelY, tileSize, tileSize)
    }
    
    /**
     * 世界坐标转换
     */
    fun worldToMapPixel(worldX: Double, worldY: Double): Pair<Int, Int>? {
        return currentProvider?.worldToMapPixel(worldX, worldY)
    }
    
    /**
     * 获取当前策略信息
     */
    fun getCurrentStrategyInfo(): StrategyInfo? {
        val provider = currentProvider ?: return null
        
        return StrategyInfo(
            strategy = provider.strategy,
            mapSize = "${provider.metadata.width}×${provider.metadata.height}",
            memoryUsage = provider.metadata.memoryUsage,
            cacheStats = provider.getCacheStats() // 仅流式策略有效
        )
    }
    
    /**
     * 策略切换性能分析
     */
    fun analyzeStrategyPerformance(): PerformanceAnalysis {
        val provider = currentProvider
        if (provider == null) {
            return PerformanceAnalysis.empty()
        }
        
        return when (provider.strategy) {
            LoadingStrategy.IN_MEMORY -> {
                PerformanceAnalysis(
                    strategy = "一次性加载",
                    memoryUsage = provider.metadata.memoryUsage,
                    advantages = listOf("启动快", "无IO开销", "简单稳定"),
                    disadvantages = listOf("内存占用大", "不适合超大地图"),
                    recommendation = if (provider.metadata.memoryUsage > 32 * 1024 * 1024) {
                        "建议地图再大一点时切换到流式读取"
                    } else {
                        "当前策略最优"
                    }
                )
            }
            LoadingStrategy.STREAMING -> {
                val cacheStats = provider.getCacheStats()
                PerformanceAnalysis(
                    strategy = "流式读取",
                    memoryUsage = provider.metadata.memoryUsage,
                    advantages = listOf("内存可控", "支持无限大小", "智能缓存"),
                    disadvantages = listOf("启动稍慢", "需要IO操作"),
                    recommendation = if (cacheStats?.hitRate ?: 0f > 0.8f) {
                        "缓存命中率良好，性能最优"
                    } else {
                        "考虑预加载关键区域提升性能"
                    }
                )
            }
            LoadingStrategy.IN_MEMORY_OPTIMIZED -> {
                PerformanceAnalysis(
                    strategy = "内存优化加载",
                    memoryUsage = provider.metadata.memoryUsage,
                    advantages = listOf("启动快", "对象池复用", "GC友好"),
                    disadvantages = listOf("内存占用大", "不适合超大地图"),
                    recommendation = if (provider.metadata.memoryUsage > 32 * 1024 * 1024) {
                        "建议地图再大一点时切换到流式读取"
                    } else {
                        "当前策略最优"
                    }
                )
            }
        }
    }
    
    /**
     * 清理当前provider
     */
    private fun cleanupCurrentProvider() {
        currentProvider?.let { provider ->
            // 如果是流式provider，需要显式关闭
            when (val delegate = getDelegate(provider)) {
                is StreamingGrayscaleRegionProvider -> {
                    delegate.close()
                    Log.d(TAG, "流式Provider已清理")
                }
                is ParcelGrayscaleRegionProvider -> {
                    // 一次性provider无需特殊清理
                    Log.d(TAG, "一次性Provider已清理")
                }
                else -> {
                    // 处理其他类型的provider
                    Log.d(TAG, "未知类型Provider已清理")
                }
            }
        }
        currentProvider = null
    }
    
    /**
     * 记录策略切换日志
     */
    private fun logStrategyTransition(
        oldStrategy: LoadingStrategy, 
        newStrategy: LoadingStrategy,
        metadata: AdaptiveMetadata
    ) {
        Log.i(TAG, "策略切换: $oldStrategy → $newStrategy")
        Log.i(TAG, "触发原因: 地图大小 ${metadata.width}×${metadata.height}, " +
                   "内存需求 ${formatSize(metadata.memoryUsage)}")
        
        when (newStrategy) {
            LoadingStrategy.IN_MEMORY -> {
                Log.i(TAG, "切换到一次性加载: 追求极致性能")
            }
            LoadingStrategy.STREAMING -> {
                Log.i(TAG, "切换到流式读取: 避免内存压力")
            }
            LoadingStrategy.IN_MEMORY_OPTIMIZED -> {
                Log.i(TAG, "切换到内存优化加载: 对象池复用 + 增量更新")
            }
        }
    }
    
    /**
     * 获取provider的实际delegate对象
     */
    private fun getDelegate(provider: AdaptiveRegionProvider): Any? {
        return try {
            val field = AdaptiveRegionProvider::class.java.getDeclaredField("delegate")
            field.isAccessible = true
            field.get(provider)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 格式化字节大小
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
    
    /**
     * 释放所有资源
     */
    fun cleanup() {
        cleanupCurrentProvider()
        Log.d(TAG, "渐进式地图管理器已清理")
    }
    
    /**
     * 策略信息数据类
     */
    data class StrategyInfo(
        val strategy: LoadingStrategy,
        val mapSize: String,
        val memoryUsage: Long,
        val cacheStats: StreamingGrayscaleRegionProvider.CacheStats?
    )
    
    /**
     * 性能分析数据类
     */
    data class PerformanceAnalysis(
        val strategy: String,
        val memoryUsage: Long,
        val advantages: List<String>,
        val disadvantages: List<String>,
        val recommendation: String
    ) {
        companion object {
            fun empty() = PerformanceAnalysis("未知", 0, emptyList(), emptyList(), "无数据")
        }
    }
}
