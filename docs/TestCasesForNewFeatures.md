# 多行文字和RTL功能测试用例

## 📋 测试概述

本文档提供了详细的测试用例，用于验证多行文字自动换行和RTL文字显示功能的正确性和性能。

## 🧪 测试用例分类

### 1. 多行文字换行测试

#### 测试用例 1.1：基础自动换行
```kotlin
@Test
fun testBasicLineWrapping() {
    val longText = "这是一个很长的文字内容，需要测试自动换行功能是否正常工作。"
    
    val textInfo = TextRenderingUtils.createWrappedTextInfo(
        content = longText,
        x = 0.0, y = 0.0,
        maxWidth = 200f,
        style = TextStyle(lineSpacing = 1.2f)
    )
    
    textRenderer.addText(textInfo)
    
    // 验证点
    assertTrue("应该需要换行", textInfo.needsLineWrap())
    assertEquals("最大宽度应该是200", 200f, textInfo.style.maxWidth)
    assertTrue("行间距应该是1.2", textInfo.style.lineSpacing == 1.2f)
}
```

#### 测试用例 1.2：不换行文字
```kotlin
@Test
fun testNoLineWrapping() {
    val shortText = "短文字"
    
    val textInfo = TextRenderingUtils.createTextInfo(
        content = shortText,
        x = 0.0, y = 0.0,
        style = TextStyle(maxWidth = -1f) // 默认不换行
    )
    
    textRenderer.addText(textInfo)
    
    // 验证点
    assertFalse("不应该需要换行", textInfo.needsLineWrap())
    assertEquals("最大宽度应该是-1", -1f, textInfo.style.maxWidth)
}
```

#### 测试用例 1.3：多行气泡文字
```kotlin
@Test
fun testMultiLineBubbleText() {
    val bubbleText = "这是一个多行气泡文字测试，应该在气泡内正确换行显示。"
    
    val textInfo = TextRenderingUtils.createMultiLineBubbleTextInfo(
        content = bubbleText,
        x = 10.0, y = 20.0,
        maxWidth = 250f,
        bubbleStyle = TextRenderingUtils.BubbleStyles.INFO
    )
    
    textRenderer.addText(textInfo)
    
    // 验证点
    assertTrue("应该包含气泡", textInfo.hasBubble())
    assertTrue("应该需要换行", textInfo.needsLineWrap())
    assertEquals("显示类型应该是气泡文字", TextDisplayType.BUBBLE_TEXT_ONLY, textInfo.displayType)
}
```

### 2. RTL文字测试

#### 测试用例 2.1：阿拉伯语RTL
```kotlin
@Test
fun testArabicRTLText() {
    val arabicText = "مرحبا بك في التطبيق الجديد"
    
    val textInfo = TextRenderingUtils.createRTLTextInfo(
        content = arabicText,
        x = 0.0, y = 0.0,
        style = TextStyle(
            textDirection = TextDirection.RTL,
            textAlign = TextAlign.START
        ),
        forceRTL = true
    )
    
    textRenderer.addText(textInfo)
    
    // 验证点
    assertTrue("应该是RTL文字", textInfo.isRTL())
    assertEquals("文字方向应该是RTL", TextDirection.RTL, textInfo.style.textDirection)
    assertEquals("对齐方式应该是START", TextAlign.START, textInfo.style.textAlign)
}
```

#### 测试用例 2.2：希伯来语RTL
```kotlin
@Test
fun testHebrewRTLText() {
    val hebrewText = "שלום עולם זה טקסט בעברית"
    
    val textInfo = TextRenderingUtils.createRTLTextInfo(
        content = hebrewText,
        x = 0.0, y = 0.0,
        style = TextStyle(textDirection = TextDirection.AUTO)
    )
    
    textRenderer.addText(textInfo)
    
    // 验证点  
    assertTrue("应该自动检测为RTL", textInfo.isRTL())
    assertTrue("应该包含RTL字符", TextMeasureUtils.containsRTLCharacters(hebrewText))
}
```

#### 测试用例 2.3：混合方向文字
```kotlin
@Test
fun testMixedDirectionText() {
    val mixedText = "Hello مرحبا World עולם"
    
    val textInfo = TextRenderingUtils.createRTLTextInfo(
        content = mixedText,
        x = 0.0, y = 0.0,
        style = TextStyle(textDirection = TextDirection.AUTO)
    )
    
    textRenderer.addText(textInfo)
    
    // 验证点
    assertTrue("混合文字应该包含RTL字符", TextMeasureUtils.containsRTLCharacters(mixedText))
    // 自动检测可能根据主要字符确定方向
}
```

### 3. 对齐方式测试

