package com.houtrry.common_map.text

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.houtrry.common_map.data.*

/**
 * 文字渲染工厂类 - 升级版
 * 提供便捷的API来创建和管理文字渲染器，支持复杂文字显示
 */
object TextRenderingFactory {
    
    private const val TAG = "TextRenderingFactory"
    
    /**
     * 创建推荐的文字渲染器（智能混合方案）
     * @param context Android上下文
     * @param scenario 渲染场景
     * @param config 可选配置
     * @return HybridTextRenderer实例
     */
    fun createRecommendedRenderer(
        context: Context,
        scenario: RenderingScenario = RenderingScenario.NewMapScenario,
        config: HybridConfig = HybridConfig()
    ): HybridTextRenderer {
        Log.d(TAG, "创建推荐的智能混合文字渲染器")
        return HybridTextRenderer(context).apply {
            initialize(scenario, config)
        }
    }
    
    /**
     * 创建Canvas文字渲染器
     * @param context Android上下文
     * @return CanvasTextRenderer实例
     */
    fun createCanvasRenderer(context: Context): CanvasTextRenderer {
        Log.d(TAG, "创建Canvas文字渲染器")
        return CanvasTextRenderer(context).apply {
            initialize()
        }
    }
    
    /**
     * 创建SDF文字渲染器
     * @param context Android上下文
     * @return SDFTextRenderer实例
     */
    fun createSDFRenderer(context: Context): SDFTextRenderer {
        Log.d(TAG, "创建SDF文字渲染器")
        return SDFTextRenderer(context).apply {
            initialize()
        }
    }
    
    /**
     * 创建复杂文字渲染器
     * @param context Android上下文
     * @return ComplexTextRenderer实例
     */
    fun createComplexRenderer(context: Context): ComplexTextRenderer {
        Log.d(TAG, "创建复杂文字渲染器")
        return ComplexTextRenderer(context).apply {
            initialize()
        }
    }
    
    /**
     * 根据项目需求自动选择最适合的渲染器
     * @param context Android上下文
     * @param requirements 项目需求
     * @return 最适合的渲染器实例
     */
    fun createAutoRenderer(
        context: Context, 
        requirements: ProjectRequirements
    ): ITextRenderer {
        Log.d(TAG, "根据需求自动选择渲染器: $requirements")
        
        return when {
            // 只有复杂文字 -> 复杂渲染器
            requirements.hasComplexDisplay && !requirements.hasSimpleText -> {
                Log.d(TAG, "选择复杂文字渲染器（纯复杂显示）")
                createComplexRenderer(context)
            }
            
            // 大量简单文字且很少变化 -> 纯SDF
            requirements.maxTextCount > 1000 && !requirements.frequentChanges && !requirements.hasComplexDisplay -> {
                Log.d(TAG, "选择SDF渲染器（大量静态简单文字）")
                createSDFRenderer(context)
            }
            
            // 少量简单文字且经常变化 -> 纯Canvas
            requirements.maxTextCount < 50 && requirements.frequentChanges && !requirements.hasComplexDisplay -> {
                Log.d(TAG, "选择Canvas渲染器（少量动态简单文字）")
                createCanvasRenderer(context)
            }
            
            // 其他情况 -> 智能混合方案
            else -> {
                Log.d(TAG, "选择智能混合渲染器（平衡性能和灵活性）")
                val scenario = if (requirements.hasExistingTexts) {
                    RenderingScenario.ExtendMapScenario(requirements.existingTexts ?: emptyList())
                } else {
                    RenderingScenario.NewMapScenario
                }
                
                val config = HybridConfig(
                    sdfThreshold = requirements.sdfSwitchThreshold ?: 50,
                    migrationDelay = 3000L,
                    batchSize = 10,
                    complexTextThreshold = requirements.complexTextThreshold ?: 20
                )
                
                createRecommendedRenderer(context, scenario, config)
            }
        }
    }
}

/**
 * 项目需求描述 - 升级版
 * @param maxTextCount 预期最大文字数量
 * @param frequentChanges 是否经常增删文字
 * @param hasExistingTexts 是否已有文字需要显示
 * @param existingTexts 已存在的文字列表
 * @param hasComplexDisplay 是否包含复杂显示（图标、气泡等）
 * @param hasSimpleText 是否包含简单文字
 * @param sdfSwitchThreshold SDF切换阈值（可选）
 * @param complexTextThreshold 复杂文字切换阈值（可选）
 */
