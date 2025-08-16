# 高性能“气泡+图标+文字”渲染方案（ES2 兼容，2000+ 规模）

## 目标

- 在 0~2000+ 标注规模下，稳定渲染“气泡+图标+文字”。
- 最小改动接入 `common_map` 现有框架（`HybridTextRenderer/SDFTextRenderer` 等）。

## 方案概述

- 分层渲染：气泡 → 图标 → 文字（SDF）。
- 资源复用：
  - 气泡主体：九宫格纹理缓存（不含箭头），箭头独立几何绘制，避免变形。
  - 图标：图标图集，按 UV 取子图批量绘制。
  - 文字：SDF 字体图集，批量绘制字符。
- 屏幕空间布局：所有排版在像素坐标计算，最后统一转换到 NDC 顶点后绘制。
- 屏幕限流/设备自适应：限制屏幕内“复杂对象”上限，按设备等级动态开关阴影/描边。

```mermaid
flowchart TB
    I[输入: 0~2000+ 标注] --> C[视锥/网格/碰撞(可扩展)]
    C --> L[屏幕空间布局(px)]
    L --> B[气泡主体(九宫格纹理, 批绘)]
    B --> A[箭头(三角几何)]
    A --> D[图标(Atlas 批绘)]
    D --> T[文字(SDF 图集)]
    style B fill:#4FC3F7
    style D fill:#81C784
    style T fill:#9575CD
```

## 关键模块

- `ScreenLayoutUtils`
  - `layoutBubbleIconText(...)`：根据文本/图标/气泡参数输出像素级矩形与三角形，支持 `arrow.autoFlip`。
  - 所有尺寸/间距/箭头方向通过参数传入，避免写死。
- `BubbleNineSliceCache`
  - 以“颜色/描边/阴影/圆角/尺寸桶”作为 key 生成并缓存主体纹理，避免重复上传。
- `IconAtlasCache`
  - 多图集：自动查询 `GL_MAX_TEXTURE_SIZE`（失败兜底 2048），按等格单元拆分到多个图集；渲染时按 `textureId+UV` 分批绘制。
- `SpriteBatchRenderer`
  - 轻量批绘四边形（一次绑定纹理，多次绘制）。
- `ArrowRenderer`
  - 单色三角形渲染（与 Sprite 同混合）。

## 设备自适应

- 使用 `getDevicePerformanceLevel`：
  - 高端：复杂上限 400，阴影/描边开启。
  - 中端：复杂上限 200，阴影/描边开启。
  - 低端：复杂上限 100，阴影关闭、描边开启。

## 降级策略（可配置）

- 开关：`HybridConfig.allowDegrade`（默认 true）。
- 顺序：`HybridConfig.degradeOrder`（默认 `[ICON_ONLY, ICON_WITH_SHORT_TEXT, FULL_BUBBLE]`）。
- 短文本阈值：`HybridConfig.shortTextMaxWidthPx`（按像素裁切，默认 80px，自动追加省略号）。
- 禁止降级（`allowDegrade=false`）：超限时仍绘制完整气泡，保证一致性，可能牺牲帧率。

## SDF 字体回退策略

- `SDFTextRenderer.supportsText(text)` 对复杂脚本/Emoji/合字保守返回 false，统一回退 Canvas/Complex 路径，保证中英阿等 30+ 语言的正确形态与换行；Latin/CJK 等常见字符走 SDF 以保证性能。

## 使用示例

```kotlin
val renderer = TextRenderingFactory.createRecommendedRenderer(context)

// 可选：按设备自适应 + 降级策略/多图集默认即可
val config = HybridConfig(
    allowDegrade = true,
    degradeOrder = listOf(
        HybridTextRenderer.DegradeStep.ICON_ONLY,
        HybridTextRenderer.DegradeStep.ICON_WITH_SHORT_TEXT,
        HybridTextRenderer.DegradeStep.FULL_BUBBLE
    ),
    shortTextMaxWidthPx = 80f
)
renderer.initialize(RenderingScenario.NewMapScenario, config)
val items = (0 until 2000).map { i ->
    TextRenderingUtils.createBubbleIconTextInfo(
        content = "POI-$i",
        x = i % 100 * 5.0, y = i / 100 * 5.0,
        iconResourceId = R.drawable.ic_poi,
        bubbleStyle = TextRenderingUtils.BubbleStyles.INFO,
        iconSize = 20 to 20,
        style = TextStyle(fontSize = 14f)
    )
}
renderer.addTexts(items)
// 渲染调用不变
```

## 验收建议（10 条）

