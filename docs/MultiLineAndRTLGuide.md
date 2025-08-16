# OpenGL ES 2.0 多行文字和RTL支持指南

## 📋 概述

本指南详细介绍如何使用升级版的混合文字渲染系统来处理多行文字（自动换行）和从右到左（RTL）文字显示，如阿拉伯语、希伯来语等。

## 🔄 新增功能特性

### ✅ 支持的功能
- 🔸 **自动换行**：设置最大宽度，超出自动换行
- 🔸 **手动换行**：支持`\n`换行符
- 🔸 **RTL文字**：阿拉伯语、希伯来语等从右到左显示
- 🔸 **混合方向**：同一文本中混合LTR和RTL文字
- 🔸 **对齐控制**：支持START/END语义对齐
- 🔸 **行间距控制**：可调整行间距倍数

### 🎯 支持的语言
- **LTR（从左到右）**：中文、英文、法文、德文等
- **RTL（从右到左）**：阿拉伯文、希伯来文、波斯文、乌尔都语等
- **混合文本**：同时包含LTR和RTL字符的文本

## 🚀 使用示例

### 1. 基本换行文字

```kotlin
class BasicMultiLineExample {
    
    fun createWrappedText() {
        // 创建自动换行的文字
        val longText = "这是一段很长的文字内容，当超过指定的最大宽度时，会自动进行换行处理，确保文字在指定区域内正确显示。"
        
        val textInfo = TextRenderingUtils.createWrappedTextInfo(
            content = longText,
            x = 10.0, y = 20.0,
            maxWidth = 300f, // 最大宽度300像素
            style = TextStyle(
                fontSize = 16f,
                lineSpacing = 1.5f, // 行间距1.5倍
                textAlign = TextAlign.LEFT
            )
        )
        
        textRenderer.addText(textInfo)
        
        Log.d("MultiLine", "是否需要换行: ${textInfo.needsLineWrap()}")
    }
    
    fun createManualLineBreaks() {
        // 手动换行
        val multiLineText = "第一行内容\n第二行内容\n第三行内容"
        
        val textInfo = TextRenderingUtils.createTextInfo(
            content = multiLineText,
            x = 50.0, y = 100.0,
            style = TextStyle(
                fontSize = 14f,
                lineSpacing = 1.2f,
                textAlign = TextAlign.CENTER
            )
        )
        
        textRenderer.addText(textInfo)
    }
}
```

### 2. RTL文字支持

```kotlin
class RTLTextExample {
    
    fun createArabicText() {
        // 阿拉伯语文字（从右到左）
        val arabicText = "مرحبا بك في تطبيقنا الجديد"
        
        val textInfo = TextRenderingUtils.createRTLTextInfo(
            content = arabicText,
            x = 10.0, y = 50.0,
            style = TextStyle(
                fontSize = 18f,
                textAlign = TextAlign.START, // START在RTL中表示右对齐
                textDirection = TextDirection.RTL
            ),
            forceRTL = true
        )
        
        textRenderer.addText(textInfo)
        
        Log.d("RTL", "是否RTL文字: ${textInfo.isRTL()}")
    }
    
    fun createHebrewText() {
        // 希伯来语文字
        val hebrewText = "שלום עולם"
        
        val textInfo = TextRenderingUtils.createRTLTextInfo(
            content = hebrewText,
            x = 20.0, y = 80.0,
            style = TextStyle(
                fontSize = 16f,
                textAlign = TextAlign.END, // END在RTL中表示左对齐
                textDirection = TextDirection.AUTO // 自动检测
            )
        )
        
        textRenderer.addText(textInfo)
    }
    
    fun createMixedDirectionText() {
        // 混合方向文字：英文+阿拉伯文
        val mixedText = "Welcome مرحبا to our app تطبيقنا"
        
        val textInfo = TextRenderingUtils.createRTLTextInfo(
            content = mixedText,
            x = 30.0, y = 110.0,
            style = TextStyle(
                fontSize = 16f,
                textAlign = TextAlign.START,
                textDirection = TextDirection.AUTO // 自动检测主要方向
            )
        )
        
        textRenderer.addText(textInfo)
    }
}
```

