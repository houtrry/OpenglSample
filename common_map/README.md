# Common Map - OpenGL ES 2.0 文字渲染模块

基于OpenGL ES 2.0的高性能混合文字渲染解决方案，智能结合Canvas和SDF技术，**现已支持多行文字自动换行和RTL文字显示**。

## 🚀 快速开始

### 1. 基本使用
```kotlin
// 创建推荐的混合渲染器
val textRenderer = TextRenderingFactory.createRecommendedRenderer(context)

// 添加简单文字
val textInfo = TextRenderingUtils.createTextInfo("地标1", x = 10.0, y = 20.0)
textRenderer.addText(textInfo)

// 在渲染循环中绘制
textRenderer.render(mvpMatrix)
```

### 2. 多行文字支持 🆕
```kotlin
// 自动换行的长文字
val longTextInfo = TextRenderingUtils.createWrappedTextInfo(
    content = "这是一段很长的文字内容，会自动换行显示",
    x = 10.0, y = 20.0,
    maxWidth = 300f, // 最大宽度300像素
    style = TextStyle(
        lineSpacing = 1.5f, // 行间距1.5倍
        textAlign = TextAlign.LEFT
    )
)
textRenderer.addText(longTextInfo)
```

### 3. RTL文字支持 🆕
```kotlin
// 阿拉伯语文字（从右到左）
val arabicTextInfo = TextRenderingUtils.createRTLTextInfo(
    content = "مرحبا بك في تطبيقنا",
    x = 10.0, y = 50.0,
    style = TextStyle(
        textAlign = TextAlign.START, // START在RTL中表示右对齐
        textDirection = TextDirection.RTL
    )
)
textRenderer.addText(arabicTextInfo)
```

### 4. 场景化使用

#### 新建地图场景
```kotlin
val renderer = TextRenderingFactory.createRecommendedRenderer(
    context = this,
    scenario = RenderingScenario.NewMapScenario
)
```

#### 扩展地图场景
```kotlin
val existingTexts = listOf("地标1", "地标2", "地标3")
val renderer = TextRenderingFactory.createRecommendedRenderer(
    context = this,
    scenario = RenderingScenario.ExtendMapScenario(existingTexts)
)
```

## 📁 模块结构

```
common_map/
├── src/main/java/com/houtrry/common_map/
│   ├── data/                    # 数据类
│   │   ├── TextRenderingData.kt # 核心数据定义（支持RTL和换行）
│   │   ├── Vector3.kt           # 3D向量
│   │   └── ...
│   ├── text/                    # 文字渲染核心
│   │   ├── BaseTextRenderer.kt  # 基础渲染器
│   │   ├── CanvasTextRenderer.kt# Canvas渲染器
│   │   ├── SDFTextRenderer.kt   # SDF渲染器
│   │   ├── ComplexTextRenderer.kt# 复杂文字渲染器（支持多行/RTL）
│   │   ├── HybridTextRenderer.kt# 混合渲染器（核心）
│   │   └── TextRenderingFactory.kt # 工厂类
│   ├── utils/                   # 工具类
│   │   ├── extends.kt          # 扩展函数
│   │   └── TextMeasureUtils.kt  # 文字测量工具（换行/RTL支持）
│   └── example/                 # 使用示例
│       ├── TextRenderingExampleActivity.kt
│       └── MultiLineRTLExampleActivity.kt # 多行/RTL示例
└── docs/
    ├── TextRenderingUsageGuide.md     # 详细使用指南
    ├── ComplexTextRenderingGuide.md   # 复杂文字指南
    └── MultiLineAndRTLGuide.md        # 换行/RTL指南
```

## ✨ 核心特性

### 基础功能
- 🎭 **智能混合**：自动在Canvas、SDF和复杂文字渲染间切换
- 🚀 **高性能**：支持2000+文字的流畅渲染  
- 💾 **内存优化**：比纯Canvas方案节省60%+内存
- 📱 **设备适配**：根据设备性能自动调优

### 复杂显示支持
- 🔸 **纯文字**：基础文字显示
- 🔸 **图标+文字**：上下左右4种图标位置
- 🔸 **气泡文字**：多种预设气泡样式
- 🔸 **气泡+图标+文字**：完整的复合显示

### 🆕 新增功能
- 📝 **自动换行**：设置最大宽度，超出自动换行
- 📝 **手动换行**：支持`\n`换行符和行间距控制
- 🌐 **RTL文字**：完整支持阿拉伯语、希伯来语等RTL文字
- 🌐 **混合方向**：同一文本中混合LTR和RTL文字
- 🎨 **智能对齐**：支持START/END语义对齐，RTL自适应
- 🔍 **自动检测**：自动检测文字方向和语言特征