#### 测试用例 3.1：START/END语义对齐
```kotlin
@Test
fun testSemanticAlignment() {
    // LTR文字的START对齐
    val ltrTextInfo = TextRenderingUtils.createTextInfo(
        content = "Left-to-Right Text",
        x = 0.0, y = 0.0,
        style = TextStyle(
            textDirection = TextDirection.LTR,
            textAlign = TextAlign.START
        )
    )
    
    // RTL文字的START对齐
    val rtlTextInfo = TextRenderingUtils.createRTLTextInfo(
        content = "نص من اليمين إلى اليسار",
        x = 0.0, y = 50.0,
        style = TextStyle(
            textDirection = TextDirection.RTL,
            textAlign = TextAlign.START
        )
    )
    
    textRenderer.addText(ltrTextInfo)
    textRenderer.addText(rtlTextInfo)
    
    // 验证点
    val ltrActualAlign = TextMeasureUtils.getActualAlignment(ltrTextInfo.style, false)
    val rtlActualAlign = TextMeasureUtils.getActualAlignment(rtlTextInfo.style, true)
    
    assertEquals("LTR的START应该是LEFT", TextAlign.LEFT, ltrActualAlign)
    assertEquals("RTL的START应该是RIGHT", TextAlign.RIGHT, rtlActualAlign)
}
```

### 4. 性能测试

#### 测试用例 4.1：大量多行文字性能
```kotlin
@Test
fun testManyMultiLineTextPerformance() {
    val longTexts = (1..50).map { index ->
        "这是第${index}个长文字内容，用于测试大量多行文字的渲染性能表现。"
    }
    
    val startTime = System.currentTimeMillis()
    
    longTexts.forEach { text ->
        val textInfo = TextRenderingUtils.createWrappedTextInfo(
            content = text,
            x = Random.nextDouble(-20.0, 20.0),
            y = Random.nextDouble(-15.0, 15.0),
            maxWidth = 300f
        )
        textRenderer.addText(textInfo)
    }
    
    // 执行一次渲染
    textRenderer.render(mvpMatrix)
    val endTime = System.currentTimeMillis()
    
    // 验证点
    val stats = textRenderer.getPerformanceStats()
    assertTrue("渲染时间应该合理", stats.renderTime < 50f) // 50ms内
    assertTrue("文字数量正确", stats.textCount == 50)
    
    val totalTime = endTime - startTime
    assertTrue("总处理时间应该合理", totalTime < 1000) // 1秒内
}
```

#### 测试用例 4.2：RTL文字性能测试
```kotlin
@Test
fun testRTLTextPerformance() {
    val rtlTexts = listOf(
        "مرحبا بكم في التطبيق",
        "שלום וברכה לכולם",
        "سلام علیکم دوستان",
        "مع السلامة والود"
    )
    
    val startTime = System.currentTimeMillis()
    
    repeat(25) { // 每种RTL文字重复25次，共100个
        rtlTexts.forEach { text ->
            val textInfo = TextRenderingUtils.createRTLTextInfo(
                content = text,
                x = Random.nextDouble(-20.0, 20.0),
                y = Random.nextDouble(-15.0, 15.0),
                forceRTL = true
            )
            textRenderer.addText(textInfo)
        }
    }
    
    textRenderer.render(mvpMatrix)
    val endTime = System.currentTimeMillis()
    
    // 验证点
    val stats = textRenderer.getPerformanceStats()
    assertTrue("RTL文字渲染时间应该合理", stats.renderTime < 30f)
    assertTrue("RTL文字数量正确", stats.textCount == 100)
    
    val totalTime = endTime - startTime
    assertTrue("RTL处理总时间应该合理", totalTime < 2000)
}
```

### 5. 边界条件测试

#### 测试用例 5.1：极端换行情况
```kotlin
@Test
fun testExtremeLineWrapping() {
    // 测试极窄的宽度
    val textInfo1 = TextRenderingUtils.createWrappedTextInfo(
        content = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z",
        x = 0.0, y = 0.0,
        maxWidth = 50f // 极窄
    )
    
    // 测试单个超长单词
    val textInfo2 = TextRenderingUtils.createWrappedTextInfo(
        content = "Supercalifragilisticexpialidocious",
        x = 0.0, y = 100.0,
        maxWidth = 100f
    )
    
    textRenderer.addText(textInfo1)
    textRenderer.addText(textInfo2)
    
    // 验证点：不应该崩溃，能正常处理
    val stats = textRenderer.getPerformanceStats()
    assertTrue("极端情况下仍能正常渲染", stats.textCount == 2)
}
```