### 3. 多行RTL文字

```kotlin
class MultiLineRTLExample {
    
    fun createWrappedArabicText() {
        // 长阿拉伯语文字，自动换行
        val longArabicText = """
            هذا نص طويل باللغة العربية سيتم تقسيمه تلقائياً إلى عدة أسطر 
            عندما يتجاوز العرض المحدد للنص
        """.trimIndent()
        
        val textInfo = TextRenderingUtils.createWrappedTextInfo(
            content = longArabicText,
            x = 10.0, y = 150.0,
            maxWidth = 250f,
            style = TextStyle(
                fontSize = 14f,
                lineSpacing = 1.4f,
                textDirection = TextDirection.RTL,
                textAlign = TextAlign.START // RTL中的起始对齐（右对齐）
            )
        )
        
        textRenderer.addText(textInfo)
    }
    
    fun createMultiLineRTLBubble() {
        // 气泡中的多行RTL文字
        val rtlBubbleText = "رسالة مهمة\nيرجى قراءة هذا النص بعناية"
        
        val textInfo = TextRenderingUtils.createMultiLineBubbleTextInfo(
            content = rtlBubbleText,
            x = 40.0, y = 200.0,
            maxWidth = 200f,
            bubbleStyle = TextRenderingUtils.BubbleStyles.INFO,
            style = TextStyle(
                fontSize = 14f,
                textColor = Color.WHITE,
                lineSpacing = 1.3f,
                textDirection = TextDirection.RTL,
                textAlign = TextAlign.START
            )
        )
        
        textRenderer.addText(textInfo)
    }
}
```

### 4. 高级使用场景

```kotlin
class AdvancedMultiLineRTLExample {
    
    fun createResponsiveText() {
        // 响应式文字：根据内容长度决定是否换行
        val adaptiveTexts = listOf(
            "短文本",
            "这是一个中等长度的文本内容，可能需要换行",
            "这是一个非常长的文本内容，肯定需要进行自动换行处理，以确保在指定的区域内正确显示所有内容"
        )
        
        adaptiveTexts.forEachIndexed { index, text ->
            val maxWidth = if (text.length > 20) 300f else -1f // 长文字才换行
            
            val textInfo = if (maxWidth > 0) {
                TextRenderingUtils.createWrappedTextInfo(
                    content = text,
                    x = 10.0, y = 250.0 + index * 80.0,
                    maxWidth = maxWidth,
                    style = TextStyle(
                        fontSize = 16f,
                        lineSpacing = 1.2f,
                        textAlign = TextAlign.LEFT
                    )
                )
            } else {
                TextRenderingUtils.createTextInfo(
                    content = text,
                    x = 10.0, y = 250.0 + index * 80.0,
                    style = TextStyle(fontSize = 16f)
                )
            }
            
            textRenderer.addText(textInfo)
        }
    }
    
    fun createInternationalizedUI() {
        // 国际化UI：支持多种语言
        val messages = mapOf(
            "en" to "Hello World",
            "ar" to "مرحبا بالعالم", 
            "he" to "שלום עולם",
            "zh" to "你好世界",
            "fa" to "سلام دنیا" // 波斯语
        )
        
        messages.entries.forEachIndexed { index, (lang, text) ->
            val isRTL = lang in listOf("ar", "he", "fa")
            
            val textInfo = if (isRTL) {
                TextRenderingUtils.createRTLTextInfo(
                    content = text,
                    x = 50.0 + index * 100.0, y = 400.0,
                    style = TextStyle(
                        fontSize = 16f,
                        textAlign = TextAlign.START,
                        textDirection = if (isRTL) TextDirection.RTL else TextDirection.LTR
                    ),
                    forceRTL = isRTL
                )
            } else {
                TextRenderingUtils.createTextInfo(
                    content = text,
                    x = 50.0 + index * 100.0, y = 400.0,
                    style = TextStyle(
                        fontSize = 16f,
                        textAlign = TextAlign.START
                    )
                )
            }
            
            textRenderer.addText(textInfo)
        }
    }
}
```

