# SDF 统一指南（从基础到实战）

> 本文整合了项目内关于 SDF（Signed Distance Field，有符号距离场）的多篇文档内容，包括：
> - `SDF基础知识详解.md`
> - `SDF_OpenGL_Guide.md`
> - `Android_OpenGL_Text_Rendering_Analysis.md`
> - `TextRenderingUsageGuide.md`
> - `ComplexTextRenderingGuide.md`
> - `MarkerBatchOptimization.md`
>
> 目标：为对 SDF 了解甚少的读者提供一份“读完即可上手”的统一入口，涵盖概念、原理、实现、应用与优化。

---

## 目录
- 1. SDF 是什么：最直观的理解
- 2. 为什么用 SDF：它解决了哪些痛点
- 3. SDF 如何工作：从像素到距离的计算
- 4. 在 OpenGL ES 中使用 SDF：着色器与管线
- 5. 字体图集与批量渲染：从字符到屏幕
- 6. 混合方案（SDF + Canvas）：何时切换更合适
- 7. 性能与内存：指标、数据与优化建议
- 8. 多语言与大字符集：覆盖范围与策略
- 9. 参数选择与实践建议：半径、阈值、范围
- 10. 方案对比与选型建议
- 附录 A：参考代码片段（GLSL & Kotlin）
- 附录 B：相关文档链接

---

## 1. SDF 是什么：最直观的理解

- 概念：SDF 用一个标量场描述形状，数值表示“某点到最近边界的距离”，并带符号区分内外。
  - 正数：在形状内部
  - 0：在边界上
  - 负数：在形状外部
- 直观理解：把整张图当作“距离热力图”，距离边界越远，数值越大；穿过边界，数值符号翻转。
- 与位图差异：位图只有黑/白（0/1）；SDF 为每个像素提供连续的距离值，利于缩放与抗锯齿。

---

## 2. 为什么用 SDF：它解决了哪些痛点

- 任意缩放保持边缘清晰，极大降低不同字号/分辨率下的素材数量。
- GPU 友好：在片段着色器中用 `smoothstep` 等函数即可得到平滑边缘和可控描边/发光等效果。
- 内存效率高：一套 SDF 纹理支持多种字体大小；结合图集可批量渲染大量文本。
- 适合地图、UI、游戏中的大规模文字渲染与频繁缩放场景。

---

## 3. SDF 如何工作：从像素到距离的计算

- 核心流程：原始位图 → 边缘检测 → 距离计算（带符号） → 归一化 → 生成 SDF 纹理。
- 距离计算要点：
  - 以内/外判定为依据（典型用像素 alpha 与阈值比较，如 128/0.5）。
  - 在设定的搜索半径内寻找最近边界像素，取最小欧氏距离。
  - 归一化为 0-1（或 0-255）范围，便于纹理存储与 GPU 采样。
- 参数影响：搜索半径越大，距离估计越准确但生成更慢、纹理对比度更“柔”；反之亦然。

---

## 4. 在 OpenGL ES 中使用 SDF：着色器与管线

- 顶点着色器：常规传递位置与纹理坐标。
- 片段着色器：从 SDF 纹理采样距离值，使用 `smoothstep(edgeLow, edgeHigh, distance)` 估算 alpha，输出颜色。
- 可扩展效果：
  - 描边：对距离阈值做两段 `smoothstep`，差值作为描边覆盖。
  - 发光/阴影：扩大阈值范围并叠加颜色。
- 关键 uniform：`uSmoothness`（平滑宽度）随分辨率/字号缩放做适配。

---

## 5. 字体图集与批量渲染：从字符到屏幕

- 字体图集（Font Atlas）：把多个字形的 SDF 拼成一张大纹理，渲染时按字符索引取对应 UV。
- 批量渲染：
  - 将每个字的四边形顶点与 UV 组织成批次，一次或少量 draw call 输出整行/整段文字。
  - 优化方向：统一状态（同一纹理/着色器）、顶点打包、实例化（设备与 API 允许时）。
- 缓存策略：
  - 高频文本/常用字预先生成并缓存；低频/动态文本延迟生成或走 Canvas 路径。

---

## 6. 混合方案（SDF + Canvas）：何时切换更合适

- 思路：根据文本数量、变化频率与设备能力，在 SDF 和 Canvas 之间自适应切换（参见 `TextRenderingUsageGuide.md`、`ComplexTextRenderingGuide.md`）。
- 常见阈值策略：
  - 当总文字数 ≥ N（如 50~100）时，切 SDF 以提升批量渲染效率。
  - 复杂文本（多色/图标/富文本）走 Canvas；简单稳定文本走 SDF。
  - 迁移延迟与批大小：避免一瞬间大规模切换造成卡顿（如延迟 1.5~3s，每批 10~20 个）。
