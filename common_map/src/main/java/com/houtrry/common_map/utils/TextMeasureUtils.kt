package com.houtrry.common_map.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.houtrry.common_map.data.*

/**
 * 文字测量工具类
 * 支持换行、RTL文字等复杂文字处理
 */
object TextMeasureUtils {
    
    /**
     * 测量单行文字尺寸
     * @param text 文字内容
     * @param paint 画笔
     * @return 文字尺寸结果
     */
    fun measureSingleLineText(text: String, paint: Paint): TextMeasureResult {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        return TextMeasureResult(
            width = bounds.width(),
            height = bounds.height(),
            lines = 1,
            lineHeights = listOf(bounds.height().toFloat()),
            lineWidths = listOf(paint.measureText(text))
        )
    }
    
    /**
     * 测量多行文字尺寸（支持自动换行）
     * @param text 文字内容
     * @param style 文字样式
     * @param context Android上下文
     * @return 多行文字信息
     */
    fun measureMultiLineText(
        text: String, 
        style: TextStyle, 
        context: Context
    ): MultiLineTextInfo {
        val textPaint = createTextPaint(style, context)
        
        return if (style.maxWidth > 0) {
            // 需要换行
            measureWithWrap(text, style, textPaint)
        } else {
            // 不需要换行
            val singleLineResult = measureSingleLineText(text, textPaint)
            MultiLineTextInfo(
                lines = listOf(text),
                totalWidth = singleLineResult.lineWidths.first(),
                totalHeight = singleLineResult.lineHeights.first(),
                lineSpacing = style.lineSpacing
            )
        }
    }
    
    /**
     * 使用StaticLayout进行自动换行测量
     */
    private fun measureWithWrap(
        text: String,
        style: TextStyle,
        textPaint: TextPaint
    ): MultiLineTextInfo {
        val maxWidth = style.maxWidth.toInt()
        
        val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxWidth)
                .setAlignment(getLayoutAlignment(style))
                .setLineSpacing(0f, style.lineSpacing)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text, textPaint, maxWidth,
                getLayoutAlignment(style),
                style.lineSpacing, 0f, false
            )
        }
        
        val lines = mutableListOf<String>()
        val lineWidths = mutableListOf<Float>()
        val lineHeights = mutableListOf<Float>()
        
        var maxLineWidth = 0f
        var totalHeight = 0f
        
        for (i in 0 until staticLayout.lineCount) {
            val lineStart = staticLayout.getLineStart(i)
            val lineEnd = staticLayout.getLineEnd(i)
            val lineText = text.substring(lineStart, lineEnd).trim()
            
            val lineWidth = staticLayout.getLineWidth(i)
            val lineHeight = staticLayout.getLineBottom(i) - staticLayout.getLineTop(i)
            
            lines.add(lineText)
            lineWidths.add(lineWidth)
            lineHeights.add(lineHeight.toFloat())
            
            maxLineWidth = maxOf(maxLineWidth, lineWidth)
            totalHeight += lineHeight * style.lineSpacing
        }
        
        return MultiLineTextInfo(
            lines = lines,
            totalWidth = maxLineWidth,
            totalHeight = totalHeight,
            lineSpacing = style.lineSpacing
        )
    }
    
    /**
     * 手动换行处理（更精确的控制）
     */
    fun wrapTextManually(
        text: String,
        maxWidth: Float,
        paint: Paint
    ): List<String> {
        if (maxWidth <= 0) return listOf(text)
        
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) {
                word
            } else {
                "$currentLine $word"
            }
            
            val testWidth = paint.measureText(testLine)
            
            if (testWidth <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    // 单个词就超过最大宽度，强制换行
                    lines.add(word)
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return lines
    }
    
    /**
     * 创建文字画笔
     */
    fun createTextPaint(style: TextStyle, context: Context): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = style.fontSize * context.resources.displayMetrics.scaledDensity
            color = style.textColor
            typeface = style.typeface
            alpha = (style.alpha * 255).toInt()
        }
    }
    
    /**
     * 获取Layout对齐方式
     */
    private fun getLayoutAlignment(style: TextStyle): Layout.Alignment {
        return when (style.textAlign) {
            TextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            TextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlign.START -> {
                if (style.textDirection == TextDirection.RTL) {
                    Layout.Alignment.ALIGN_OPPOSITE
                } else {
                    Layout.Alignment.ALIGN_NORMAL
                }
            }
            TextAlign.END -> {
                if (style.textDirection == TextDirection.RTL) {
                    Layout.Alignment.ALIGN_NORMAL
                } else {
                    Layout.Alignment.ALIGN_OPPOSITE
                }
            }
        }
    }
    
    /**
     * 检测文字是否包含RTL字符
     */
    fun containsRTLCharacters(text: String): Boolean {
        return text.toCharArray().any { char ->
            val directionality = Character.getDirectionality(char)
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
            directionality == Character.DIRECTIONALITY_ARABIC_NUMBER
        }
    }
    
    /**
     * 获取文字的实际对齐方式（考虑RTL）
     */
    fun getActualAlignment(style: TextStyle, isRTL: Boolean): TextAlign {
        return when (style.textAlign) {
            TextAlign.START -> if (isRTL) TextAlign.RIGHT else TextAlign.LEFT
            TextAlign.END -> if (isRTL) TextAlign.LEFT else TextAlign.RIGHT
            else -> style.textAlign
        }
    }
    
    /**
     * 计算文字在指定区域内的绘制坐标
     * @param textInfo 多行文字信息
     * @param containerWidth 容器宽度
     * @param containerHeight 容器高度  
     * @param style 文字样式
     * @param isRTL 是否RTL
     * @return 每行文字的绘制坐标列表
     */
    fun calculateDrawPositions(
        textInfo: MultiLineTextInfo,
        containerWidth: Float,
        containerHeight: Float,
        style: TextStyle,
        isRTL: Boolean
    ): List<Pair<Float, Float>> {
        val actualAlign = getActualAlignment(style, isRTL)
        val positions = mutableListOf<Pair<Float, Float>>()
        
        // 垂直居中起始Y坐标
        val startY = (containerHeight - textInfo.totalHeight) / 2
        var currentY = startY
        
        textInfo.lines.forEachIndexed { index, line ->
            val lineWidth = if (index < textInfo.lines.size) {
                // 简化：这里应该从MultiLineTextInfo中获取实际宽度
                textInfo.totalWidth / textInfo.lines.size
            } else {
                textInfo.totalWidth
            }
            
            val x = when (actualAlign) {
                TextAlign.LEFT -> 0f
                TextAlign.RIGHT -> containerWidth - lineWidth
                TextAlign.CENTER -> (containerWidth - lineWidth) / 2
                else -> 0f // START和END已经转换为LEFT/RIGHT
            }
            
            positions.add(Pair(x, currentY))
            currentY += textInfo.totalHeight / textInfo.lines.size * textInfo.lineSpacing
        }
        
        return positions
    }
    
    /**
     * 估算多行文字的内存使用量
     */
    fun estimateMultiLineMemoryUsage(textInfo: MultiLineTextInfo): Long {
        // 每行文字按bitmap估算，包含行间距
        val avgLineWidth = textInfo.totalWidth
        val avgLineHeight = textInfo.totalHeight / textInfo.lines.size
        val totalLines = textInfo.lines.size
        
        // ARGB_8888 = 4 bytes per pixel
        return (avgLineWidth * avgLineHeight * totalLines * 4 * 1.2).toLong() // 1.2为缓冲系数
    }
} 