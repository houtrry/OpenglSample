package com.houtrry.common_map.text

import android.content.Context
import android.util.Log
import com.houtrry.common_map.data.*
import java.util.Timer
import java.util.TimerTask

/**
 * 混合文字渲染器 (OpenGL ES 2.0) - 升级版
 * 智能地在Canvas、SDF和复杂文字渲染之间切换，支持图标、气泡等复杂显示
 */
class HybridTextRenderer(context: Context) : BaseTextRenderer(context) {
    
    companion object {
        private const val TAG = "HybridTextRenderer"
        
        // 默认配置
        private const val DEFAULT_SDF_THRESHOLD = 50     // SDF切换阈值
        private const val DEFAULT_MIGRATION_DELAY = 3000L // 迁移延迟(毫秒)
        private const val DEFAULT_BATCH_SIZE = 10        // 批处理大小
        private const val DEFAULT_COMPLEX_TEXT_THRESHOLD = 20 // 复杂文字阈值
    }
    
    // 渲染器实例
    private val canvasRenderer = CanvasTextRenderer(context)
    private val sdfRenderer = SDFTextRenderer(context)
    private val complexRenderer = ComplexTextRenderer(context) // 新增复杂文字渲染器
    
    // 配置参数
    private var sdfThreshold = DEFAULT_SDF_THRESHOLD
    private var migrationDelay = DEFAULT_MIGRATION_DELAY
    private var batchSize = DEFAULT_BATCH_SIZE
    private var complexTextThreshold = DEFAULT_COMPLEX_TEXT_THRESHOLD
    
    // 当前渲染策略
    private var currentStrategy: RenderingStrategy = CanvasStrategy()
    private var scenario: RenderingScenario = RenderingScenario.NewMapScenario
    
    // 迁移管理
    private val pendingTexts = mutableSetOf<String>()
    private var migrationTimer: Timer? = null
    private var lastMigrationCheck = 0L
    
    // 文字分类统计
    private var simpleTextCount = 0
    private var complexTextCount = 0
    
    // 性能统计
    private val performanceMonitor = PerformanceMonitor()
    
    /**
     * 初始化混合渲染器
     * @param scenario 渲染场景
     * @param config 可选配置
     */
    fun initialize(scenario: RenderingScenario, config: HybridConfig = HybridConfig()) {
        this.scenario = scenario
        this.sdfThreshold = config.sdfThreshold
        this.migrationDelay = config.migrationDelay
        this.batchSize = config.batchSize
        this.complexTextThreshold = config.complexTextThreshold
        
        Log.d(TAG, "初始化混合渲染器(升级版)，场景: ${scenario.javaClass.simpleName}")
        
        when (scenario) {
            is RenderingScenario.NewMapScenario -> {
                initializeForNewMap()
            }
            is RenderingScenario.ExtendMapScenario -> {
                initializeForExtendMap(scenario.existingTexts)
            }
        }
        
        // 启动性能监控
        performanceMonitor.start()
    }
    
    override fun initialize() {
        // 默认初始化为新建地图场景
        initialize(RenderingScenario.NewMapScenario)
    }
    
    private fun initializeForNewMap() {
        // 新建地图：开始使用Canvas策略
        currentStrategy = CanvasStrategy()
        canvasRenderer.initialize()
        
        Log.d(TAG, "新建地图模式，使用Canvas渲染")
        isInitialized = true
    }
    