## 🎨 对齐方式说明

### 文字对齐详解

```kotlin
object TextAlignmentGuide {
    
    fun demonstrateAlignment() {
        val sampleText = "示例文字 Sample Text نص تجريبي"
        
        // 不同对齐方式的效果
        val alignments = mapOf(
            TextAlign.LEFT to "总是左对齐",
            TextAlign.RIGHT to "总是右对齐", 
            TextAlign.CENTER to "总是居中对齐",
            TextAlign.START to "起始对齐（LTR左，RTL右）",
            TextAlign.END to "结束对齐（LTR右，RTL左）"
        )
        
        alignments.entries.forEachIndexed { index, (align, description) ->
            // LTR版本
            val ltrText = TextRenderingUtils.createTextInfo(
                content = "$description (LTR)",
                x = 50.0, y = 50.0 + index * 60.0,
                style = TextStyle(
                    textAlign = align,
                    textDirection = TextDirection.LTR
                )
            )
            
            // RTL版本  
            val rtlText = TextRenderingUtils.createTextInfo(
                content = "$description (RTL)",
                x = 300.0, y = 50.0 + index * 60.0,
                style = TextStyle(
                    textAlign = align,
                    textDirection = TextDirection.RTL
                )
            )
            
            textRenderer.addText(ltrText)
            textRenderer.addText(rtlText)
        }
    }
}
```

## 📊 性能优化建议

### 1. 换行文字优化

```kotlin
class MultiLineOptimization {
    
    fun optimizeForPerformance() {
        // ✅ 推荐：合理设置最大宽度
        val textInfo = TextRenderingUtils.createWrappedTextInfo(
            content = longText,
            x = x, y = y,
            maxWidth = 300f, // 避免过宽或过窄
            style = TextStyle(
                lineSpacing = 1.2f // 合理的行间距
            )
        )
        
        // ❌ 避免：过于频繁的换行
        // maxWidth = 50f // 太窄，会导致很多行
        
        // ❌ 避免：过大的行间距
        // lineSpacing = 3.0f // 会占用过多空间
    }
    
    fun cacheMultiLineText() {
        // ✅ 推荐：对于经常使用的多行文字，预先计算
        val commonTexts = listOf(
            "常用的长文本内容1",
            "常用的长文本内容2"
        )
        
        val precomputedTexts = commonTexts.map { text ->
            val multiLineInfo = TextMeasureUtils.measureMultiLineText(
                text, 
                TextStyle(maxWidth = 300f), 
                context
            )
            Pair(text, multiLineInfo)
        }
        
        // 使用预计算的结果
        precomputedTexts.forEach { (text, info) ->
            Log.d("Cache", "文字: $text, 行数: ${info.lines.size}")
        }
    }
}
```

### 2. RTL文字优化

```kotlin
class RTLOptimization {
    
    fun detectRTLEfficiently() {
        val texts = listOf("English", "العربية", "עברית", "فارسی")
        
        texts.forEach { text ->
            // ✅ 高效的RTL检测
            val hasRTL = TextMeasureUtils.containsRTLCharacters(text)
            
            val textInfo = if (hasRTL) {
                TextRenderingUtils.createRTLTextInfo(
                    content = text,
                    x = 0.0, y = 0.0,
                    forceRTL = true
                )
            } else {
                TextRenderingUtils.createTextInfo(
                    content = text,
                    x = 0.0, y = 0.0
                )
            }
            
            textRenderer.addText(textInfo)
        }
    }
    
    fun batchRTLProcessing() {
        val rtlTexts = listOf(
            "مرحبا",
            "شكرا لك",
            "مع السلامة"
        )
        
        // ✅ 推荐：批量处理RTL文字
        val rtlTextInfos = rtlTexts.mapIndexed { index, text ->
            TextRenderingUtils.createRTLTextInfo(
                content = text,
                x = 0.0, y = index * 40.0,
                style = TextStyle(
                    textDirection = TextDirection.RTL,
                    textAlign = TextAlign.START
                )
            )
        }
        
        textRenderer.addTexts(rtlTextInfos)
    }
}
```

