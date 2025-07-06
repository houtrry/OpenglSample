# Signed Distance Field (SDF) 技术详解与OpenGL ES应用

## 1. SDF技术概述

### 1.1 什么是SDF？

Signed Distance Field（有符号距离场）是一种表示几何形状的技术，它将每个像素的值定义为该像素到最近边界的距离，并根据像素在形状内部还是外部赋予正负号。

```
内部像素：正值（距离边界的距离）
边界像素：0值
外部像素：负值（距离边界的距离）
```

### 1.2 SDF的优势

1. **高质量缩放**：无论放大多少倍，边缘都保持平滑
2. **内存效率**：一个SDF纹理可以表示多种字体大小
3. **实时渲染**：GPU可以快速处理SDF数据
4. **抗锯齿**：自动提供平滑的边缘

## 2. SDF生成原理

### 2.1 距离计算

```cpp
// 伪代码：计算像素到边界的距离
float calculateDistance(int x, int y, Bitmap source) {
    float minDistance = MAX_FLOAT;
    
    // 搜索半径内的所有像素
    for (int dy = -radius; dy <= radius; dy++) {
        for (int dx = -radius; dx <= radius; dx++) {
            int nx = x + dx;
            int ny = y + dy;
            
            if (isEdgePixel(nx, ny, source)) {
                float distance = sqrt(dx*dx + dy*dy);
                minDistance = min(minDistance, distance);
            }
        }
    }
    
    return minDistance;
}
```

### 2.2 SDF生成流程

```
原始文字图像 → 边缘检测 → 距离场计算 → SDF纹理
     ↓              ↓           ↓           ↓
   [文字]        [边缘]      [距离值]    [SDF纹理]
```

## 3. OpenGL ES中的SDF实现

### 3.1 顶点着色器

```glsl
// SDF顶点着色器
attribute vec4 vPosition;
attribute vec2 vTexCoord;
uniform mat4 uMVPMatrix;

varying vec2 texCoord;

void main() {
    gl_Position = uMVPMatrix * vPosition;
    texCoord = vTexCoord;
}
```

### 3.2 片段着色器

```glsl
// SDF片段着色器
precision mediump float;

uniform sampler2D uFontAtlas;
uniform vec4 uTextColor;
uniform float uSmoothness; // 平滑度参数

varying vec2 texCoord;

void main() {
    // 从SDF纹理读取距离值
    float distance = texture2D(uFontAtlas, texCoord).r;
    
    // 使用smoothstep进行平滑插值
    float alpha = smoothstep(0.5 - uSmoothness, 0.5 + uSmoothness, distance);
    
    // 输出最终颜色
    gl_FragColor = vec4(uTextColor.rgb, alpha * uTextColor.a);
}
```

### 3.3 SDF纹理生成代码

```kotlin
class SDFGenerator {
    
    fun generateSDFText(text: String, width: Int, height: Int): Bitmap {
        // 1. 创建原始文字位图
        val sourceBitmap = createTextBitmap(text, width, height)
        
        // 2. 生成SDF
        val sdfBitmap = generateSDF(sourceBitmap, 10) // 搜索半径10像素
        
        return sdfBitmap
    }
    
    private fun generateSDF(sourceBitmap: Bitmap, searchRadius: Int): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val sdfBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val pixel = pixels[index]
                val alpha = Color.alpha(pixel)
                
                // 判断是内部点还是外部点
                val isInside = alpha > 128
                
                // 计算到边界的距离
                val distance = calculateDistanceToEdge(pixels, x, y, width, height, isInside, searchRadius)
                
                // 转换为SDF值 (0-1范围)
                val sdfValue = ((distance + searchRadius) / (2 * searchRadius)).coerceIn(0f, 1f)
                val sdfByte = (sdfValue * 255).toInt()
                
                sdfBitmap.setPixel(x, y, Color.argb(sdfByte, sdfByte, sdfByte, sdfByte))
            }
        }
        
        return sdfBitmap
    }
    
    private fun calculateDistanceToEdge(
        pixels: IntArray, 
        x: Int, 
        y: Int, 
        width: Int, 
        height: Int, 
        isInside: Boolean, 
        searchRadius: Int
    ): Float {
        var minDistance = Float.MAX_VALUE
        
        for (dy in -searchRadius..searchRadius) {
            for (dx in -searchRadius..searchRadius) {
                val nx = x + dx
                val ny = y + dy
                
                if (nx in 0 until width && ny in 0 until height) {
                    val index = ny * width + nx
                    val pixel = pixels[index]
                    val alpha = Color.alpha(pixel)
                    
                    // 判断是否为边界
                    val isEdge = if (isInside) alpha <= 128 else alpha > 128
                    
                    if (isEdge) {
                        val distance = sqrt((dx * dx + dy * dy).toFloat())
                        if (distance < minDistance) {
                            minDistance = distance
                        }
                    }
                }
            }
        }
        
        return if (minDistance == Float.MAX_VALUE) 0f else minDistance
    }
}
```

## 4. SDF在OpenGL ES中的应用

### 4.1 字体纹理图集