    private fun initializeForExtendMap(existingTexts: List<String>) {
        when {
            existingTexts.isEmpty() -> {
                // 空地图，等同于新建
                initializeForNewMap()
            }
            existingTexts.size < sdfThreshold -> {
                // 少量文字：继续使用Canvas
                currentStrategy = CanvasStrategy()
                canvasRenderer.initialize()
                
                // 批量添加已有文字（简化为纯文字）
                val existingTextInfos = existingTexts.map { content ->
                    TextInfo(content, com.houtrry.common_map.data.Vector3(0.0, 0.0, 0.0))
                }
                canvasRenderer.addTexts(existingTextInfos)
                existingTextInfos.forEach { textInfos[it.id] = it }
                simpleTextCount = existingTexts.size
                
                Log.d(TAG, "扩展地图模式，${existingTexts.size}个文字使用Canvas")
            }
            else -> {
                // 大量文字：直接使用混合策略
                currentStrategy = IntelligentHybridStrategy()
                sdfRenderer.initialize()
                canvasRenderer.initialize()
                complexRenderer.initialize()
                
                // 预处理已有文字为SDF
                Thread {
                    preloadTextsToSDF(existingTexts)
                }.start()
                
                Log.d(TAG, "扩展地图模式，${existingTexts.size}个文字预处理SDF")
            }
        }
        
        isInitialized = true
    }
    
