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
    private val spriteBatch = SpriteBatchRenderer()
    private val arrowRenderer = ArrowRenderer()
    private val iconAtlasCache = IconAtlasCache(context)
    private val bubbleCache = BubbleNineSliceCache(context)
    
    // 配置参数
    private var sdfThreshold = DEFAULT_SDF_THRESHOLD
    private var migrationDelay = DEFAULT_MIGRATION_DELAY
    private var batchSize = DEFAULT_BATCH_SIZE
    private var complexTextThreshold = DEFAULT_COMPLEX_TEXT_THRESHOLD
    private var limitComplexPerScreenOverride: Int? = null
    private var layoutParams: ScreenLayoutUtils.LayoutParams = ScreenLayoutUtils.LayoutParams()
    private var gridSizePx: Int = 64
    private var maxPerGrid: Int = 1
    private var collisionWidthPx: Float = 96f
    private var collisionHeightPx: Float = 32f
    
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
        this.limitComplexPerScreenOverride = config.limitComplexPerScreen
        // 降级策略配置
        this.degradeAllow = config.allowDegrade
        this.degradeOrder = config.degradeOrder
        this.shortTextMaxWidthPx = config.shortTextMaxWidthPx
        this.layoutParams = ScreenLayoutUtils.LayoutParams(
            iconTextGapDp = config.iconTextGapDp,
            paddingDp = ScreenLayoutUtils.PaddingPx(
                config.paddingLeftDp, config.paddingTopDp, config.paddingRightDp, config.paddingBottomDp
            ),
            arrow = ScreenLayoutUtils.ArrowConfig(
                widthDp = config.arrowWidthDp, heightDp = config.arrowHeightDp, autoFlip = config.arrowAutoFlip
            )
        )
        // 屏幕网格限流与碰撞参数（dp -> px）
        this.gridSizePx = ScreenLayoutUtils.dpToPx(context, config.gridSizeDp).toInt().coerceAtLeast(8)
        this.maxPerGrid = config.maxPerGrid.coerceAtLeast(1)
        this.collisionWidthPx = ScreenLayoutUtils.dpToPx(context, config.collisionWidthDp)
        this.collisionHeightPx = ScreenLayoutUtils.dpToPx(context, config.collisionHeightDp)
        
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
        spriteBatch.initialize()
        arrowRenderer.initialize()
        
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
                spriteBatch.initialize()
                arrowRenderer.initialize()
                
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
                // 智能混合模式：分层渲染（屏幕空间布局 + 限流）
                val visible = planVisibleTexts(mvpMatrix)
                val (bubbleLayer, arrowLayer, iconLayer, textLayerSdf, textLayerCanvas) = buildLayers(visible, mvpMatrix)

                // 先气泡主体（九宫格纹理批绘）
                spriteBatch.begin(mvpMatrix)
                bubbleLayer.forEach { quad ->
                    spriteBatch.draw(quad.textureId, quad.verticesNdc, quad.uvs)
                }
                spriteBatch.end()

                // 箭头（几何）
                arrowLayer.forEach { tri ->
                    arrowRenderer.draw(mvpMatrix, tri.rgba, tri.verticesNdc)
                }

                // 再图标（图集批绘）
                spriteBatch.begin(mvpMatrix)
                iconLayer.forEach { quad ->
                    spriteBatch.draw(quad.textureId, quad.verticesNdc, quad.uvs)
                }
                spriteBatch.end()

                // 最后文字：优先 SDF，其次 Canvas 兜底
                sdfRenderer.clear(); if (textLayerSdf.isNotEmpty()) sdfRenderer.addTexts(textLayerSdf)
                canvasRenderer.clear(); if (textLayerCanvas.isNotEmpty()) canvasRenderer.addTexts(textLayerCanvas)
                sdfRenderer.render(mvpMatrix)
                canvasRenderer.render(mvpMatrix)
            }
        }
        
        val renderTime = (System.currentTimeMillis() - startTime).toFloat()
        lastRenderTime = renderTime
        performanceMonitor.recordRenderTime(renderTime)
    }

    // ==================== 分层/限流/布局 ====================

    private data class QuadItem(val textureId: Int, val verticesNdc: FloatArray, val uvs: FloatArray)
    private data class TriItem(val verticesNdc: FloatArray, val rgba: FloatArray)

    private data class VisiblePlan(
        val items: List<TextInfo>,
        val maxComplexOnScreen: Int,
        val arrowAutoFlip: Boolean,
        val drawShadow: Boolean,
        val drawBorder: Boolean
    )

    private fun planVisibleTexts(mvpMatrix: FloatArray): VisiblePlan {
        // 最小改动：暂用全部（可替换为视锥/网格/碰撞实现）。阈值参数可根据设备等级调整。
        val device = TextRenderingFactory.getDevicePerformanceLevel(context)
        val (maxComplexDefault, doShadow, doBorder) = when (device) {
            DevicePerformanceLevel.HIGH -> Triple(400, true, true)
            DevicePerformanceLevel.MEDIUM -> Triple(200, true, true)
            DevicePerformanceLevel.LOW -> Triple(100, false, true)
        }
        val maxComplex = limitComplexPerScreenOverride ?: maxComplexDefault
        val arrowAuto = layoutParams.arrow.autoFlip
        val items = textInfos.values.toList()
        return VisiblePlan(
            items = applyScreenGridAndCollision(items, mvpMatrix),
            maxComplexOnScreen = maxComplex,
            arrowAutoFlip = arrowAuto,
            drawShadow = doShadow,
            drawBorder = doBorder
        )
    }

    // 屏幕网格限流 + 标签碰撞规避（像素空间）
    private fun applyScreenGridAndCollision(items: List<TextInfo>, mvpMatrix: FloatArray): List<TextInfo> {
        val (vw, vh) = ScreenLayoutUtils.getViewportSize(context)
        val gridSize = gridSizePx
        val cols = (vw + gridSize - 1) / gridSize
        val rows = (vh + gridSize - 1) / gridSize

        // 计算真实布局矩形
        val layouts = items.mapNotNull { info ->
            val rect = computeLayoutRectPx(info, mvpMatrix) ?: return@mapNotNull null
            Triple(info, rect, Pair(rect.centerX(), rect.centerY()))
        }

        // 1) 网格限流
        val gridBuckets = HashMap<Int, MutableList<Triple<TextInfo, android.graphics.RectF, Pair<Float, Float>>>>()
        layouts.forEach { triple ->
            val center = triple.third
            val gx = (center.first / gridSize).toInt().coerceIn(0, cols - 1)
            val gy = (center.second / gridSize).toInt().coerceIn(0, rows - 1)
            val key = gy * cols + gx
            gridBuckets.getOrPut(key) { mutableListOf() }.add(triple)
        }
        val gridFiltered = gridBuckets.values.flatMap { bucket -> bucket.take(maxPerGrid) }

        // 2) 碰撞规避：AABB 真正布局
        val taken = mutableListOf<TextInfo>()
        val occupied = mutableListOf<android.graphics.RectF>()
        gridFiltered.forEach { (info, rect, _) ->
            val collides = occupied.any { android.graphics.RectF.intersects(it, rect) }
            if (!collides) {
                occupied.add(rect)
                taken.add(info)
            }
        }
        return taken
    }

    private fun computeLayoutRectPx(info: TextInfo, mvpMatrix: FloatArray): android.graphics.RectF? {
        val iconW = (info.iconInfo?.width ?: 0).toFloat()
        val iconH = (info.iconInfo?.height ?: 0).toFloat()
        val arrowFlip = info.bubbleInfo?.arrowAutoFlip ?: layoutParams.arrow.autoFlip
        val layout = ScreenLayoutUtils.layoutBubbleIconText(
            context,
            info,
            iconW,
            iconH,
            layoutParams.copy(arrow = layoutParams.arrow.copy(autoFlip = arrowFlip)),
            measurer = ::measureTextUsingSdfWrapped
        ) { x, y, z -> worldToScreenPx(x, y, z, mvpMatrix) }

        // 优先：有气泡则用气泡主体矩形
        if (info.bubbleInfo != null) {
            val r = layout.bubbleRectPx
            return android.graphics.RectF(r.left, r.top, r.right, r.bottom)
        }

        // 其次：无气泡，取图标与文本合并包围盒
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY

        layout.iconRectPx?.let { ir ->
            left = kotlin.math.min(left, ir.left)
            top = kotlin.math.min(top, ir.top)
            right = kotlin.math.max(right, ir.right)
            bottom = kotlin.math.max(bottom, ir.bottom)
        }

        // 文本近似矩形（以 textOriginPx 为左上基线，使用估计宽高）
        val (tw, th) = ScreenLayoutUtils.estimateTextSizePx(context, info)
        val tx = layout.textOriginPx.first
        val ty = layout.textOriginPx.second
        val textRect = android.graphics.RectF(tx, ty - th, tx + tw, ty)
        left = kotlin.math.min(left, textRect.left)
        top = kotlin.math.min(top, textRect.top)
        right = kotlin.math.max(right, textRect.right)
        bottom = kotlin.math.max(bottom, textRect.bottom)

        if (left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()) {
            return android.graphics.RectF(left, top, right, bottom)
        }

        // 兜底：使用碰撞估算尺寸
        val (vw, vh) = ScreenLayoutUtils.getViewportSize(context)
        val cx = vw * 0.5f; val cy = vh * 0.5f
        return android.graphics.RectF(
            cx - collisionWidthPx / 2f, cy - collisionHeightPx / 2f, cx + collisionWidthPx / 2f, cy + collisionHeightPx / 2f
        )
    }

    private fun measureTextUsingSdfWrapped(context: android.content.Context, info: TextInfo): Pair<Float, Float> {
        if (info.content.isEmpty()) return 0f to 0f
        val fontPx = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_SP,
            info.style.fontSize,
            context.resources.displayMetrics
        )
        val lineHeight = (fontPx * info.style.lineSpacing).coerceAtLeast(fontPx)
        val avgCharWidth = fontPx * 0.6f

        val maxWidthPx = info.style.maxWidth
        if (maxWidthPx <= 0f) {
            val width = info.content.length.coerceAtLeast(1) * avgCharWidth
            return width to lineHeight
        }

        var currentLineWidth = 0f
        var maxLineWidth = 0f
        var lines = 1
        info.content.forEach { ch ->
            val w = when (ch) {
                '\n' -> {
                    maxLineWidth = kotlin.math.max(maxLineWidth, currentLineWidth)
                    currentLineWidth = 0f
                    lines += 1
                    0f
                }
                else -> avgCharWidth
            }
            if (w == 0f) return@forEach
            if (currentLineWidth + w > maxWidthPx) {
                maxLineWidth = kotlin.math.max(maxLineWidth, currentLineWidth)
                currentLineWidth = w
                lines += 1
            } else {
                currentLineWidth += w
            }
        }
        maxLineWidth = kotlin.math.max(maxLineWidth, currentLineWidth)
        val totalHeight = lines * lineHeight
        return maxLineWidth to totalHeight
    }

    private fun buildLayers(plan: VisiblePlan, mvpMatrix: FloatArray): Quintuple<List<QuadItem>, List<TriItem>, List<QuadItem>, List<TextInfo>, List<TextInfo>> {
        val bubbleQuads = mutableListOf<QuadItem>()
        val arrowTris = mutableListOf<TriItem>()
        val iconQuads = mutableListOf<QuadItem>()
        val sdfTexts = mutableListOf<TextInfo>()
        val canvasTexts = mutableListOf<TextInfo>()

        var complexOnScreen = 0

        // 确保图标图集（一次性）
        if (iconAtlasCache.getTextureId() == 0) {
            val uniqueRes = plan.items.mapNotNull { it.iconInfo?.resourceId }.distinct()
            if (uniqueRes.isNotEmpty()) {
                val bitmaps = uniqueRes.mapNotNull { resId ->
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
                        if (bmp != null) Pair(bmp, resId) else null
                    } catch (_: Exception) { null }
                }
                if (bitmaps.isNotEmpty()) {
                    iconAtlasCache.ensureAtlas(bitmaps)
                }
            }
        }

        plan.items.forEach { info ->
            val isComplex = info.displayType == TextDisplayType.BUBBLE_TEXT_WITH_ICON || info.displayType == TextDisplayType.BUBBLE_TEXT_ONLY
            if (isComplex && complexOnScreen >= plan.maxComplexOnScreen) {
                // 超出复杂上限：执行可配置降级策略
                if (!degradeAllow) {
                    // 禁止降级：仍绘制完整气泡
                    complexOnScreen++
                    val layout = ScreenLayoutUtils.layoutBubbleIconText(
                        context,
                        info,
                        (info.iconInfo?.width ?: 0).toFloat(),
                        (info.iconInfo?.height ?: 0).toFloat(),
                        layoutParams.copy(arrow = layoutParams.arrow.copy(autoFlip = info.bubbleInfo?.arrowAutoFlip ?: plan.arrowAutoFlip)),
                        measurer = ::measureTextUsingSdfWrapped
                    ) { x, y, z -> worldToScreenPx(x, y, z, mvpMatrix) }

                    addBubbleIconAndArrow(info, layout, plan, bubbleQuads, arrowTris, iconQuads)
                } else {
                    var degraded = false
                    for (step in degradeOrder) {
                        when (step) {
                            DegradeStep.ICON_ONLY -> {
                                val layout = ScreenLayoutUtils.layoutBubbleIconText(
                                    context, info,
                                    (info.iconInfo?.width ?: 0).toFloat(), (info.iconInfo?.height ?: 0).toFloat(),
                                    layoutParams.copy(arrow = layoutParams.arrow.copy(autoFlip = false)),
                                    measurer = ::measureTextUsingSdfWrapped
                                ) { x, y, z -> worldToScreenPx(x, y, z, mvpMatrix) }
                                val icon = info.iconInfo
                                if (icon != null && layout.iconRectPx != null) {
                                    val entry = iconAtlasCache.getEntryWithTex(icon.resourceId)
                                    if (entry != null) {
                                        val (verts, uvs) = ndcQuadFromPx(layout.iconRectPx, ScreenLayoutUtils.getViewportSize(context), IconAtlasCache.IconEntry(entry.u1, entry.v1, entry.u2, entry.v2))
                                        iconQuads.add(QuadItem(entry.textureId, verts, uvs))
                                    }
                                }
                                degraded = true
                                break
                            }
                            DegradeStep.ICON_WITH_SHORT_TEXT -> {
                                val shortened = shortenTextByWidth(info, shortTextMaxWidthPx)
                                val layout = ScreenLayoutUtils.layoutBubbleIconText(
                                    context, shortened,
                                    (shortened.iconInfo?.width ?: 0).toFloat(), (shortened.iconInfo?.height ?: 0).toFloat(),
                                    layoutParams.copy(arrow = layoutParams.arrow.copy(autoFlip = false)),
                                    measurer = ::measureTextUsingSdfWrapped
                                ) { x, y, z -> worldToScreenPx(x, y, z, mvpMatrix) }
                                val icon = shortened.iconInfo
                                if (icon != null && layout.iconRectPx != null) {
                                    val entry = iconAtlasCache.getEntryWithTex(icon.resourceId)
                                    if (entry != null) {
                                        val (verts, uvs) = ndcQuadFromPx(layout.iconRectPx, ScreenLayoutUtils.getViewportSize(context), IconAtlasCache.IconEntry(entry.u1, entry.v1, entry.u2, entry.v2))
                                        iconQuads.add(QuadItem(entry.textureId, verts, uvs))
                                    }
                                }
                                if (sdfRenderer.supportsText(shortened.content)) sdfTexts.add(shortened) else canvasTexts.add(shortened)
                                degraded = true
                                break
                            }
                            DegradeStep.FULL_BUBBLE -> {
                                complexOnScreen++
                                val layout = ScreenLayoutUtils.layoutBubbleIconText(
                                    context,
                                    info,
                                    (info.iconInfo?.width ?: 0).toFloat(), (info.iconInfo?.height ?: 0).toFloat(),
                                    layoutParams.copy(arrow = layoutParams.arrow.copy(autoFlip = info.bubbleInfo?.arrowAutoFlip ?: plan.arrowAutoFlip)),
                                    measurer = ::measureTextUsingSdfWrapped
                                ) { x, y, z -> worldToScreenPx(x, y, z, mvpMatrix) }
                                addBubbleIconAndArrow(info, layout, plan, bubbleQuads, arrowTris, iconQuads)
                                degraded = true
                                break
                            }
                        }
                    }
                    if (!degraded) {
                        val icon = info.iconInfo
                        if (icon != null) {
                            val entry = iconAtlasCache.getEntryWithTex(icon.resourceId)
                            entry?.let {
                                val rect = ScreenLayoutUtils.PxRect(0f, 0f, icon.width.toFloat(), icon.height.toFloat())
                                val (verts, uvs) = ndcQuadFromPx(rect, ScreenLayoutUtils.getViewportSize(context), IconAtlasCache.IconEntry(it.u1, it.v1, it.u2, it.v2))
                                iconQuads.add(QuadItem(it.textureId, verts, uvs))
                            }
                        }
                    }
                }
            } else {
                if (isComplex) complexOnScreen++
                val layout = ScreenLayoutUtils.layoutBubbleIconText(
                    context,
                    info,
                    (info.iconInfo?.width ?: 0).toFloat(),
                    (info.iconInfo?.height ?: 0).toFloat(),
                    // 单项优先生效：若 info.bubbleInfo.arrowAutoFlip != null 则覆盖全局
                    layoutParams.copy(arrow = layoutParams.arrow.copy(autoFlip = info.bubbleInfo?.arrowAutoFlip ?: plan.arrowAutoFlip)),
                    measurer = ::measureTextUsingSdfWrapped
                ) { x, y, z -> worldToScreenPx(x, y, z, mvpMatrix) }

                addBubbleIconAndArrow(info, layout, plan, bubbleQuads, arrowTris, iconQuads)
            }

            // 文本分发：优先 SDF
            if (sdfRenderer.supportsText(info.content)) sdfTexts.add(info) else canvasTexts.add(info)
        }

        return Quintuple(bubbleQuads, arrowTris, iconQuads, sdfTexts, canvasTexts)
    }

    // ============== 降级策略支持 ==============

    enum class DegradeStep { ICON_ONLY, ICON_WITH_SHORT_TEXT, FULL_BUBBLE }

    private var degradeAllow: Boolean = true
    private var degradeOrder: List<DegradeStep> = listOf(DegradeStep.ICON_ONLY, DegradeStep.ICON_WITH_SHORT_TEXT, DegradeStep.FULL_BUBBLE)
    private var shortTextMaxWidthPx: Float = 80f

    private fun shortenTextByWidth(info: TextInfo, maxWidthPx: Float): TextInfo {
        if (maxWidthPx <= 0f) return info
        val ctx = context
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.textSize = info.style.fontSize * ctx.resources.displayMetrics.scaledDensity
        var text = info.content
        var measured = paint.measureText(text)
        if (measured <= maxWidthPx) return info
        while (text.isNotEmpty() && measured > maxWidthPx) {
            text = text.dropLast(1)
            measured = paint.measureText(text + "…")
        }
        val newInfo = info.copy(content = if (text.isEmpty()) info.content.take(1) else text + "…")
        return newInfo
    }

    private fun addBubbleIconAndArrow(
        info: TextInfo,
        layout: ScreenLayoutUtils.BubbleLayout,
        plan: VisiblePlan,
        bubbleQuads: MutableList<QuadItem>,
        arrowTris: MutableList<TriItem>,
        iconQuads: MutableList<QuadItem>
    ) {
        val style = info.bubbleInfo?.style
        if (style != null) {
            val borderPx = if (plan.drawBorder) 1 else 0
            val shadowColor = if (plan.drawShadow) style.shadowColor else null
            val shadowRadiusPx = if (plan.drawShadow) style.shadowRadius.toInt() else 0
            val bubbleW = layout.bubbleRectPx.width.toInt().coerceAtLeast(16)
            val bubbleH = layout.bubbleRectPx.height.toInt().coerceAtLeast(16)
            val nine = bubbleCache.getOrCreate(
                bgColor = style.backgroundColor,
                borderColor = style.borderColor,
                borderWidthPx = borderPx,
                shadowColor = shadowColor,
                shadowRadiusPx = shadowRadiusPx,
                cornerPx = info.bubbleInfo?.cornerRadius?.toInt() ?: 8,
                bucketW = quantizeToBucket(bubbleW),
                bucketH = quantizeToBucket(bubbleH)
            )
            val (quadVerts, quadUvs) = ndcQuadFromPx(layout.bubbleRectPx, ScreenLayoutUtils.getViewportSize(context), nine)
            bubbleQuads.add(QuadItem(nine.textureId, quadVerts, quadUvs))
        }

        val triVerts = ndcTriangleFromPx(layout.arrowApexPx, layout.arrowBaseLeftPx, layout.arrowBaseRightPx, ScreenLayoutUtils.getViewportSize(context))
        val rgba = floatArrayOf(0f, 0f, 0f, 0.3f)
        arrowTris.add(TriItem(triVerts, rgba))

        val icon = info.iconInfo
        if (icon != null && layout.iconRectPx != null) {
            val entry = iconAtlasCache.getEntryWithTex(icon.resourceId)
            if (entry != null) {
                val (verts, uvs) = ndcQuadFromPx(layout.iconRectPx, ScreenLayoutUtils.getViewportSize(context), IconAtlasCache.IconEntry(entry.u1, entry.v1, entry.u2, entry.v2))
                iconQuads.add(QuadItem(entry.textureId, verts, uvs))
            }
        }
    }

    private fun quantizeToBucket(value: Int, step: Int = 8): Int {
        val r = value % step
        return if (r == 0) value else value + (step - r)
    }

    private fun ndcQuadFromPx(rect: ScreenLayoutUtils.PxRect, viewport: Pair<Int, Int>, uv: Any): Pair<FloatArray, FloatArray> {
        val (vw, vh) = viewport
        val x1 = rect.left / vw * 2f - 1f
        val y1 = 1f - rect.top / vh * 2f
        val x2 = rect.right / vw * 2f - 1f
        val y2 = 1f - rect.bottom / vh * 2f
        val verts = floatArrayOf(
            x1, y1, 0f,
            x1, y2, 0f,
            x2, y2, 0f,
            x2, y1, 0f
        )
        val uvs = when (uv) {
            is BubbleNineSliceCache.NineSlice -> floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
            is IconAtlasCache.IconEntry -> floatArrayOf(uv.u1, uv.v1, uv.u1, uv.v2, uv.u2, uv.v2, uv.u2, uv.v1)
            else -> floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
        }
        return Pair(verts, uvs)
    }

    private fun ndcTriangleFromPx(
        apex: Pair<Float, Float>, baseL: Pair<Float, Float>, baseR: Pair<Float, Float>, viewport: Pair<Int, Int>
    ): FloatArray {
        fun toNdc(p: Pair<Float, Float>, vw: Int, vh: Int): Pair<Float, Float> {
            val x = p.first / vw * 2f - 1f
            val y = 1f - p.second / vh * 2f
            return Pair(x, y)
        }
        val (vw, vh) = viewport
        val a = toNdc(apex, vw, vh)
        val bl = toNdc(baseL, vw, vh)
        val br = toNdc(baseR, vw, vh)
        return floatArrayOf(a.first, a.second, 0f, bl.first, bl.second, 0f, br.first, br.second, 0f)
    }

    private fun worldToScreenPx(x: Double, y: Double, z: Double, mvp: FloatArray): Pair<Float, Float> {
        // world -> NDC
        val ndc = worldToNdc(x.toFloat(), y.toFloat(), z.toFloat(), mvp)
        // NDC -> px
        val (vw, vh) = ScreenLayoutUtils.getViewportSize(context)
        val px = ((ndc.first + 1f) * 0.5f) * vw
        val py = ((1f - ndc.second) * 0.5f) * vh
        return Pair(px, py)
    }

    private fun worldToNdc(x: Float, y: Float, z: Float, mvp: FloatArray): Pair<Float, Float> {
        val vx = x
        val vy = y
        val vz = z
        val vw = 1f
        val nx = mvp[0] * vx + mvp[4] * vy + mvp[8] * vz + mvp[12] * vw
        val ny = mvp[1] * vx + mvp[5] * vy + mvp[9] * vz + mvp[13] * vw
        val nz = mvp[2] * vx + mvp[6] * vy + mvp[10] * vz + mvp[14] * vw
        val nw = mvp[3] * vx + mvp[7] * vy + mvp[11] * vz + mvp[15] * vw
        val invW = if (nw != 0f) 1f / nw else 1f
        return Pair(nx * invW, ny * invW)
    }

    // 简单的五元组数据承载
    private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
    
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
    val complexTextThreshold: Int = 20,   // 复杂文字切换阈值
    // 降级策略
    val allowDegrade: Boolean = true,
    val degradeOrder: List<HybridTextRenderer.DegradeStep> = listOf(HybridTextRenderer.DegradeStep.ICON_ONLY, HybridTextRenderer.DegradeStep.ICON_WITH_SHORT_TEXT, HybridTextRenderer.DegradeStep.FULL_BUBBLE),
    val shortTextMaxWidthPx: Float = 80f,
    // 屏幕限流 & 布局参数（可调）
    val limitComplexPerScreen: Int? = null,
    val iconTextGapDp: Float = 4f,
    val paddingLeftDp: Float = 8f,
    val paddingTopDp: Float = 6f,
    val paddingRightDp: Float = 8f,
    val paddingBottomDp: Float = 6f,
    val arrowWidthDp: Float = 12f,
    val arrowHeightDp: Float = 6f,
    val arrowAutoFlip: Boolean = true,
    // 网格与碰撞参数
    val gridSizeDp: Float = 64f,
    val maxPerGrid: Int = 1,
    val collisionWidthDp: Float = 96f,
    val collisionHeightDp: Float = 32f
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