- 设备分级：高性能设备阈值更高、迁移更快；低性能设备阈值更低、批更小、延迟更长。

---

## 7. 性能与内存：指标、数据与优化建议

- 性能特征：
  - 静态/高频文本：SDF 批量渲染优势显著。
  - 动态/低频文本：Canvas 灵活、生成即用；与 SDF 混合可综合收益最佳。
- 内存占用：
  - SDF 图集相对集中、可复用；避免为每个字号/文本单独生成位图。
  - 大字符集（Unicode）需分片图集与按需加载，控制峰值内存。
- 典型优化：
  - 预生成与按需生成结合；LRU 缓存常用纹理；尽量减少纹理上传与重排。
  - 控制 `uSmoothness` 随缩放自适应，避免过度模糊或锯齿。

---

## 8. 多语言与大字符集：覆盖范围与策略

- 预设字符子集：按场景挑选常用字符（拉丁、常用中日韩字符等），其他按需生成与缓存。
- 图集分片：以区域/语言/业务模块划分，降低单图集尺寸与查找复杂度。
- 字形缺失回退：未命中字形走 Canvas 即时生成，或回退到占位符与延迟填充。

---

## 9. 参数选择与实践建议：半径、阈值、范围

- 搜索半径（Radius）：常见 8~16 像素；字号小取小、字号大取大；注意生成耗时与质量平衡。
- 阈值（Threshold）：区分内/外的 alpha 门限，常用 0.5（或 128）。
- 归一化范围（Range）：纹理写入 0~1（浮点）或 0~255（字节）；渲染端统一按 0~1 处理。
- `uSmoothness`：与屏幕 DPI、字号和缩放级别相关，建议做动态映射（如 `smoothness = base / scale`）。

---

## 10. 方案对比与选型建议

| 维度 | Canvas+Bitmap | SDF | 混合方案 |
|------|----------------|-----|----------|
| 开发复杂度 | 低 | 中 | 较高 |
| 批量渲染性能 | 中 | 高 | 高 |
| 动态灵活性 | 高 | 中 | 高 |
| 内存占用 | 高 | 低 | 中 |
| 多分辨率适配 | 一般 | 优秀 | 优秀 |

- 结论：
  - 全静态/大规模文本：首选 SDF。
  - 高频+低频混合场景：混合方案最佳。
  - 小规模、强动态、多样式：倾向 Canvas。

---

## 附录 A：参考代码片段（GLSL & Kotlin）

### A.1 SDF 片段着色器（片段）
```glsl
precision mediump float;
uniform sampler2D uFontAtlas;
uniform vec4 uTextColor;
uniform float uSmoothness;
varying vec2 texCoord;

void main() {
    float distance = texture2D(uFontAtlas, texCoord).r;
    float alpha = smoothstep(0.5 - uSmoothness, 0.5 + uSmoothness, distance);
    gl_FragColor = vec4(uTextColor.rgb, alpha * uTextColor.a);
}
```

### A.2 简化的 SDF 生成（Kotlin，示意）
```kotlin
private fun generateSDF(source: Bitmap, searchRadius: Int): Bitmap {
    val width = source.width
    val height = source.height
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)

    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val idx = y * width + x
            val alpha = Color.alpha(pixels[idx])
            val isInside = alpha > 128
            var minDist = Float.MAX_VALUE
            for (dy in -searchRadius..searchRadius) {
                for (dx in -searchRadius..searchRadius) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until width || ny !in 0 until height) continue
                    val nAlpha = Color.alpha(pixels[ny * width + nx])
                    val isEdge = if (isInside) nAlpha <= 128 else nAlpha > 128
                    if (isEdge) {
                        val d = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                        if (d < minDist) minDist = d
                    }
                }
            }
            if (minDist == Float.MAX_VALUE) minDist = 0f
            val normalized = ((minDist + searchRadius) / (2f * searchRadius)).coerceIn(0f, 1f)
            val v = (normalized * 255).toInt()
            out.setPixel(x, y, Color.argb(v, v, v, v))
        }
    }
    return out
}
```

---

## 附录 B：相关文档链接
- `Android_OpenGL_Text_Rendering_Analysis.md`：当前项目的文字绘制方案分析（保留）
- `SDF_统一指南.md`：本文（SDF 相关内容整合后保留）

---

最后更新时间：2025-01