data class ProjectRequirements(
    val maxTextCount: Int,                         // 预期最大文字数量
    val frequentChanges: Boolean = false,          // 是否经常增删文字
    val hasExistingTexts: Boolean = false,         // 是否已有文字需要显示
    val existingTexts: List<String>? = null,       // 已存在的文字列表
    val hasComplexDisplay: Boolean = false,        // 是否包含复杂显示
    val hasSimpleText: Boolean = true,             // 是否包含简单文字
    val sdfSwitchThreshold: Int? = null,           // SDF切换阈值（可选）
    val complexTextThreshold: Int? = null          // 复杂文字切换阈值（可选）
)

/**
 * 文字渲染工具类 - 升级版
 * 提供常用的工具方法，支持复杂文字类型
 */
object TextRenderingUtils {
    
    /**
     * 创建简单的文字信息
     * @param content 文字内容
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标（可选，默认0）
     * @param style 文字样式（可选）
     * @return TextInfo实例
     */
    fun createTextInfo(
        content: String,
        x: Double,
        y: Double,
        z: Double = 0.0,
        style: TextStyle = TextStyle()
    ): TextInfo {
        return TextInfo(
            content = content,
            position = Vector3(x, y, z),
            style = style,
            displayType = TextDisplayType.PURE_TEXT
        )
    }
    
    /**
     * 创建支持换行的文字信息
     * @param content 文字内容
     * @param x X坐标
     * @param y Y坐标
     * @param maxWidth 最大宽度（像素）
     * @param z Z坐标（可选，默认0）
     * @param style 基础文字样式（可选）
     * @return TextInfo实例
     */
    fun createWrappedTextInfo(
        content: String,
        x: Double,
        y: Double,
        maxWidth: Float,
        z: Double = 0.0,
        style: TextStyle = TextStyle()
    ): TextInfo {
        val wrappedStyle = style.copy(maxWidth = maxWidth)
        return TextInfo(
            content = content,
            position = Vector3(x, y, z),
            style = wrappedStyle,
            displayType = TextDisplayType.PURE_TEXT
        )
    }
    
    /**
     * 创建RTL文字信息
     * @param content 文字内容（阿拉伯语、希伯来语等）
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标（可选，默认0）
     * @param style 基础文字样式（可选）
     * @param forceRTL 是否强制RTL（默认自动检测）
     * @return TextInfo实例
     */
    fun createRTLTextInfo(
        content: String,
        x: Double,
        y: Double,
        z: Double = 0.0,
        style: TextStyle = TextStyle(),
        forceRTL: Boolean = false
    ): TextInfo {
        val rtlStyle = if (forceRTL) {
            style.copy(
                textDirection = TextDirection.RTL,
                textAlign = if (style.textAlign == TextAlign.START) TextAlign.START else TextAlign.RIGHT
            )
        } else {
            style.copy(textDirection = TextDirection.AUTO)
        }
        
        return TextInfo(
            content = content,
            position = Vector3(x, y, z),
            style = rtlStyle,
            displayType = TextDisplayType.PURE_TEXT
        )
    }
    
    /**
     * 创建多行气泡文字信息
     * @param content 文字内容
     * @param x X坐标
     * @param y Y坐标
     * @param maxWidth 最大宽度
     * @param bubbleStyle 气泡样式（可选）
     * @param z Z坐标（可选，默认0）
     * @param style 文字样式（可选）
     * @return TextInfo实例
     */
    fun createMultiLineBubbleTextInfo(
        content: String,
        x: Double,
        y: Double,
        maxWidth: Float,
        bubbleStyle: BubbleStyle = BubbleStyle(),
        z: Double = 0.0,
        style: TextStyle = TextStyle()
    ): TextInfo {
        val wrappedStyle = style.copy(maxWidth = maxWidth)
        return TextInfo(
            content = content,
            position = Vector3(x, y, z),
            style = wrappedStyle,
            displayType = TextDisplayType.BUBBLE_TEXT_ONLY,
            bubbleInfo = BubbleInfo(style = bubbleStyle)
        )
    }
    