#### 测试用例 5.2：空文字和特殊字符
```kotlin
@Test
fun testSpecialCases() {
    val specialTexts = listOf(
        "", // 空文字
        " ", // 空格
        "\n", // 换行符
        "\t", // 制表符
        "🌟🎉🎯", // Emoji
        "123456789", // 纯数字
        "!@#$%^&*()", // 特殊符号
        "English العربية עברית 中文" // 多语言混合
    )
    
    specialTexts.forEach { text ->
        val textInfo = TextRenderingUtils.createWrappedTextInfo(
            content = text,
            x = 0.0, y = 0.0,
            maxWidth = 200f
        )
        
        // 不应该崩溃
        textRenderer.addText(textInfo)
    }
    
    // 验证点
    val stats = textRenderer.getPerformanceStats()
    assertTrue("特殊字符处理正常", stats.textCount == specialTexts.size)
}
```

## 📊 预期结果和验证标准

### 功能验证标准

| 功能 | 验证标准 | 预期结果 |
|------|---------|---------|
| **自动换行** | maxWidth > 0时启用换行 | ✅ needsLineWrap() == true |
| **RTL检测** | 包含RTL字符时自动检测 | ✅ isRTL() == true |
| **语义对齐** | START/END根据方向调整 | ✅ 实际对齐符合预期 |
| **性能表现** | 大量文字渲染时间 | ✅ <50ms (100个文字) |
| **内存使用** | 多行文字内存占用 | ✅ 合理范围内 |

### 性能验证标准

| 场景 | 文字数量 | 预期渲染时间 | 预期内存 | 通过标准 |
|------|---------|-------------|---------|---------|
| **简单换行** | 50个 | <10ms | <20MB | ✅ |
| **复杂RTL** | 100个 | <30ms | <25MB | ✅ |
| **混合显示** | 200个 | <50ms | <40MB | ✅ |

### 兼容性验证

- ✅ **Android API Level**: 16+ (支持RTL需API 17+)
- ✅ **OpenGL ES版本**: 2.0+
- ✅ **设备性能**: 低中高端设备全覆盖
- ✅ **语言支持**: 主要LTR/RTL语言

## 🔧 测试执行方式

### 1. 自动化测试
```kotlin
// 在Android测试中运行
@RunWith(AndroidJUnit4::class)
class MultiLineRTLTextTest {
    
    @Before
    fun setup() {
        textRenderer = TextRenderingFactory.createRecommendedRenderer(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
    }
    
    // ... 测试用例
}
```

### 2. 手动测试
使用`MultiLineRTLExampleActivity`进行手动测试：
1. 启动示例应用
2. 测试不同maxWidth设置
3. 测试各种RTL文字输入
4. 观察性能统计信息
5. 验证显示效果

### 3. 压力测试
```kotlin
@Test
fun stressTestMultiLineRTL() {
    // 添加大量混合文字
    repeat(500) { index ->
        val content = if (index % 2 == 0) {
            "中英混合测试文字 Mixed content test text ${index}"
        } else {
            "نص مختلط للاختبار Hebrew עברית ${index}"
        }
        
        val textInfo = TextRenderingUtils.createWrappedTextInfo(
            content = content,
            x = Random.nextDouble(-50.0, 50.0),
            y = Random.nextDouble(-30.0, 30.0),
            maxWidth = Random.nextFloat() * 300 + 100 // 100-400px
        )
        
        textRenderer.addText(textInfo)
    }
    
    // 验证性能
    repeat(60) { // 模拟60帧
        textRenderer.render(mvpMatrix)
    }
    
    val stats = textRenderer.getPerformanceStats()
    assertTrue("压力测试通过", stats.renderTime < 100f)
}
```

## 📝 测试报告模板

### 测试结果记录
```
测试日期: ___________
设备信息: ___________
Android版本: ________

功能测试结果:
□ 基础换行功能     ☐ 通过 ☐ 失败
□ RTL文字显示      ☐ 通过 ☐ 失败  
□ 混合方向文字     ☐ 通过 ☐ 失败
□ 语义对齐功能     ☐ 通过 ☐ 失败
□ 性能表现        ☐ 通过 ☐ 失败

边界条件测试:
□ 极端换行        ☐ 通过 ☐ 失败
□ 特殊字符处理     ☐ 通过 ☐ 失败
□ 空文字处理      ☐ 通过 ☐ 失败

性能数据:
- 平均渲染时间: _____ms
- 内存使用峰值: _____MB  
- 文字处理速度: _____个/秒

问题记录:
______________________________
______________________________
```

---

## 📞 测试支持

通过这些全面的测试用例，可以确保多行文字和RTL功能的稳定性和性能。如果在测试过程中发现任何问题，请参考相关文档或联系技术支持。

**记住**：测试覆盖率越高，产品质量越可靠！🎯 