package com.houtrry.common_map.text

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import com.houtrry.common_map.data.*
import com.houtrry.common_map.utils.toBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 复杂文字渲染器 (OpenGL ES 2.0)
 * 专门处理包含图标、气泡等复杂显示元素的文字渲染
 */
class ComplexTextRenderer(context: Context) : BaseTextRenderer(context) {
    
    companion object {
        private const val TAG = "ComplexTextRenderer"
        
        // Canvas渲染器着色器（复用CanvasTextRenderer的着色器）
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 texCoord;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                texCoord = vTexCoord;
            }
        """
        
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform float uAlpha;
            varying vec2 texCoord;
            
            void main() {
                vec4 color = texture2D(uTexture, texCoord);
                gl_FragColor = vec4(color.rgb, color.a * uAlpha);
            }
        """
    }
    
    private var shaderProgram = 0
    private var mvpMatrixHandle = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0
    private var alphaHandle = 0
    
    // 顶点数据
    private val vertices = floatArrayOf(
        -1f,  1f, 0f,  // 左上
        -1f, -1f, 0f,  // 左下
         1f, -1f, 0f,  // 右下
         1f,  1f, 0f   // 右上
    )
    
    private val textureCoords = floatArrayOf(
        0f, 0f,  // 左上
        0f, 1f,  // 左下
        1f, 1f,  // 右下
        1f, 0f   // 右上
    )
    
    private val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
    
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer
    
    // 复杂文字纹理缓存
    private val complexTextureCache = mutableMapOf<String, ComplexTextureInfo>()
    private val maxCacheSize = 50 // 复杂文字缓存较少，因为占用内存更大
    
    override fun initialize() {
        if (isInitialized) return
        
        try {
            // 初始化缓冲区
            vertexBuffer = vertices.toBuffer()
            texCoordBuffer = textureCoords.toBuffer()
            indexBuffer = indices.toBuffer()
            
            // 创建着色器程序
            shaderProgram = createShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            
            // 获取着色器句柄
            mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
            positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
            texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoord")
            textureHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture")
            alphaHandle = GLES20.glGetUniformLocation(shaderProgram, "uAlpha")
            
            isInitialized = true
            Log.d(TAG, "复杂文字渲染器初始化成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "复杂文字渲染器初始化失败", e)
            throw e
        }
    }
    
    override fun render(mvpMatrix: FloatArray) {
        if (!isInitialized || textInfos.isEmpty()) return
        
        val startTime = System.currentTimeMillis()
        
        GLES20.glUseProgram(shaderProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        // 设置顶点数据
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        
        // 逐个渲染每个复杂文字
        textInfos.values.forEach { textInfo ->
            renderSingleComplexText(textInfo, mvpMatrix)
        }
        
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glUseProgram(0)
        
        lastRenderTime = (System.currentTimeMillis() - startTime).toFloat()
    }
    
    private fun renderSingleComplexText(textInfo: TextInfo, mvpMatrix: FloatArray) {
        val textureInfo = getOrCreateComplexTexture(textInfo)
        
        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureInfo.textureId)
        GLES20.glUniform1i(textureHandle, 0)
        
        // 设置MVP矩阵
        val textMVPMatrix = calculateTextMVPMatrix(textInfo, mvpMatrix)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, textMVPMatrix, 0)
        
        // 设置透明度
        GLES20.glUniform1f(alphaHandle, textInfo.style.alpha)
        
        // 绘制
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
    
    private fun getOrCreateComplexTexture(textInfo: TextInfo): ComplexTextureInfo {
        return complexTextureCache.getOrPut(textInfo.id) {
            createComplexTexture(textInfo)
        }
    }
    
    /**
     * 创建复杂文字纹理（包含文字、图标、气泡等）
     */
    private fun createComplexTexture(textInfo: TextInfo): ComplexTextureInfo {
        when (textInfo.displayType) {
            TextDisplayType.PURE_TEXT -> {
                return createPureTextTexture(textInfo)
            }
            TextDisplayType.ICON_ABOVE_TEXT,
            TextDisplayType.ICON_LEFT_TEXT,
            TextDisplayType.ICON_RIGHT_TEXT,
            TextDisplayType.ICON_BELOW_TEXT -> {
                return createIconTextTexture(textInfo)
            }
            TextDisplayType.BUBBLE_TEXT_ONLY -> {
                return createBubbleTextTexture(textInfo)
            }
            TextDisplayType.BUBBLE_TEXT_WITH_ICON -> {
                return createBubbleIconTextTexture(textInfo)
            }
        }
    }
    
    /**
     * 创建纯文字纹理
     */
    private fun createPureTextTexture(textInfo: TextInfo): ComplexTextureInfo {
        val paint = createTextPaint(textInfo.style)
        
        val multiLineInfo = if (textInfo.needsLineWrap()) {
            // 支持换行的多行文字
            com.houtrry.common_map.utils.TextMeasureUtils.measureMultiLineText(
                textInfo.content, textInfo.style, context
            )
        } else {
            // 单行文字
            val singleLineResult = com.houtrry.common_map.utils.TextMeasureUtils.measureSingleLineText(textInfo.content, paint)
            MultiLineTextInfo(
                lines = listOf(textInfo.content),
                totalWidth = singleLineResult.lineWidths.first(),
                totalHeight = singleLineResult.lineHeights.first(),
                lineSpacing = textInfo.style.lineSpacing
            )
        }
        
        val width = (multiLineInfo.totalWidth + 20).toInt().coerceAtLeast(64)
        val height = (multiLineInfo.totalHeight + 20).toInt().coerceAtLeast(32)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制多行文字（支持RTL）
        drawMultiLineText(canvas, textInfo, multiLineInfo, paint, width, height)
        
        return createOpenGLTexture(bitmap, width, height)
    }
    
    /**
     * 创建图标+文字纹理
     */
    private fun createIconTextTexture(textInfo: TextInfo): ComplexTextureInfo {
        val iconInfo = textInfo.iconInfo ?: throw IllegalArgumentException("IconInfo不能为空")
        val paint = createTextPaint(textInfo.style)
        val textBounds = measureText(textInfo.content, paint)
        
        // 计算布局尺寸
        val layoutSize = calculateIconTextLayout(textInfo.displayType, iconInfo, textBounds)
        
        val bitmap = Bitmap.createBitmap(layoutSize.width, layoutSize.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制图标和文字
        drawIconAndText(canvas, textInfo, paint, layoutSize, textBounds)
        
        return createOpenGLTexture(bitmap, layoutSize.width, layoutSize.height)
    }
    
    /**
     * 创建气泡+文字纹理
     */
    private fun createBubbleTextTexture(textInfo: TextInfo): ComplexTextureInfo {
        val bubbleInfo = textInfo.bubbleInfo ?: throw IllegalArgumentException("BubbleInfo不能为空")
        val paint = createTextPaint(textInfo.style)
        val textBounds = measureText(textInfo.content, paint)
        
        // 计算气泡尺寸
        val bubbleSize = calculateBubbleSize(textBounds, bubbleInfo)
        
        val bitmap = Bitmap.createBitmap(bubbleSize.width, bubbleSize.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制气泡
        drawBubble(canvas, bubbleInfo, bubbleSize)
        
        // 绘制文字
        drawTextInBubble(canvas, textInfo.content, paint, bubbleInfo, bubbleSize, textBounds)
        
        return createOpenGLTexture(bitmap, bubbleSize.width, bubbleSize.height)
    }
    
    /**
     * 创建气泡+图标+文字纹理
     */
    private fun createBubbleIconTextTexture(textInfo: TextInfo): ComplexTextureInfo {
        val bubbleInfo = textInfo.bubbleInfo ?: throw IllegalArgumentException("BubbleInfo不能为空")
        val iconInfo = textInfo.iconInfo ?: throw IllegalArgumentException("IconInfo不能为空")
        val paint = createTextPaint(textInfo.style)
        val textBounds = measureText(textInfo.content, paint)
        
        // 计算复杂布局尺寸
        val layoutSize = calculateComplexBubbleLayout(textBounds, iconInfo, bubbleInfo)
        
        val bitmap = Bitmap.createBitmap(layoutSize.width, layoutSize.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制气泡
        drawBubble(canvas, bubbleInfo, layoutSize)
        
        // 绘制图标和文字
        drawIconAndTextInBubble(canvas, textInfo, paint, layoutSize, textBounds)
        
        return createOpenGLTexture(bitmap, layoutSize.width, layoutSize.height)
    }
    
    // =========================== 辅助绘制方法 ===========================
    
    private fun createTextPaint(style: TextStyle): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = style.fontSize * context.resources.displayMetrics.scaledDensity
            color = style.textColor
            typeface = style.typeface
        }
    }
    
    private fun measureText(text: String, paint: Paint): Rect {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds
    }
    
    private fun drawText(canvas: Canvas, text: String, paint: Paint, width: Int, height: Int, textBounds: Rect) {
        val x = width / 2f
        val y = height / 2f - textBounds.exactCenterY()
        
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, x, y, paint)
    }
    
    /**
     * 绘制多行文字（支持RTL和换行）
     */
    private fun drawMultiLineText(
        canvas: Canvas,
        textInfo: TextInfo,
        multiLineInfo: MultiLineTextInfo,
        paint: Paint,
        containerWidth: Int,
        containerHeight: Int
    ) {
        val isRTL = textInfo.isRTL()
        val actualAlign = com.houtrry.common_map.utils.TextMeasureUtils.getActualAlignment(textInfo.style, isRTL)
        
        // 计算绘制位置
        val positions = com.houtrry.common_map.utils.TextMeasureUtils.calculateDrawPositions(
            multiLineInfo, 
            containerWidth.toFloat(),
            containerHeight.toFloat(),
            textInfo.style,
            isRTL
        )
        
        // 设置画笔对齐方式
        paint.textAlign = when (actualAlign) {
            TextAlign.LEFT -> Paint.Align.LEFT
            TextAlign.RIGHT -> Paint.Align.RIGHT
            TextAlign.CENTER -> Paint.Align.CENTER
            else -> Paint.Align.CENTER
        }
        
        // 逐行绘制文字
        multiLineInfo.lines.forEachIndexed { index, line ->
            if (index < positions.size) {
                val (x, y) = positions[index]
                
                if (isRTL && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    // RTL文字特殊处理
                    canvas.drawTextRun(
                        line, 0, line.length,
                        0, line.length,
                        x, y,
                        isRTL, paint
                    )
                } else {
                    // 标准文字绘制
                    canvas.drawText(line, x, y, paint)
                }
            }
        }
    }
    
    private fun drawBubble(canvas: Canvas, bubbleInfo: BubbleInfo, size: BubbleSize) {
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bubbleInfo.style.backgroundColor
            style = Paint.Style.FILL
        }
        
        // 绘制气泡背景
        val rect = RectF(0f, 0f, size.width.toFloat(), size.height.toFloat())
        canvas.drawRoundRect(rect, bubbleInfo.cornerRadius, bubbleInfo.cornerRadius, bubblePaint)
        
        // 绘制气泡边框
        if (bubbleInfo.style.borderWidth > 0) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bubbleInfo.style.borderColor
                style = Paint.Style.STROKE
                strokeWidth = bubbleInfo.style.borderWidth
            }
            canvas.drawRoundRect(rect, bubbleInfo.cornerRadius, bubbleInfo.cornerRadius, borderPaint)
        }
        
        // 绘制阴影（简化实现）
        bubbleInfo.style.shadowColor?.let { shadowColor ->
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = shadowColor
                style = Paint.Style.FILL
                maskFilter = BlurMaskFilter(bubbleInfo.style.shadowRadius, BlurMaskFilter.Blur.NORMAL)
            }
            val shadowRect = RectF(2f, 2f, size.width.toFloat() + 2f, size.height.toFloat() + 2f)
            canvas.drawRoundRect(shadowRect, bubbleInfo.cornerRadius, bubbleInfo.cornerRadius, shadowPaint)
        }
    }
    
    private fun calculateIconTextLayout(displayType: TextDisplayType, iconInfo: IconInfo, textBounds: Rect): BubbleSize {
        return when (displayType) {
            TextDisplayType.ICON_ABOVE_TEXT, TextDisplayType.ICON_BELOW_TEXT -> {
                BubbleSize(
                    width = maxOf(iconInfo.width, textBounds.width()) + 20,
                    height = iconInfo.height + textBounds.height() + 30
                )
            }
            TextDisplayType.ICON_LEFT_TEXT, TextDisplayType.ICON_RIGHT_TEXT -> {
                BubbleSize(
                    width = iconInfo.width + textBounds.width() + 30,
                    height = maxOf(iconInfo.height, textBounds.height()) + 20
                )
            }
            else -> BubbleSize(textBounds.width() + 20, textBounds.height() + 20)
        }
    }
    
    private fun calculateBubbleSize(textBounds: Rect, bubbleInfo: BubbleInfo): BubbleSize {
        val padding = bubbleInfo.padding
        return BubbleSize(
            width = textBounds.width() + (padding.left + padding.right).toInt() + 10,
            height = textBounds.height() + (padding.top + padding.bottom).toInt() + 10
        )
    }
    
    private fun calculateComplexBubbleLayout(textBounds: Rect, iconInfo: IconInfo, bubbleInfo: BubbleInfo): BubbleSize {
        val padding = bubbleInfo.padding
        val contentWidth = iconInfo.width + textBounds.width() + 10 // 图标和文字间距
        val contentHeight = maxOf(iconInfo.height, textBounds.height())
        
        return BubbleSize(
            width = contentWidth + (padding.left + padding.right).toInt(),
            height = contentHeight + (padding.top + padding.bottom).toInt()
        )
    }
    
    private fun drawIconAndText(canvas: Canvas, textInfo: TextInfo, paint: Paint, layoutSize: BubbleSize, textBounds: Rect) {
        // 简化实现：这里需要根据displayType具体布局图标和文字
        // 实际实现中需要详细的位置计算
        val iconInfo = textInfo.iconInfo!!
        
        when (textInfo.displayType) {
            TextDisplayType.ICON_ABOVE_TEXT -> {
                // 图标在上，文字在下
                val iconX = (layoutSize.width - iconInfo.width) / 2
                val iconY = 10
                val textX = layoutSize.width / 2f
                val textY = iconY + iconInfo.height + 20 - textBounds.exactCenterY()
                
                // 绘制图标（这里需要实际的图标资源处理）
                drawIcon(canvas, iconInfo, iconX, iconY)
                
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(textInfo.content, textX, textY, paint)
            }
            // 其他布局类型...
            else -> {
                // 默认处理
                drawText(canvas, textInfo.content, paint, layoutSize.width, layoutSize.height, textBounds)
            }
        }
    }
    
    private fun drawTextInBubble(canvas: Canvas, text: String, paint: Paint, bubbleInfo: BubbleInfo, bubbleSize: BubbleSize, textBounds: Rect) {
        val padding = bubbleInfo.padding
        val x = bubbleSize.width / 2f
        val y = bubbleSize.height / 2f - textBounds.exactCenterY()
        
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, x, y, paint)
    }
    
    private fun drawIconAndTextInBubble(canvas: Canvas, textInfo: TextInfo, paint: Paint, layoutSize: BubbleSize, textBounds: Rect) {
        // 复杂布局：在气泡中同时绘制图标和文字
        val iconInfo = textInfo.iconInfo!!
        val bubbleInfo = textInfo.bubbleInfo!!
        val padding = bubbleInfo.padding
        
        // 简化布局：图标在左，文字在右
        val iconX = padding.left.toInt()
        val iconY = (layoutSize.height - iconInfo.height) / 2
        val textX = iconX + iconInfo.width + 10f
        val textY = layoutSize.height / 2f - textBounds.exactCenterY()
        
        drawIcon(canvas, iconInfo, iconX, iconY)
        
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(textInfo.content, textX, textY, paint)
    }
    
    private fun drawIcon(canvas: Canvas, iconInfo: IconInfo, x: Int, y: Int) {
        // 简化实现：绘制一个占位图标
        // 实际实现中需要加载真实的图标资源
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = iconInfo.tint ?: Color.GRAY
            style = Paint.Style.FILL
        }
        
        val rect = RectF(x.toFloat(), y.toFloat(), 
            (x + iconInfo.width).toFloat(), (y + iconInfo.height).toFloat())
        canvas.drawOval(rect, iconPaint)
    }
    
    private fun createOpenGLTexture(bitmap: Bitmap, width: Int, height: Int): ComplexTextureInfo {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        
        // 更新内存使用统计
        memoryUsage += (width * height * 4)
        
        Log.d(TAG, "创建复杂文字纹理，尺寸: ${width}x${height}")
        
        return ComplexTextureInfo(textureId, width, height)
    }
    
    private fun calculateTextMVPMatrix(textInfo: TextInfo, baseMVPMatrix: FloatArray): FloatArray {
        // 简化实现，实际使用时需要根据textInfo.position计算实际的MVP矩阵
        return baseMVPMatrix
    }
    
    private fun createShaderProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("复杂文字着色器程序链接失败: $error")
        }
        
        return program
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("复杂文字着色器编译失败: $error")
        }
        
        return shader
    }
    
    override fun onTextAdded(textInfo: TextInfo) {
        // 清理缓存如果超过限制
        if (complexTextureCache.size >= maxCacheSize) {
            clearOldestTextures()
        }
        Log.d(TAG, "添加复杂文字: ${textInfo.content}, 类型: ${textInfo.displayType}")
    }
    
    override fun onTextsAdded(textInfos: List<TextInfo>) {
        Log.d(TAG, "批量添加复杂文字: ${textInfos.size}个")
    }
    
    override fun onTextRemoved(textInfo: TextInfo) {
        complexTextureCache.remove(textInfo.id)?.let { textureInfo ->
            GLES20.glDeleteTextures(1, intArrayOf(textureInfo.textureId), 0)
            memoryUsage -= (textureInfo.width * textureInfo.height * 4)
            Log.d(TAG, "删除复杂文字纹理: ${textInfo.content}")
        }
    }
    
    override fun onAllTextsCleared() {
        complexTextureCache.values.forEach { textureInfo ->
            GLES20.glDeleteTextures(1, intArrayOf(textureInfo.textureId), 0)
        }
        complexTextureCache.clear()
        memoryUsage = 0L
        Log.d(TAG, "清空所有复杂文字纹理")
    }
    
    private fun clearOldestTextures() {
        val toRemove = complexTextureCache.keys.take(maxCacheSize / 2)
        toRemove.forEach { textId ->
            complexTextureCache.remove(textId)?.let { textureInfo ->
                GLES20.glDeleteTextures(1, intArrayOf(textureInfo.textureId), 0)
                memoryUsage -= (textureInfo.width * textureInfo.height * 4)
            }
        }
    }
    
    override fun release() {
        onAllTextsCleared()
        
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }
        
        isInitialized = false
        Log.d(TAG, "复杂文字渲染器已释放")
    }
    
    /**
     * 复杂文字纹理信息
     */
    private data class ComplexTextureInfo(
        val textureId: Int,
        val width: Int,
        val height: Int
    )
    
    /**
     * 气泡/布局尺寸
     */
    private data class BubbleSize(
        val width: Int,
        val height: Int
    )
} 