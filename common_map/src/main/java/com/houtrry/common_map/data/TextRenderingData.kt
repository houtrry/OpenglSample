package com.houtrry.common_map.data

import android.graphics.Color
import android.graphics.Typeface

/**
 * 文字信息数据类
 * @param content 文字内容
 * @param position 文字在地图上的位置
 * @param style 文字样式
 * @param displayType 显示类型
 * @param iconInfo 图标信息（可选）
 * @param bubbleInfo 气泡信息（可选）
 * @param id 唯一标识符
 */
data class TextInfo(
    val content: String,
    val position: Vector3,
    val style: TextStyle = TextStyle(),
    val displayType: TextDisplayType = TextDisplayType.PURE_TEXT,
    val iconInfo: IconInfo? = null,
    val bubbleInfo: BubbleInfo? = null,
    val id: String = generateTextId(content, position)
) {
    companion object {
        private fun generateTextId(content: String, position: Vector3): String {
            return "${content.hashCode()}_${position.x.toInt()}_${position.y.toInt()}"
        }
    }
    
    /**
     * 判断是否为复杂显示（需要Canvas渲染）
     */
    fun isComplexDisplay(): Boolean {
        return displayType != TextDisplayType.PURE_TEXT
    }
    
    /**
     * 判断是否包含气泡
     */
    fun hasBubble(): Boolean {
        return bubbleInfo != null
    }
    
    /**
     * 判断是否包含图标
     */
    fun hasIcon(): Boolean {
        return iconInfo != null
    }
    
    /**
     * 判断是否需要换行
     */
    fun needsLineWrap(): Boolean {
        return style.maxWidth > 0
    }
    
    /**
     * 判断是否为RTL文字
     */
    fun isRTL(): Boolean {
        return style.textDirection == TextDirection.RTL || 
               (style.textDirection == TextDirection.AUTO && detectRTL(content))
    }
    
    /**
     * 自动检测文字方向
     */
    private fun detectRTL(text: String): Boolean {
        // 检测常见的RTL字符
        val rtlChars = text.toCharArray()
        for (char in rtlChars) {
            val directionality = Character.getDirectionality(char)
            if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true
            }
        }
        return false
    }
}

/**
 * 文字显示类型枚举
 */
enum class TextDisplayType {
    /**
     * 纯文字，无其他元素
     */
    PURE_TEXT,
    
    /**
     * 上方图标，下方文字
     */
    ICON_ABOVE_TEXT,
    
    /**
     * 左侧图标，右侧文字
     */
    ICON_LEFT_TEXT,
    
    /**
     * 右侧图标，左侧文字
     */
    ICON_RIGHT_TEXT,
    
    /**
     * 下方图标，上方文字
     */
    ICON_BELOW_TEXT,
    
    /**
     * 气泡中只有文字
     */
    BUBBLE_TEXT_ONLY,
    
    /**
     * 气泡中有文字和图标
     */
    BUBBLE_TEXT_WITH_ICON
}

/**
 * 图标信息
 * @param resourceId 图标资源ID
 * @param width 图标宽度
 * @param height 图标高度
 * @param tint 图标着色（可选）
 */
data class IconInfo(
    val resourceId: Int,
    val width: Int,
    val height: Int,
    val tint: Int? = null
)

/**
 * 气泡信息
 * @param style 气泡样式
 * @param padding 内边距
 * @param cornerRadius 圆角半径
 */
data class BubbleInfo(
    val style: BubbleStyle = BubbleStyle(),
    val padding: BubblePadding = BubblePadding(),
    val cornerRadius: Float = 8f,
    /**
     * 单个标注级的箭头自动翻转开关：
     * null 表示使用全局配置；true 表示该标注启用自动翻转；false 表示固定方向
     */
    val arrowAutoFlip: Boolean? = null
)

/**
 * 气泡样式
 * @param backgroundColor 背景颜色
 * @param borderColor 边框颜色
 * @param borderWidth 边框宽度
 * @param shadowColor 阴影颜色（可选）
 * @param shadowRadius 阴影半径
 */
data class BubbleStyle(
    val backgroundColor: Int = Color.WHITE,
    val borderColor: Int = Color.GRAY,
    val borderWidth: Float = 1f,
    val shadowColor: Int? = Color.parseColor("#33000000"),
    val shadowRadius: Float = 4f
)

/**
 * 气泡内边距
 * @param left 左边距
 * @param top 上边距
 * @param right 右边距
 * @param bottom 下边距
 */
data class BubblePadding(
    val left: Float = 8f,
    val top: Float = 6f,
    val right: Float = 8f,
    val bottom: Float = 6f
)

