package com.houtrry.lopengles20.tile

import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 智能瓦片加载管理器：解决分帧加载中的中断和优先级问题
 * 
 * 核心解决方案：
 * ✅ 优先级队列：当前可见 > 即将可见 > 历史缓存
 * ✅ 队列管理：避免无限累积，及时清理过时Tile
 * ✅ 中断恢复：视口改变时智能调整加载优先级
 * ✅ 负载均衡：根据帧率自动调整加载速度
 * 
 * 解决用户提出的问题：
 * 1. Tile渲染中断问题：通过优先级队列确保重要Tile优先加载
 * 2. 队列污染问题：定期清理不再需要的Tile，避免内存浪费
 */
class SmartTileLoadingManager {
    
    companion object {
        private const val TAG = "SmartTileLoading"
    }
    
    private val lock = ReentrantLock()
    
    // 分级加载队列
    private val highPriorityQueue = ConcurrentLinkedQueue<TileCoord>()    // 当前可见
    private val mediumPriorityQueue = ConcurrentLinkedQueue<TileCoord>()  // 边缘可见  
    private val lowPriorityQueue = ConcurrentLinkedQueue<TileCoord>()     // 预加载
    
    // 正在加载的Tile（避免重复加载）
    private val loadingTiles = mutableSetOf<TileCoord>()
    
    // 性能监控
    private var frameCount = 0
    private var totalLoadTime = 0L
    private var avgFrameTime = 16f
    
    /**
     * 智能添加待加载Tile：根据距离视口中心的远近分配优先级
     */
    fun addPendingTiles(
        visibleTiles: List<TileCoord>,
        viewportCenter: Pair<Int, Int>,
        clearOldQueue: Boolean = false
    ) {
        lock.withLock {
            if (clearOldQueue) {
                // 视口大幅改变时，清理旧队列
                clearQueues()
                Log.d(TAG, "清理旧加载队列")
            }
            
            for (coord in visibleTiles) {
                if (loadingTiles.contains(coord)) {
                    continue  // 跳过正在加载的
                }
                
                // 根据距离视口中心的远近分配优先级
                val distance = calculateDistance(coord, viewportCenter)
                val queue = when {
                    distance <= 1 -> highPriorityQueue      // 视口中心
                    distance <= 3 -> mediumPriorityQueue    // 边缘可见
                    else -> lowPriorityQueue                // 预加载
                }
                
                if (!queue.contains(coord)) {
                    queue.offer(coord)
                }
            }
            
            Log.d(TAG, "加载队列状态: 高优先级${highPriorityQueue.size}, " +
                      "中优先级${mediumPriorityQueue.size}, 低优先级${lowPriorityQueue.size}")
        }
    }
    
    /**
     * 智能分帧加载：优先级 + 帧率自适应
     */
    fun loadPendingOnGlThread(
        maxCount: Int,
        tileLoader: (TileCoord) -> Boolean  // 实际的Tile加载函数
    ): Int {
        val frameStartTime = System.currentTimeMillis()
        var loadedCount = 0
        val actualMaxCount = adaptiveMaxCount(maxCount)
        
        lock.withLock {
            // 按优先级顺序加载
            val queues = listOf(highPriorityQueue, mediumPriorityQueue, lowPriorityQueue)
            
            for (queue in queues) {
                while (queue.isNotEmpty() && loadedCount < actualMaxCount) {
                    val coord = queue.poll() ?: break
                    
                    if (loadingTiles.contains(coord)) {
                        continue  // 跳过正在加载的
                    }
                    
                    loadingTiles.add(coord)
                    
                    try {
                        val success = tileLoader(coord)
                        if (success) {
                            loadedCount++
                            Log.v(TAG, "成功加载Tile: $coord")
                        } else {
                            // 加载失败，重新加入低优先级队列
                            lowPriorityQueue.offer(coord)
                        }
                    } finally {
                        loadingTiles.remove(coord)
                    }
                }
                
                if (loadedCount >= actualMaxCount) break
            }
        }
        
        // 性能监控和自适应调整
        val frameTime = System.currentTimeMillis() - frameStartTime
        updatePerformanceMetrics(frameTime.toFloat())
        
        Log.d(TAG, "本帧加载: ${loadedCount}个Tile, 耗时: ${frameTime}ms, 自适应限制: $actualMaxCount")
        return loadedCount
    }
    
    /**
     * 视口大幅改变时的智能处理
     */
    fun onViewportSignificantChange(
        newVisibleTiles: List<TileCoord>,
        viewportCenter: Pair<Int, Int>
    ) {
        Log.d(TAG, "检测到视口大幅改变，重新调整加载优先级")
        
        // 清理队列并重新分配优先级
        addPendingTiles(newVisibleTiles, viewportCenter, clearOldQueue = true)
        
        // 清理正在加载但不再需要的Tile
        lock.withLock {
            val visibleSet = newVisibleTiles.toSet()
            val obsoleteLoading = loadingTiles.filter { it !in visibleSet }
            loadingTiles.removeAll(obsoleteLoading)
            
            Log.d(TAG, "清理过时加载任务: ${obsoleteLoading.size}个")
        }
    }
    
    /**
     * 计算Tile到视口中心的距离
     */
    private fun calculateDistance(coord: TileCoord, center: Pair<Int, Int>): Int {
        val dx = kotlin.math.abs(coord.x - center.first)
        val dy = kotlin.math.abs(coord.y - center.second)
        return kotlin.math.max(dx, dy)  // 切比雪夫距离
    }
    
    /**
     * 帧率自适应：根据平均帧时间调整每帧加载量
     */
    private fun adaptiveMaxCount(baseMaxCount: Int): Int {
        return when {
            avgFrameTime > 20f -> kotlin.math.max(1, baseMaxCount - 1)  // 帧率过低，减少加载
            avgFrameTime < 12f -> kotlin.math.min(6, baseMaxCount + 1)  // 帧率充足，增加加载
            else -> baseMaxCount  // 帧率正常，保持不变
        }
    }
    
    /**
     * 更新性能指标
     */
    private fun updatePerformanceMetrics(frameTime: Float) {
        frameCount++
        totalLoadTime += frameTime.toLong()
        
        // 计算指数移动平均
        avgFrameTime = avgFrameTime * 0.9f + frameTime * 0.1f
        
        // 每100帧打印一次统计
        if (frameCount % 100 == 0) {
            val avgTotal = totalLoadTime.toFloat() / frameCount
            Log.i(TAG, "性能统计(${frameCount}帧): 平均帧时间=${String.format("%.1f", avgFrameTime)}ms, " +
                      "平均总耗时=${String.format("%.1f", avgTotal)}ms")
        }
    }
    
    /**
     * 清理所有队列
     */
    private fun clearQueues() {
        highPriorityQueue.clear()
        mediumPriorityQueue.clear()
        lowPriorityQueue.clear()
        loadingTiles.clear()
    }
    
    /**
     * 获取当前队列状态
     */
    fun getQueueStats(): QueueStats {
        return QueueStats(
            highPriorityCount = highPriorityQueue.size,
            mediumPriorityCount = mediumPriorityQueue.size,
            lowPriorityCount = lowPriorityQueue.size,
            loadingCount = loadingTiles.size,
            avgFrameTime = avgFrameTime
        )
    }
    
    /**
     * 队列状态数据类
     */
    data class QueueStats(
        val highPriorityCount: Int,
        val mediumPriorityCount: Int,
        val lowPriorityCount: Int,
        val loadingCount: Int,
        val avgFrameTime: Float
    )
}