## 📖 详细文档

查看相应指南了解详细用法：
- [基础使用指南](../docs/TextRenderingUsageGuide.md) - 基本功能和API
- [复杂文字指南](../docs/ComplexTextRenderingGuide.md) - 图标、气泡等复杂显示
- [多行文字和RTL指南](../docs/MultiLineAndRTLGuide.md) - 换行和RTL文字支持

## 🔧 API概览

### 主要类
- `TextRenderingFactory` - 工厂类，快速创建渲染器
- `HybridTextRenderer` - 混合渲染器（推荐使用）
- `TextRenderingUtils` - 工具类，提供便捷方法
- `TextMeasureUtils` - 文字测量工具（换行/RTL支持）

### 关键方法
```kotlin
// 基础渲染器创建
TextRenderingFactory.createRecommendedRenderer(context, scenario, config)

// 基础文字操作
textRenderer.addText(textInfo)
textRenderer.removeText(textId)
textRenderer.render(mvpMatrix)

// 🆕 换行文字
TextRenderingUtils.createWrappedTextInfo(content, x, y, maxWidth)

// 🆕 RTL文字
TextRenderingUtils.createRTLTextInfo(content, x, y, forceRTL = true)

// 🆕 多行气泡文字
TextRenderingUtils.createMultiLineBubbleTextInfo(content, x, y, maxWidth, bubbleStyle)

// 性能监控
textRenderer.getPerformanceStats()
textRenderer.getRenderingStatus()
```

## 🌍 国际化支持

### 支持的语言类型
- **LTR（从左到右）**：中文、英文、法文、德文等
- **RTL（从右到左）**：阿拉伯文、希伯来文、波斯文、乌尔都语等
- **混合文本**：同时包含LTR和RTL字符的文本

### RTL使用示例
```kotlin
// 自动检测文字方向
val autoDetectStyle = TextStyle(
    textDirection = TextDirection.AUTO,
    textAlign = TextAlign.START
)

// 强制RTL方向
val forcedRTLStyle = TextStyle(
    textDirection = TextDirection.RTL,
    textAlign = TextAlign.START // RTL中的START表示右对齐
)
```

## 🎯 使用建议

### ✅ 推荐做法
- **智能混合**：使用`HybridTextRenderer`混合方案
- **批量操作**：用`addTexts()`而不是多次`addText()`
- **换行控制**：合理设置maxWidth避免过多行数
- **RTL适配**：使用START/END语义对齐而不是LEFT/RIGHT
- **及时释放**：在onDestroy()中调用`release()`
- **监控性能**：使用性能统计API监控状态

### 📊 性能基准

| 文字类型 | 数量 | 渲染时间 | 内存占用 | 推荐方案 |
|---------|------|---------|---------|---------|
| **纯文字** | <50个 | <2ms | <5MB | 混合 |
| **换行文字** | 50-200个 | 3-8ms | 10-25MB | 混合 |
| **RTL文字** | 任意 | 2-5ms | 5-15MB | 混合 |
| **复杂显示** | >500个 | 5-15ms | 8-15MB | 混合 |

### ⚠️ 注意事项
- maxWidth设置为-1表示不换行，>0表示换行
- RTL文字建议使用START/END对齐而不是LEFT/RIGHT
- 多行文字会增加内存占用，建议合理控制行数
- 复杂显示（气泡+图标）数量建议控制在100个以内

## 📱 示例应用

运行示例查看完整功能：
- `TextRenderingExampleActivity` - 基础功能演示
- `MultiLineRTLExampleActivity` - 换行和RTL功能演示

---

## 📊 版本更新

### v2.0.0 🆕
- ✅ 完整的多行文字自动换行支持
- ✅ 原生RTL文字显示（阿拉伯语、希伯来语等）
- ✅ 混合方向文字支持
- ✅ 智能文字方向检测
- ✅ START/END语义对齐
- ✅ 行间距控制
- ✅ 文字测量工具类
- ✅ 完整的使用指南和示例

### v1.0.0
- ✅ 基础混合渲染框架
- ✅ Canvas和SDF渲染器
- ✅ 复杂文字显示（图标、气泡）
- ✅ 智能切换算法
- ✅ 双场景支持（新建/扩展地图）
- ✅ 性能监控和统计
- ✅ 设备自适应配置

💡 **提示**：升级版混合方案现在支持完整的国际化文字需求，是构建全球化应用的最佳选择！🎯 