    /**
     * 创建带图标的文字信息
     * @param content 文字内容
     * @param x X坐标
     * @param y Y坐标
     * @param iconResourceId 图标资源ID
     * @param iconPosition 图标位置
     * @param iconSize 图标尺寸（可选）
     * @param z Z坐标（可选，默认0）
     * @param style 文字样式（可选）
     * @return TextInfo实例
     */
    fun createIconTextInfo(
        content: String,
        x: Double,
        y: Double,
        iconResourceId: Int,
        iconPosition: IconPosition,
        iconSize: Pair<Int, Int> = Pair(24, 24),
        z: Double = 0.0,
        style: TextStyle = TextStyle()
    ): TextInfo {
        val displayType = when (iconPosition) {
            IconPosition.ABOVE -> TextDisplayType.ICON_ABOVE_TEXT
            IconPosition.BELOW -> TextDisplayType.ICON_BELOW_TEXT
            IconPosition.LEFT -> TextDisplayType.ICON_LEFT_TEXT
            IconPosition.RIGHT -> TextDisplayType.ICON_RIGHT_TEXT
        }
        
        return TextInfo(
            content = content,
            position = Vector3(x, y, z),
            style = style,
            displayType = displayType,
            iconInfo = IconInfo(
                resourceId = iconResourceId,
                width = iconSize.first,
                height = iconSize.second
            )
        )
    }
    
    /**
     * 创建气泡文字信息
     * @param content 文字内容
     * @param x X坐标
     * @param y Y坐标
     * @param bubbleStyle 气泡样式（可选）
     * @param z Z坐标（可选，默认0）
     * @param style 文字样式（可选）
     * @return TextInfo实例
     */
    fun createBubbleTextInfo(
        content: String,
        x: Double,
        y: Double,
        bubbleStyle: BubbleStyle = BubbleStyle(),
        z: Double = 0.0,
        style: TextStyle = TextStyle()
    ): TextInfo {
        return TextInfo(
            content = content,
            position = Vector3(x, y, z),
            style = style,
            displayType = TextDisplayType.BUBBLE_TEXT_ONLY,
            bubbleInfo = BubbleInfo(style = bubbleStyle)
        )
    }
    
    /**
     * 创建气泡+图标+文字信息
     * @param content 文字内容
     * @param x X坐标
     * @param y Y坐标
     * @param iconResourceId 图标资源ID
     * @param bubbleStyle 气泡样式（可选）
     * @param iconSize 图标尺寸（可选）
     * @param z Z坐标（可选，默认0）
     * @param style 文字样式（可选）
     * @return TextInfo实例
     */
    fun createBubbleIconTextInfo(
        content: String,
        x: Double,
        y: Double,
        iconResourceId: Int,
        bubbleStyle: BubbleStyle = BubbleStyle(),
        iconSize: Pair<Int, Int> = Pair(20, 20),
        z: Double = 0.0,
        style: TextStyle = TextStyle()
    ): TextInfo {
        return TextInfo(
            content = content,
            position = Vector3(x, y, z),
            style = style,
            displayType = TextDisplayType.BUBBLE_TEXT_WITH_ICON,
            iconInfo = IconInfo(
                resourceId = iconResourceId,
                width = iconSize.first,
                height = iconSize.second
            ),
            bubbleInfo = BubbleInfo(style = bubbleStyle)
        )
    }
    
    /**
     * 批量创建文字信息
     * @param texts 文字内容列表
     * @param positions 对应的位置列表
     * @param style 统一的文字样式（可选）
     * @return TextInfo列表
     */
    fun createTextInfos(
        texts: List<String>,
        positions: List<Vector3>,
        style: TextStyle = TextStyle()
    ): List<TextInfo> {
        require(texts.size == positions.size) { "文字数量必须与位置数量相等" }
        
        return texts.zip(positions) { content, position ->
            TextInfo(content = content, position = position, style = style)
        }
    }
    
    /**
     * 创建网格布局的文字信息
     * @param texts 文字内容列表
     * @param rows 行数
     * @param cols 列数
     * @param spacing 间距
     * @param style 文字样式（可选）
     * @return TextInfo列表
     */
    fun createGridTextInfos(
        texts: List<String>,
        rows: Int,
        cols: Int,
        spacing: Double = 1.0,
        style: TextStyle = TextStyle()
    ): List<TextInfo> {
        require(texts.size <= rows * cols) { "文字数量不能超过网格容量" }
        
        return texts.mapIndexed { index, content ->
            val row = index / cols
            val col = index % cols
            val x = col * spacing
            val y = row * spacing
            
            TextInfo(
                content = content,
                position = Vector3(x, y, 0.0),
                style = style
            )
        }
    }
    