    private fun preloadTextsToSDF(existingTexts: List<String>) {
        try {
            // 分析字符集
            val characters = existingTexts.flatMap { it.toCharArray().asIterable() }.toSet()
            
            // 生成SDF图集
            sdfRenderer.generateSdfAtlas(characters)
            
            // 添加文字到SDF渲染器（简化为纯文字）
            val textInfos = existingTexts.map { content ->
                TextInfo(content, com.houtrry.common_map.data.Vector3(0.0, 0.0, 0.0))
            }
            
            // 在主线程中添加
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                sdfRenderer.addTexts(textInfos)
                textInfos.forEach { this.textInfos[it.id] = it }
                simpleTextCount = existingTexts.size
                Log.d(TAG, "SDF预处理完成，${existingTexts.size}个文字")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SDF预处理失败", e)
            // 回退到Canvas
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                fallbackToCanvas(existingTexts)
            }
        }
    }
    
    private fun fallbackToCanvas(existingTexts: List<String>) {
        currentStrategy = CanvasStrategy()
        val textInfos = existingTexts.map { content ->
            TextInfo(content, com.houtrry.common_map.data.Vector3(0.0, 0.0, 0.0))
        }
        canvasRenderer.addTexts(textInfos)
        textInfos.forEach { this.textInfos[it.id] = it }
        simpleTextCount = existingTexts.size
        Log.d(TAG, "回退到Canvas模式")
    }
    
    override fun addText(textInfo: TextInfo) {
        if (!isInitialized) initialize()
        
        textInfos[textInfo.id] = textInfo
        
        // 更新统计
        if (textInfo.isComplexDisplay()) {
            complexTextCount++
        } else {
            simpleTextCount++
        }
        
        when (currentStrategy) {
            is CanvasStrategy -> handleCanvasAddText(textInfo)
            is HybridStrategy -> handleHybridAddText(textInfo)
            is IntelligentHybridStrategy -> handleIntelligentHybridAddText(textInfo)
        }
        
        onTextAdded(textInfo)
    }
    
    private fun handleCanvasAddText(textInfo: TextInfo) {
        // 根据复杂度选择渲染器
        if (textInfo.isComplexDisplay()) {
            complexRenderer.addText(textInfo)
        } else {
            canvasRenderer.addText(textInfo)
        }
        
        // 检查是否需要切换到智能混合模式
        if (shouldSwitchToIntelligentHybrid()) {
            Log.d(TAG, "达到智能切换阈值，启动智能混合模式迁移")
            startMigrationToIntelligentHybrid()
        }
    }
    
    private fun handleHybridAddText(textInfo: TextInfo) {
        // 传统混合模式：只处理简单文字
        if (textInfo.isComplexDisplay()) {
            complexRenderer.addText(textInfo)
        } else {
            // 新增文字先用Canvas立即显示
            canvasRenderer.addText(textInfo)
            
            // 标记为待迁移
            if (sdfRenderer.supportsText(textInfo.content)) {
                pendingTexts.add(textInfo.id)
                scheduleMigration()
            } else {
                Log.d(TAG, "文字包含不支持字符，继续Canvas: ${textInfo.content}")
            }
        }
    }
    
    private fun handleIntelligentHybridAddText(textInfo: TextInfo) {
        when {
            textInfo.isComplexDisplay() -> {
                // 复杂文字：直接用复杂渲染器
                complexRenderer.addText(textInfo)
                Log.d(TAG, "复杂文字使用ComplexRenderer: ${textInfo.content}")
            }
            
            simpleTextCount < sdfThreshold -> {
                // 简单文字数量少：用Canvas
                canvasRenderer.addText(textInfo)
                Log.d(TAG, "简单文字使用Canvas: ${textInfo.content}")
            }
            
            sdfRenderer.supportsText(textInfo.content) -> {
                // 简单文字且支持SDF：用SDF
                sdfRenderer.addText(textInfo)
                Log.d(TAG, "简单文字使用SDF: ${textInfo.content}")
            }
            
            else -> {
                // 不支持SDF的简单文字：用Canvas
                canvasRenderer.addText(textInfo)
                Log.d(TAG, "不支持SDF的文字使用Canvas: ${textInfo.content}")
            }
        }
    }
    
    private fun shouldSwitchToIntelligentHybrid(): Boolean {
        val totalCount = textInfos.size
        val avgRenderTime = performanceMonitor.getAverageRenderTime()
        val memoryUsage = getPerformanceStats().memoryUsage
        
        return when {
            totalCount >= sdfThreshold -> {
                Log.d(TAG, "数量阈值触发智能切换: $totalCount >= $sdfThreshold")
                true
            }
            complexTextCount >= complexTextThreshold -> {
                Log.d(TAG, "复杂文字阈值触发智能切换: $complexTextCount >= $complexTextThreshold")
                true
            }
            avgRenderTime > 15f -> {
                Log.d(TAG, "性能阈值触发智能切换: ${avgRenderTime}ms > 15ms")
                true
            }
            memoryUsage > 40_000_000 -> {
                Log.d(TAG, "内存阈值触发智能切换: ${memoryUsage / 1024 / 1024}MB > 40MB")
                true
            }
            else -> false
        }
    }
    
    private fun startMigrationToIntelligentHybrid() {
        currentStrategy = IntelligentHybridStrategy()
        
        Thread {
            try {
                // 初始化所有渲染器
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (!sdfRenderer.isInitialized) sdfRenderer.initialize()
                    if (!complexRenderer.isInitialized) complexRenderer.initialize()
                }
                Thread.sleep(100)
                
                // 分析现有文字并分类
                val allTexts = textInfos.values
                val simpleTexts = allTexts.filter { !it.isComplexDisplay() }
                val complexTexts = allTexts.filter { it.isComplexDisplay() }
                
                // 为简单文字生成SDF图集
                if (simpleTexts.isNotEmpty()) {
                    val characters = simpleTexts.flatMap { it.content.toCharArray().asIterable() }.toSet()
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        sdfRenderer.generateSdfAtlas(characters)
                    }
                    Thread.sleep(500)
                }
                
                // 迁移文字到合适的渲染器
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    // 清空当前渲染器
                    canvasRenderer.clear()
                    
                    // 重新分配
                    val sdfSupportedTexts = simpleTexts.filter { sdfRenderer.supportsText(it.content) }
                    val canvasSupportedTexts = simpleTexts.filter { !sdfRenderer.supportsText(it.content) }
                    
                    if (sdfSupportedTexts.isNotEmpty()) {
                        sdfRenderer.addTexts(sdfSupportedTexts)
                    }
                    if (canvasSupportedTexts.isNotEmpty()) {
                        canvasRenderer.addTexts(canvasSupportedTexts)
                    }
                    if (complexTexts.isNotEmpty()) {
                        complexRenderer.addTexts(complexTexts)
                    }
                    
                    Log.d(TAG, "智能混合模式迁移完成：SDF(${sdfSupportedTexts.size}) + Canvas(${canvasSupportedTexts.size}) + Complex(${complexTexts.size})")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "智能混合模式迁移失败", e)
                // 保持Canvas模式
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    currentStrategy = CanvasStrategy()
                }
            }
        }.start()
    }
    
    private fun scheduleMigration() {
        if (pendingTexts.size >= batchSize || 
            System.currentTimeMillis() - lastMigrationCheck > migrationDelay) {
            performMigration()
        } else {
            // 延迟迁移
            migrationTimer?.cancel()
            migrationTimer = Timer()
            migrationTimer?.schedule(object : TimerTask() {
                override fun run() {
                    performMigration()
                }
            }, migrationDelay)
        }
    }
    
    private fun performMigration() {
        if (pendingTexts.isEmpty()) return
        
        val textsToMigrate = pendingTexts.mapNotNull { textInfos[it] }
        
        if (textsToMigrate.isNotEmpty()) {
            sdfRenderer.addTexts(textsToMigrate)
            textsToMigrate.forEach { canvasRenderer.removeText(it.id) }
            
            Log.d(TAG, "批量迁移完成: ${textsToMigrate.size}个文字移至SDF")
        }
        
        pendingTexts.clear()
        lastMigrationCheck = System.currentTimeMillis()
    }
    
    override fun render(mvpMatrix: FloatArray) {
        if (!isInitialized || textInfos.isEmpty()) return
        
        val startTime = System.currentTimeMillis()
        
        when (currentStrategy) {
            is CanvasStrategy -> {
                // 分别渲染简单和复杂文字
                canvasRenderer.render(mvpMatrix)
                if (complexTextCount > 0) {
                    complexRenderer.render(mvpMatrix)
                }
            }
            is HybridStrategy -> {
                // 传统混合模式
                sdfRenderer.render(mvpMatrix)
                canvasRenderer.render(mvpMatrix)
                if (complexTextCount > 0) {
                    complexRenderer.render(mvpMatrix)
                }
            }
            is IntelligentHybridStrategy -> {
                // 智能混合模式：所有渲染器协同工作
                sdfRenderer.render(mvpMatrix)        // SDF文字（高性能）
                canvasRenderer.render(mvpMatrix)     // 不支持SDF的简单文字
                complexRenderer.render(mvpMatrix)    // 复杂文字
            }
        }
        
        val renderTime = (System.currentTimeMillis() - startTime).toFloat()
        lastRenderTime = renderTime
        performanceMonitor.recordRenderTime(renderTime)
    }
    
    override fun removeText(textId: String) {
        textInfos.remove(textId)?.let { removedText ->
            // 更新统计
            if (removedText.isComplexDisplay()) {
                complexTextCount--
            } else {
                simpleTextCount--
            }
            
            when (currentStrategy) {
                is CanvasStrategy -> {
                    if (removedText.isComplexDisplay()) {
                        complexRenderer.removeText(textId)
                    } else {
                        canvasRenderer.removeText(textId)
                    }
                }
                is HybridStrategy, is IntelligentHybridStrategy -> {
                    // 从所有渲染器中尝试移除
                    canvasRenderer.removeText(textId)
                    sdfRenderer.removeText(textId)
                    complexRenderer.removeText(textId)
                    pendingTexts.remove(textId)
                }
            }
            
            onTextRemoved(removedText)
            Log.d(TAG, "删除文字: ${removedText.content}")
        }
    }
    
    override fun clear() {
        textInfos.clear()
        canvasRenderer.clear()
        sdfRenderer.clear()
        complexRenderer.clear()
        pendingTexts.clear()
        migrationTimer?.cancel()
        
        // 重置统计
        simpleTextCount = 0
        complexTextCount = 0
        
        onAllTextsCleared()
        Log.d(TAG, "清空所有文字")
    }
    
    override fun getPerformanceStats(): PerformanceStats {
        val canvasStats = canvasRenderer.getPerformanceStats()
        val sdfStats = sdfRenderer.getPerformanceStats()
        val complexStats = complexRenderer.getPerformanceStats()
        
        return PerformanceStats(
            renderTime = lastRenderTime,
            memoryUsage = canvasStats.memoryUsage + sdfStats.memoryUsage + complexStats.memoryUsage,
            textCount = getTextCount(),
            complexTextCount = complexTextCount
        )
    }
    
    override fun release() {
        migrationTimer?.cancel()
        performanceMonitor.stop()
        
        canvasRenderer.release()
        sdfRenderer.release()
        complexRenderer.release()
        
        pendingTexts.clear()
        isInitialized = false
        
        Log.d(TAG, "混合渲染器(升级版)已释放")
    }
    
    // 抽象方法实现
    override fun onTextAdded(textInfo: TextInfo) {
        Log.d(TAG, "添加文字: ${textInfo.content}, 类型: ${textInfo.displayType}, 策略: ${currentStrategy.javaClass.simpleName}")
    }
    
    override fun onTextsAdded(textInfos: List<TextInfo>) {
        Log.d(TAG, "批量添加文字: ${textInfos.size}个")
    }
    
    override fun onTextRemoved(textInfo: TextInfo) {
        // 在removeText中已处理
    }
    
    override fun onAllTextsCleared() {
        // 在clear中已处理
    }
    
    /**
     * 获取详细的渲染器状态
     */
    fun getRenderingStatus(): RenderingStatus {
        return when (currentStrategy) {
            is CanvasStrategy -> RenderingStatus(
                strategy = "Canvas",
                canvasTextCount = canvasRenderer.getTextCount(),
                sdfTextCount = 0,
                complexTextCount = complexRenderer.getTextCount(),
                pendingMigration = 0
            )
            is HybridStrategy -> RenderingStatus(
                strategy = "Hybrid",
                canvasTextCount = canvasRenderer.getTextCount(),
                sdfTextCount = sdfRenderer.getTextCount(),
                complexTextCount = complexRenderer.getTextCount(),
                pendingMigration = pendingTexts.size
            )
            is IntelligentHybridStrategy -> RenderingStatus(
                strategy = "IntelligentHybrid",
                canvasTextCount = canvasRenderer.getTextCount(),
                sdfTextCount = sdfRenderer.getTextCount(),
                complexTextCount = complexRenderer.getTextCount(),
                pendingMigration = 0
            )
        }
    }
    
    /**
     * 强制执行迁移（用于测试和调试）
     */
    fun forceMigration() {
        if (pendingTexts.isNotEmpty()) {
            performMigration()
        }
    }
    
    /**
     * 渲染策略接口
     */
    private sealed class RenderingStrategy
    private class CanvasStrategy : RenderingStrategy()
    private class HybridStrategy : RenderingStrategy()
    private class IntelligentHybridStrategy : RenderingStrategy() // 新增智能混合策略
}