/**
 * 文字样式配置
 * @param fontSize 字体大小(sp)
 * @param textColor 文字颜色
 * @param typeface 字体
 * @param alpha 透明度(0.0-1.0)
 * @param maxWidth 最大宽度，超过则自动换行（-1表示不限制）
 * @param lineSpacing 行间距倍数
 * @param textDirection 文字方向
 * @param textAlign 文字对齐方式
 */
data class TextStyle(
    val fontSize: Float = 16f,
    val textColor: Int = Color.BLACK,
    val typeface: Typeface = Typeface.DEFAULT,
    val alpha: Float = 1f,
    val maxWidth: Float = -1f,                          // 最大宽度（像素），-1表示不限制
    val lineSpacing: Float = 1.2f,                      // 行间距倍数
    val textDirection: TextDirection = TextDirection.AUTO, // 文字方向
    val textAlign: TextAlign = TextAlign.CENTER          // 文字对齐
)

/**
 * 文字渲染类型枚举
 */
enum class TextRenderType {
    /**
     * Canvas渲染：适合复杂显示，支持图标、气泡等
     */
    CANVAS,
    
    /**
     * SDF渲染：适合纯文字，性能极优
     */
    SDF,
    
    /**
     * 临时Canvas：新增文字的临时渲染，等待迁移到SDF
     */
    TEMPORARY_CANVAS,
    
    /**
     * 混合渲染：组件化渲染，气泡+图标用Canvas，文字用SDF
     */
    HYBRID_COMPONENT
}

/**
 * 渲染复杂度等级
 */
enum class RenderComplexity {
    SIMPLE,    // 简单：纯文字
    MEDIUM,    // 中等：文字+图标
    COMPLEX    // 复杂：气泡+文字+图标
}

/**
 * 渲染场景类型
 */
sealed class RenderingScenario {
    /**
     * 新建地图场景：从0开始逐步添加文字
     */
    object NewMapScenario : RenderingScenario()
    
    /**
     * 扩展地图场景：已有大量文字，需要批量显示
     * @param existingTexts 已存在的文字列表
     */
    data class ExtendMapScenario(
        val existingTexts: List<String>
    ) : RenderingScenario()
}

/**
 * 性能统计数据
 * @param renderTime 渲染时间(毫秒)
 * @param memoryUsage 内存占用(字节)
 * @param textCount 文字数量
 * @param complexTextCount 复杂文字数量
 * @param timestamp 时间戳
 */
data class PerformanceStats(
    val renderTime: Float,
    val memoryUsage: Long,
    val textCount: Int,
    val complexTextCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * SDF字符映射信息
 * @param character 字符
 * @param atlasX 在图集中的X坐标
 * @param atlasY 在图集中的Y坐标
 * @param width 字符宽度
 * @param height 字符高度
 */
data class SDFCharInfo(
    val character: Char,
    val atlasX: Int,
    val atlasY: Int,
    val width: Int,
    val height: Int
)

/**
 * 设备性能等级
 */
enum class DevicePerformanceLevel {
    HIGH,    // 高端设备
    MEDIUM,  // 中端设备  
    LOW      // 低端设备
}

/**
 * 文字方向枚举
 */
enum class TextDirection {
    /**
     * 从左到右（默认）
     */
    LTR,
    
    /**
     * 从右到左（阿拉伯语、希伯来语等）
     */
    RTL,
    
    /**
     * 自动检测
     */
    AUTO
}

/**
 * 文字对齐方式枚举
 */
enum class TextAlign {
    LEFT,      // 左对齐
    CENTER,    // 居中对齐  
    RIGHT,     // 右对齐
    START,     // 起始对齐（LTR时左对齐，RTL时右对齐）
    END        // 结束对齐（LTR时右对齐，RTL时左对齐）
}

/**
 * 文字测量结果
 * @param width 文字宽度
 * @param height 文字高度
 * @param lines 行数
 * @param lineHeights 各行高度
 * @param lineWidths 各行宽度
 */
data class TextMeasureResult(
    val width: Int,
    val height: Int,
    val lines: Int,
    val lineHeights: List<Float> = emptyList(),
    val lineWidths: List<Float> = emptyList()
)

/**
 * 多行文字信息
 * @param lines 文字行列表
 * @param totalWidth 总宽度
 * @param totalHeight 总高度
 * @param lineSpacing 行间距
 */
data class MultiLineTextInfo(
    val lines: List<String>,
    val totalWidth: Float,
    val totalHeight: Float,
    val lineSpacing: Float
) 