```kotlin
class FontAtlas {
    private val atlasSize = 1024
    private val charSize = 64
    private val charsPerRow = atlasSize / charSize
    
    fun createFontAtlas(): Int {
        val atlasBitmap = Bitmap.createBitmap(atlasSize, atlasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(atlasBitmap)
        canvas.drawColor(Color.TRANSPARENT)
        
        val sdfGenerator = SDFGenerator()
        
        // 为每个字符生成SDF
        for (i in 0 until 256) { // ASCII字符
            val char = i.toChar()
            val x = (i % charsPerRow) * charSize
            val y = (i / charsPerRow) * charSize
            
            val charSDF = sdfGenerator.generateSDFText(char.toString(), charSize, charSize)
            canvas.drawBitmap(charSDF, x.toFloat(), y.toFloat(), null)
            charSDF.recycle()
        }
        
        // 上传到GPU
        val textureId = uploadToGPU(atlasBitmap)
        atlasBitmap.recycle()
        
        return textureId
    }
}
```

### 4.2 批量文字渲染

```kotlin
class SDFTextRenderer {
    
    fun renderTextBatch(texts: List<TextInfo>) {
        GLES20.glUseProgram(sdfProgram)
        
        // 设置SDF参数
        GLES20.glUniform1f(smoothnessHandle, 0.1f) // 平滑度
        GLES20.glUniform4f(textColorHandle, 1f, 1f, 1f, 1f) // 文字颜色
        
        // 批量渲染所有文字
        for (textInfo in texts) {
            renderSingleText(textInfo)
        }
    }
    
    private fun renderSingleText(textInfo: TextInfo) {
        // 计算文字位置和UV坐标
        val vertices = calculateTextVertices(textInfo)
        
        // 设置顶点数据
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, vertices)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, vertices.apply { position(2) })
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}
```

## 5. SDF性能优化

### 5.1 多级SDF

```kotlin
class MultiLevelSDF {
    private val sdfLevels = mutableMapOf<Int, Int>() // 字体大小 -> 纹理ID
    
    fun getSDFForSize(fontSize: Int): Int {
        // 找到最接近的预生成SDF级别
        val level = findNearestLevel(fontSize)
        return sdfLevels[level] ?: generateSDFLevel(level)
    }
    
    private fun findNearestLevel(targetSize: Int): Int {
        return sdfLevels.keys.minByOrNull { abs(it - targetSize) } ?: targetSize
    }
}
```

### 5.2 SDF缓存策略

```kotlin
class SDFCache {
    private val cache = LruCache<String, Int>(100) // 缓存100个SDF纹理
    
    fun getSDFTexture(text: String, fontSize: Int): Int {
        val key = "$text-$fontSize"
        return cache.get(key) ?: generateAndCache(key, text, fontSize)
    }
    
    private fun generateAndCache(key: String, text: String, fontSize: Int): Int {
        val textureId = generateSDFTexture(text, fontSize)
        cache.put(key, textureId)
        return textureId
    }
}
```

## 6. SDF效果对比

### 6.1 传统位图字体 vs SDF字体

| 特性 | 传统位图字体 | SDF字体 |
|------|-------------|---------|
| 缩放质量 | 放大时像素化 | 任意缩放保持平滑 |
| 内存占用 | 每个尺寸一个纹理 | 一个纹理支持多种尺寸 |
| 渲染性能 | 较快 | 稍慢（需要smoothstep计算） |
| 边缘质量 | 固定 | 高质量抗锯齿 |

### 6.2 视觉效果对比

```
传统位图字体放大效果：
████████████████████████████████
████████████████████████████████
████████████████████████████████
████████████████████████████████

SDF字体放大效果：
████████████████████████████████
████████████████████████████████
████████████████████████████████
████████████████████████████████
```

## 7. 实际应用示例

### 7.1 游戏UI文字渲染

```kotlin
class GameUIRenderer {
    private val sdfTextRenderer = SDFTextRenderer()
    
    fun renderUI() {
        // 渲染分数
        sdfTextRenderer.renderText(TextInfo("Score: 12345", 100f, 50f, 24f))
        
        // 渲染生命值
        sdfTextRenderer.renderText(TextInfo("HP: 100", 100f, 80f, 20f))
        
        // 渲染游戏状态
        sdfTextRenderer.renderText(TextInfo("PAUSED", 400f, 300f, 48f))
    }
}
```

### 7.2 地图标注文字

```kotlin
class MapLabelRenderer {
    fun renderMapLabels(labels: List<MapLabel>) {
        for (label in labels) {
            // 根据缩放级别调整字体大小
            val fontSize = calculateFontSize(label.importance, currentZoom)
            
            // 渲染SDF文字
            sdfTextRenderer.renderText(
                TextInfo(label.text, label.x, label.y, fontSize)
            )
        }
    }
}
```

## 8. 总结

SDF技术为OpenGL ES中的文字渲染提供了高质量的解决方案：

1. **高质量**：任意缩放保持平滑边缘
2. **高效**：一个纹理支持多种字体大小
3. **灵活**：支持实时颜色和效果调整
4. **实用**：特别适合移动设备和游戏应用

通过合理使用SDF技术，可以在保持渲染性能的同时，获得高质量的文字显示效果。 