/**
 * 混合渲染器配置 - 升级版
 */
data class HybridConfig(
    val sdfThreshold: Int = 50,           // SDF切换阈值
    val migrationDelay: Long = 3000L,     // 迁移延迟(毫秒)
    val batchSize: Int = 10,              // 批处理大小
    val complexTextThreshold: Int = 20    // 复杂文字切换阈值
)

/**
 * 渲染器状态信息 - 升级版
 */
data class RenderingStatus(
    val strategy: String,              // 当前策略
    val canvasTextCount: Int,          // Canvas文字数量
    val sdfTextCount: Int,             // SDF文字数量
    val complexTextCount: Int = 0,     // 复杂文字数量
    val pendingMigration: Int          // 待迁移数量
)

/**
 * 性能监控器
 */
private class PerformanceMonitor {
    private val renderTimes = mutableListOf<Float>()
    private val maxSamples = 30 // 保留最近30次记录
    
    fun start() {
        renderTimes.clear()
    }
    
    fun recordRenderTime(time: Float) {
        renderTimes.add(time)
        if (renderTimes.size > maxSamples) {
            renderTimes.removeAt(0)
        }
    }
    
    fun getAverageRenderTime(): Float {
        return if (renderTimes.isNotEmpty()) {
            renderTimes.average().toFloat()
        } else 0f
    }
    
    fun stop() {
        renderTimes.clear()
    }
} 