    /**
     * 批量创建气泡文字网格
     * @param texts 文字内容列表
     * @param rows 行数
     * @param cols 列数
     * @param spacing 间距
     * @param bubbleStyle 气泡样式（可选）
     * @param style 文字样式（可选）
     * @return TextInfo列表
     */
    fun createBubbleGridTextInfos(
        texts: List<String>,
        rows: Int,
        cols: Int,
        spacing: Double = 2.0, // 气泡需要更大间距
        bubbleStyle: BubbleStyle = BubbleStyle(),
        style: TextStyle = TextStyle()
    ): List<TextInfo> {
        require(texts.size <= rows * cols) { "文字数量不能超过网格容量" }
        
        return texts.mapIndexed { index, content ->
            val row = index / cols
            val col = index % cols
            val x = col * spacing
            val y = row * spacing
            
            TextInfo(
                content = content,
                position = Vector3(x, y, 0.0),
                style = style,
                displayType = TextDisplayType.BUBBLE_TEXT_ONLY,
                bubbleInfo = BubbleInfo(style = bubbleStyle)
            )
        }
    }
    
    /**
     * 获取设备性能等级建议
     * @param context Android上下文
     * @return DevicePerformanceLevel
     */
    fun getDevicePerformanceLevel(context: Context): DevicePerformanceLevel {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemoryMB = memoryInfo.totalMem / 1024 / 1024
        
        return when {
            totalMemoryMB >= 6 * 1024 -> DevicePerformanceLevel.HIGH    // 6GB+
            totalMemoryMB >= 3 * 1024 -> DevicePerformanceLevel.MEDIUM  // 3GB+
            else -> DevicePerformanceLevel.LOW                           // <3GB
        }
    }
    
    /**
     * 根据设备性能推荐配置
     * @param context Android上下文
     * @return HybridConfig
     */
    fun getRecommendedConfig(context: Context): HybridConfig {
        return when (getDevicePerformanceLevel(context)) {
            DevicePerformanceLevel.HIGH -> HybridConfig(
                // 迁移/批处理
                sdfThreshold = 80,
                migrationDelay = 2000L,
                batchSize = 15,
                complexTextThreshold = 30,
                // 降级策略
                allowDegrade = true,
                degradeOrder = listOf(
                    HybridTextRenderer.DegradeStep.ICON_ONLY,
                    HybridTextRenderer.DegradeStep.ICON_WITH_SHORT_TEXT,
                    HybridTextRenderer.DegradeStep.FULL_BUBBLE
                ),
                shortTextMaxWidthPx = 96f,
                // 屏内复杂对象上限 & 布局/碰撞
                limitComplexPerScreen = 400,
                iconTextGapDp = 4f,
                paddingLeftDp = 8f,
                paddingTopDp = 6f,
                paddingRightDp = 8f,
                paddingBottomDp = 6f,
                arrowWidthDp = 12f,
                arrowHeightDp = 6f,
                arrowAutoFlip = true,
                gridSizeDp = 56f,
                maxPerGrid = 1,
                collisionWidthDp = 88f,
                collisionHeightDp = 28f
            )
            DevicePerformanceLevel.MEDIUM -> HybridConfig(
                sdfThreshold = 50,
                migrationDelay = 3000L,
                batchSize = 10,
                complexTextThreshold = 20,
                allowDegrade = true,
                degradeOrder = listOf(
                    HybridTextRenderer.DegradeStep.ICON_ONLY,
                    HybridTextRenderer.DegradeStep.ICON_WITH_SHORT_TEXT,
                    HybridTextRenderer.DegradeStep.FULL_BUBBLE
                ),
                shortTextMaxWidthPx = 80f,
                limitComplexPerScreen = 200,
                iconTextGapDp = 4f,
                paddingLeftDp = 8f,
                paddingTopDp = 6f,
                paddingRightDp = 8f,
                paddingBottomDp = 6f,
                arrowWidthDp = 12f,
                arrowHeightDp = 6f,
                arrowAutoFlip = true,
                gridSizeDp = 64f,
                maxPerGrid = 1,
                collisionWidthDp = 96f,
                collisionHeightDp = 32f
            )
            DevicePerformanceLevel.LOW -> HybridConfig(
                sdfThreshold = 30,
                migrationDelay = 4000L,
                batchSize = 5,
                complexTextThreshold = 10,
                allowDegrade = true,
                degradeOrder = listOf(
                    HybridTextRenderer.DegradeStep.ICON_ONLY,
                    HybridTextRenderer.DegradeStep.ICON_WITH_SHORT_TEXT,
                    HybridTextRenderer.DegradeStep.FULL_BUBBLE
                ),
                shortTextMaxWidthPx = 72f,
                limitComplexPerScreen = 100,
                iconTextGapDp = 4f,
                paddingLeftDp = 8f,
                paddingTopDp = 6f,
                paddingRightDp = 8f,
                paddingBottomDp = 6f,
                arrowWidthDp = 12f,
                arrowHeightDp = 6f,
                arrowAutoFlip = true,
                gridSizeDp = 72f,
                maxPerGrid = 1,
                collisionWidthDp = 112f,
                collisionHeightDp = 40f
            )
        }
    }
    