- 2000 全量，低缩放：仅图标或降级，>55 FPS。
- 中缩放：约 600 可见，图标+SDF 文字，>50 FPS。
- 高缩放：约 200 气泡可见，>45 FPS。
- 长文本多行：气泡高度自适应，箭头不变形。
- 屏幕贴边：自动翻转箭头或平移至可见。
- 主题换色：九宫格主体/箭头颜色同步，零纹理重传。
- 大量相同图标：图集 1 次绑定，批绘。
- 字符集扩展：SDF 图集预热+增量，不卡顿。
- 低端设备：阴影关闭，帧率稳定。
- 快速缩放/拖拽：先图标，再文字，最后气泡，过程平滑。

---

## 附录：HybridConfig 参数表

| 参数 | 类型/默认值 | 作用 |
|---|---|---|
| `sdfThreshold` | Int = 50 | 简单文字数量达到该阈值后，启动/倾向使用 SDF 路径 |
| `migrationDelay` | Long = 3000L | 简单文字从 Canvas 迁移到 SDF 的延迟（毫秒） |
| `batchSize` | Int = 10 | 迁移批次大小（每次迁移的文字数量） |
| `complexTextThreshold` | Int = 20 | 复杂文字数量达到该阈值时触发智能混合策略 |
| `allowDegrade` | Boolean = true | 是否允许在复杂对象超限时执行降级 |
| `degradeOrder` | List = [ICON_ONLY, ICON_WITH_SHORT_TEXT, FULL_BUBBLE] | 降级顺序：仅图标 → 图标+短文本 → 完整气泡 |
| `shortTextMaxWidthPx` | Float = 80f | 短文本像素宽度阈值；超出则截断并加省略号 |
| `limitComplexPerScreen` | Int? = null | 屏内复杂对象上限；null 表示按设备等级默认值（高400/中200/低100） |
| `iconTextGapDp` | Float = 4f | 图标与文字的间距（dp） |
| `paddingLeftDp` | Float = 8f | 气泡左内边距（dp） |
| `paddingTopDp` | Float = 6f | 气泡上内边距（dp） |
| `paddingRightDp` | Float = 8f | 气泡右内边距（dp） |
| `paddingBottomDp` | Float = 6f | 气泡下内边距（dp） |
| `arrowWidthDp` | Float = 12f | 箭头宽（dp） |
| `arrowHeightDp` | Float = 6f | 箭头高（dp） |
| `arrowAutoFlip` | Boolean = true | 是否根据贴边自动翻转箭头方向 |

---

## 按机型推荐参数表（可直接用于 `TextRenderingFactory.getRecommendedConfig`）

| 设备档位 | 迁移/批处理 | 降级策略 | 屏内复杂上限 | 网格与碰撞 |
|---|---|---|---|---|
| 高端 | `sdfThreshold=80`, `migrationDelay=2000`, `batchSize=15`, `complexTextThreshold=30` | `allowDegrade=true`, `degradeOrder=[ICON_ONLY, ICON_WITH_SHORT_TEXT, FULL_BUBBLE]`, `shortTextMaxWidthPx=96` | `limitComplexPerScreen=400` | `gridSizeDp=56`, `maxPerGrid=1`, `collisionWidthDp=88`, `collisionHeightDp=28`, `arrowAutoFlip=true` |
| 中端 | `sdfThreshold=50`, `migrationDelay=3000`, `batchSize=10`, `complexTextThreshold=20` | 同上，`shortTextMaxWidthPx=80` | `limitComplexPerScreen=200` | `gridSizeDp=64`, `maxPerGrid=1`, `collisionWidthDp=96`, `collisionHeightDp=32`, `arrowAutoFlip=true` |
| 低端 | `sdfThreshold=30`, `migrationDelay=4000`, `batchSize=5`, `complexTextThreshold=10` | 同上，`shortTextMaxWidthPx=72` | `limitComplexPerScreen=100` | `gridSizeDp=72`, `maxPerGrid=1`, `collisionWidthDp=112`, `collisionHeightDp=40`, `arrowAutoFlip=true` |

说明：
- “屏内复杂上限”建议根据实时 FPS 再做自适应微调；阈值越小，帧率越稳。
- 若需更强一致性（不降级），可将 `allowDegrade=false`，但在 2000+ 爆量同屏时帧率会下降。
| `gridSizeDp` | Float = 64f | 屏幕网格大小（dp），用于限流分桶 |
| `maxPerGrid` | Int = 1 | 每网格允许的最大标注数 |
| `collisionWidthDp` | Float = 96f | 碰撞框宽（dp） |
| `collisionHeightDp` | Float = 32f | 碰撞框高（dp） |

说明：
- `DegradeStep` 取值为 `ICON_ONLY`、`ICON_WITH_SHORT_TEXT`、`FULL_BUBBLE`。
- 多图集自动按 `GL_MAX_TEXTURE_SIZE` 拆分，查询失败兜底 2048。