## ⚠️ 注意事项和最佳实践

### 1. 换行文字注意事项

```kotlin
// ✅ 推荐做法
val goodStyle = TextStyle(
    maxWidth = 300f,           // 合理的最大宽度
    lineSpacing = 1.2f,        // 适中的行间距
    textAlign = TextAlign.LEFT // 明确的对齐方式
)

// ❌ 避免的做法
val badStyle = TextStyle(
    maxWidth = 50f,            // 过小的宽度
    lineSpacing = 0.5f,        // 过小的行间距
    textAlign = TextAlign.START // 在多行文字中可能产生混淆
)
```

### 2. RTL文字注意事项

```kotlin
// ✅ 推荐：明确指定文字方向
val rtlStyle = TextStyle(
    textDirection = TextDirection.RTL,
    textAlign = TextAlign.START  // 在RTL中表示右对齐
)

// ✅ 推荐：使用AUTO自动检测
val autoStyle = TextStyle(
    textDirection = TextDirection.AUTO,
    textAlign = TextAlign.START  // 根据检测到的方向自动对齐
)

// ❌ 避免：混合使用绝对对齐和RTL
val inconsistentStyle = TextStyle(
    textDirection = TextDirection.RTL,
    textAlign = TextAlign.LEFT   // 可能导致显示不一致
)
```

### 3. 性能监控

```kotlin
class MultiLineRTLPerformanceMonitor {
    
    fun monitorComplexTextPerformance() {
        val complexTexts = getComplexTexts() // 包含换行和RTL的文字
        
        val analysis = TextRenderingUtils.analyzeDisplayComplexity(complexTexts)
        
        Log.d("Performance", """
            复杂文字分析:
            - 总数: ${analysis.totalCount}
            - 简单: ${analysis.simpleTextCount}  
            - 复杂: ${analysis.complexTextCount}
            - 复杂度比例: ${String.format("%.1f", analysis.complexityRatio * 100)}%
        """.trimIndent())
        
        // 如果复杂文字过多，考虑优化
        if (analysis.complexityRatio > 0.6f) {
            Log.w("Performance", "复杂文字比例过高，建议优化")
            optimizeComplexTexts(complexTexts)
        }
    }
    
    private fun optimizeComplexTexts(texts: List<TextInfo>) {
        // 1. 减少不必要的换行
        // 2. 简化RTL文字的处理
        // 3. 使用缓存避免重复计算
    }
}
```

## 🔧 故障排除

### Q1: 换行不生效？
**A:** 检查maxWidth设置：
```kotlin
// 确保maxWidth > 0
val style = TextStyle(maxWidth = 300f) // ✅ 正确
// val style = TextStyle(maxWidth = -1f) // ❌ 不会换行
```

### Q2: RTL文字显示方向错误？
**A:** 检查textDirection和textAlign设置：
```kotlin
// ✅ 正确的RTL设置
val rtlStyle = TextStyle(
    textDirection = TextDirection.RTL,
    textAlign = TextAlign.START // RTL中的START表示右对齐
)
```

### Q3: 多行文字在气泡中显示异常？
**A:** 确保气泡尺寸计算正确：
```kotlin
// 多行文字需要更大的气泡
val bubbleInfo = BubbleInfo(
    padding = BubblePadding(
        left = 12f, top = 10f,  // 增加内边距
        right = 12f, bottom = 10f
    )
)
```

---

## 📞 技术支持

升级版的文字渲染系统现在完全支持多行文字和RTL显示，能够处理复杂的国际化文字需求。

**核心优势**：
- ✅ 完整的换行支持
- ✅ 原生RTL文字处理  
- ✅ 混合方向文字支持
- ✅ 性能优化的文字测量
- ✅ 灵活的对齐控制

**记住**：选择合适的maxWidth和textDirection是关键！🎯 