    /**
     * 分析文字内容的字符集
     * @param texts 文字列表
     * @return 去重后的字符集合
     */
    fun analyzeCharacterSet(texts: List<String>): Set<Char> {
        return texts.flatMap { it.toCharArray().asIterable() }.toSet()
    }
    
    /**
     * 分析文字显示复杂度
     * @param textInfos 文字信息列表
     * @return 复杂度分析结果
     */
    fun analyzeDisplayComplexity(textInfos: List<TextInfo>): DisplayComplexityAnalysis {
        val simpleTexts = textInfos.filter { !it.isComplexDisplay() }
        val complexTexts = textInfos.filter { it.isComplexDisplay() }
        val bubbleTexts = textInfos.filter { it.hasBubble() }
        val iconTexts = textInfos.filter { it.hasIcon() }
        
        return DisplayComplexityAnalysis(
            totalCount = textInfos.size,
            simpleTextCount = simpleTexts.size,
            complexTextCount = complexTexts.size,
            bubbleTextCount = bubbleTexts.size,
            iconTextCount = iconTexts.size,
            complexityRatio = if (textInfos.isNotEmpty()) complexTexts.size.toFloat() / textInfos.size else 0f
        )
    }
    
    /**
     * 估算内存使用量
     * @param textCount 文字数量
     * @param avgTextLength 平均文字长度
     * @param renderType 渲染类型
     * @param complexityRatio 复杂度比例（0-1）
     * @return 估算的内存使用量（字节）
     */
    fun estimateMemoryUsage(
        textCount: Int,
        avgTextLength: Int,
        renderType: TextRenderType,
        complexityRatio: Float = 0f
    ): Long {
        val baseUsage = when (renderType) {
            TextRenderType.CANVAS -> {
                // Canvas: 每个文字独立纹理，约16KB每个
                textCount * 16L * 1024
            }
            TextRenderType.SDF -> {
                // SDF: 共享图集，约64字符每个64x64像素
                val charCount = (textCount * avgTextLength * 1.2).toLong() // 考虑重复
                charCount * 64 * 64 / 64 // 64个字符共享一个图集
            }
            TextRenderType.TEMPORARY_CANVAS -> {
                // 临时Canvas，按Canvas计算但会迁移
                textCount * 8L * 1024 // 减半估算
            }
            TextRenderType.HYBRID_COMPONENT -> {
                // 混合组件：基础SDF + 复杂Canvas
                val sdfUsage = textCount * 4L * 1024 // SDF部分
                val complexUsage = (textCount * complexityRatio * 32 * 1024).toLong() // 复杂部分更大
                sdfUsage + complexUsage
            }
        }
        
        // 复杂显示需要额外内存（气泡、图标等）
        val complexityMultiplier = 1f + complexityRatio * 2f // 复杂度越高，内存占用越大
        return (baseUsage * complexityMultiplier).toLong()
    }
    
    /**
     * 创建常用的气泡样式
     */
    object BubbleStyles {
        val DEFAULT = BubbleStyle()
        
        val SUCCESS = BubbleStyle(
            backgroundColor = Color.parseColor("#4CAF50"),
            borderColor = Color.parseColor("#388E3C")
        )
        
        val WARNING = BubbleStyle(
            backgroundColor = Color.parseColor("#FF9800"),
            borderColor = Color.parseColor("#F57C00")
        )
        
        val ERROR = BubbleStyle(
            backgroundColor = Color.parseColor("#F44336"),
            borderColor = Color.parseColor("#D32F2F")
        )
        
        val INFO = BubbleStyle(
            backgroundColor = Color.parseColor("#2196F3"),
            borderColor = Color.parseColor("#1976D2")
        )
    }
}

/**
 * 图标位置枚举
 */
enum class IconPosition {
    ABOVE,  // 上方
    BELOW,  // 下方
    LEFT,   // 左侧
    RIGHT   // 右侧
}

/**
 * 显示复杂度分析结果
 */
data class DisplayComplexityAnalysis(
    val totalCount: Int,            // 总文字数量
    val simpleTextCount: Int,       // 简单文字数量
    val complexTextCount: Int,      // 复杂文字数量
    val bubbleTextCount: Int,       // 气泡文字数量
    val iconTextCount: Int,         // 图标文字数量
    val complexityRatio: Float      // 复杂度比例
) 