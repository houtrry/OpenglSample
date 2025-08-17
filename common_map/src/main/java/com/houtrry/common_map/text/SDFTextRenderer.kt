package com.houtrry.common_map.text

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import com.houtrry.common_map.data.TextInfo
import com.houtrry.common_map.data.SDFCharInfo
import com.houtrry.common_map.utils.toBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * SDF文字渲染器实现 (OpenGL ES 2.0)
 * 使用Signed Distance Field技术，适合静态文字的高性能渲染
 */
class SDFTextRenderer(context: Context) : BaseTextRenderer(context) {
    
    companion object {
        private const val TAG = "SDFTextRenderer"
        
        // SDF着色器代码 (ES 2.0版本)
        private const val SDF_VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 texCoord;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                texCoord = vTexCoord;
            }
        """
        
        private const val SDF_FRAGMENT_SHADER = """
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
        """
    }
    
    private var sdfShaderProgram = 0
    private var sdfMvpMatrixHandle = 0
    private var sdfPositionHandle = 0
    private var sdfTexCoordHandle = 0
    private var sdfFontAtlasHandle = 0
    private var sdfTextColorHandle = 0
    private var sdfSmoothnessHandle = 0
    
    // SDF纹理图集
    private var sdfAtlasTexture = 0
    private var atlasWidth = 0
    private var atlasHeight = 0
    private val charInfoMap = mutableMapOf<Char, SDFCharInfo>()
    
    // 顶点数据
    private val quadVertices = floatArrayOf(
        -0.5f,  0.5f, 0f,  // 左上
        -0.5f, -0.5f, 0f,  // 左下
         0.5f, -0.5f, 0f,  // 右下
         0.5f,  0.5f, 0f   // 右上
    )
    
    private val quadTexCoords = floatArrayOf(
        0f, 0f,  // 左上
        0f, 1f,  // 左下
        1f, 1f,  // 右下
        1f, 0f   // 右上
    )
    
    private val quadIndices = shortArrayOf(0, 1, 2, 0, 2, 3)
    
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var texCoordBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer
    
    override fun initialize() {
        if (isInitialized) return
        
        try {
            // 初始化缓冲区
            vertexBuffer = quadVertices.toBuffer()
            texCoordBuffer = quadTexCoords.toBuffer() 
            indexBuffer = quadIndices.toBuffer()
            
            // 创建SDF着色器程序
            sdfShaderProgram = createShaderProgram(SDF_VERTEX_SHADER, SDF_FRAGMENT_SHADER)
            
            // 获取着色器句柄
            sdfMvpMatrixHandle = GLES20.glGetUniformLocation(sdfShaderProgram, "uMVPMatrix")
            sdfPositionHandle = GLES20.glGetAttribLocation(sdfShaderProgram, "vPosition")
            sdfTexCoordHandle = GLES20.glGetAttribLocation(sdfShaderProgram, "vTexCoord")
            sdfFontAtlasHandle = GLES20.glGetUniformLocation(sdfShaderProgram, "uFontAtlas")
            sdfTextColorHandle = GLES20.glGetUniformLocation(sdfShaderProgram, "uTextColor")
            sdfSmoothnessHandle = GLES20.glGetUniformLocation(sdfShaderProgram, "uSmoothness")
            
            // 生成默认字符集的SDF图集
            generateDefaultSdfAtlas()
            
            isInitialized = true
            Log.d(TAG, "SDF渲染器初始化成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "SDF渲染器初始化失败", e)
            throw e
        }
    }
    
    override fun render(mvpMatrix: FloatArray) {
        if (!isInitialized || textInfos.isEmpty() || sdfAtlasTexture == 0) return
        
        val startTime = System.currentTimeMillis()
        
        GLES20.glUseProgram(sdfShaderProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        // 绑定SDF图集纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sdfAtlasTexture)
        GLES20.glUniform1i(sdfFontAtlasHandle, 0)
        
        // 设置顶点属性
        GLES20.glEnableVertexAttribArray(sdfPositionHandle)
        GLES20.glVertexAttribPointer(sdfPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        
        GLES20.glEnableVertexAttribArray(sdfTexCoordHandle)
        GLES20.glVertexAttribPointer(sdfTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        
        // 设置平滑度
        GLES20.glUniform1f(sdfSmoothnessHandle, 0.1f)
        
        // 逐个渲染文字（ES 2.0不支持实例化渲染）
        textInfos.values.forEach { textInfo ->
            renderSingleSDFText(textInfo, mvpMatrix)
        }
        
        GLES20.glDisableVertexAttribArray(sdfPositionHandle)
        GLES20.glDisableVertexAttribArray(sdfTexCoordHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glUseProgram(0)
        
        lastRenderTime = (System.currentTimeMillis() - startTime).toFloat()
    }
    
    private fun renderSingleSDFText(textInfo: TextInfo, mvpMatrix: FloatArray) {
        // 设置文字颜色
        val color = textInfo.style.textColor
        val red = Color.red(color) / 255f
        val green = Color.green(color) / 255f
        val blue = Color.blue(color) / 255f
        GLES20.glUniform4f(sdfTextColorHandle, red, green, blue, textInfo.style.alpha)
        
        // 为文字中的每个字符渲染
        textInfo.content.forEachIndexed { index, char ->
            charInfoMap[char]?.let { charInfo ->
                renderSingleCharacter(textInfo, charInfo, index, mvpMatrix)
            }
        }
    }
    
    private fun renderSingleCharacter(textInfo: TextInfo, charInfo: SDFCharInfo, charIndex: Int, mvpMatrix: FloatArray) {
        // 计算字符在图集中的纹理坐标
        val u1 = charInfo.atlasX.toFloat() / atlasWidth
        val v1 = charInfo.atlasY.toFloat() / atlasHeight
        val u2 = (charInfo.atlasX + charInfo.width).toFloat() / atlasWidth
        val v2 = (charInfo.atlasY + charInfo.height).toFloat() / atlasHeight
        
        // 更新纹理坐标
        val charTexCoords = floatArrayOf(
            u1, v1,  // 左上
            u1, v2,  // 左下
            u2, v2,  // 右下
            u2, v1   // 右上
        )
        
        val charTexBuffer = charTexCoords.toBuffer()
        GLES20.glVertexAttribPointer(sdfTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, charTexBuffer)
        
        // 计算字符的MVP矩阵（包含位置偏移）
        val charMVPMatrix = calculateCharacterMVPMatrix(textInfo, charInfo, charIndex, mvpMatrix)
        GLES20.glUniformMatrix4fv(sdfMvpMatrixHandle, 1, false, charMVPMatrix, 0)
        
        // 绘制字符
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, quadIndices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
    
    private fun calculateCharacterMVPMatrix(textInfo: TextInfo, charInfo: SDFCharInfo, charIndex: Int, baseMVPMatrix: FloatArray): FloatArray {
        // 简化实现：这里需要根据字符位置、文字位置等计算实际的MVP矩阵
        // 实际使用时需要考虑字符间距、文字对齐等因素
        return baseMVPMatrix
    }
    
    /**
     * 生成默认字符集的SDF图集
     */
    private fun generateDefaultSdfAtlas() {
        // 基础字符集
        val basicChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,!?;:()[]{}+-*/%=<>@#$%^&"
        
        generateSdfAtlas(basicChars.toSet())
    }
    
    /**
     * 为指定字符集生成SDF图集
     */
    fun generateSdfAtlas(characters: Set<Char>) {
        Log.d(TAG, "开始生成SDF图集，字符数量: ${characters.size}")
        
        val charSize = 64 // 每个字符64x64像素
        val padding = 4   // 字符间间距
        val charsPerRow = 16 // 每行字符数
        
        atlasWidth = charsPerRow * (charSize + padding)
        atlasHeight = ((characters.size + charsPerRow - 1) / charsPerRow) * (charSize + padding)
        
        // 创建图集Bitmap
        val atlasBitmap = Bitmap.createBitmap(atlasWidth, atlasHeight, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(atlasBitmap)
        canvas.drawColor(Color.BLACK) // 背景黑色
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        
        charInfoMap.clear()
        characters.forEachIndexed { index, char ->
            val row = index / charsPerRow
            val col = index % charsPerRow
            val x = col * (charSize + padding) + charSize / 2
            val y = row * (charSize + padding) + charSize / 2
            
            // 创建单个字符的SDF纹理
            val charBitmap = createCharacterSDF(char, charSize, paint)
            canvas.drawBitmap(charBitmap, (x - charSize / 2).toFloat(), (y - charSize / 2).toFloat(), null)
            charBitmap.recycle()
            
            // 记录字符信息
            charInfoMap[char] = SDFCharInfo(
                character = char,
                atlasX = col * (charSize + padding),
                atlasY = row * (charSize + padding),
                width = charSize,
                height = charSize
            )
        }
        
        // 创建OpenGL纹理
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        sdfAtlasTexture = textureIds[0]
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sdfAtlasTexture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, atlasBitmap, 0)
        
        atlasBitmap.recycle()
        
        // 更新内存使用统计
        memoryUsage = (atlasWidth * atlasHeight).toLong()
        
        Log.d(TAG, "SDF图集生成完成，尺寸: ${atlasWidth}x${atlasHeight}")
    }
    
    private fun createCharacterSDF(char: Char, size: Int, paint: Paint): Bitmap {
        // 先创建高分辨率的字符位图
        val highRes = size * 2
        val highResBitmap = Bitmap.createBitmap(highRes, highRes, Bitmap.Config.ARGB_8888)
        val highResCanvas = Canvas(highResBitmap)
        
        val highResPaint = Paint(paint).apply {
            textSize = paint.textSize * 1.5f
        }
        
        highResCanvas.drawText(char.toString(), highRes / 2f, highRes / 2f, highResPaint)
        
        // 生成SDF
        val sdfBitmap = generateSimpleSDF(highResBitmap, size)
        highResBitmap.recycle()
        
        return sdfBitmap
    }
    
    private fun generateSimpleSDF(sourceBitmap: Bitmap, targetSize: Int): Bitmap {
        val sdfBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ALPHA_8)
        val sourceWidth = sourceBitmap.width
        val sourceHeight = sourceBitmap.height
        val scaleX = sourceWidth.toFloat() / targetSize
        val scaleY = sourceHeight.toFloat() / targetSize
        val searchRadius = 8f
        
        val sourcePixels = IntArray(sourceWidth * sourceHeight)
        sourceBitmap.getPixels(sourcePixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight)
        
        val sdfPixels = ByteArray(targetSize * targetSize)
        
        for (y in 0 until targetSize) {
            for (x in 0 until targetSize) {
                val sourceX = (x * scaleX).toInt()
                val sourceY = (y * scaleY).toInt()
                
                val distance = findMinDistance(sourceX, sourceY, sourcePixels, sourceWidth, sourceHeight, searchRadius)
                val normalizedDistance = ((distance / searchRadius) * 127f + 128f)
                    .coerceIn(0f, 255f)
                    .toInt()
                sdfPixels[y * targetSize + x] = normalizedDistance.toByte()
            }
        }
        
        sdfBitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(sdfPixels))
        return sdfBitmap
    }
    
    private fun findMinDistance(x: Int, y: Int, pixels: IntArray, width: Int, height: Int, maxRadius: Float): Float {
        val centerAlpha = Color.alpha(pixels.getOrNull(y * width + x) ?: 0)
        val isInside = centerAlpha > 128
        var minDistance = maxRadius
        
        val searchRadius = maxRadius.toInt()
        for (dy in -searchRadius..searchRadius) {
            for (dx in -searchRadius..searchRadius) {
                val nx = x + dx
                val ny = y + dy
                
                if (nx in 0 until width && ny in 0 until height) {
                    val pixelAlpha = Color.alpha(pixels[ny * width + nx])
                    val pixelInside = pixelAlpha > 128
                    
                    if (isInside != pixelInside) {
                        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toFloat())
                        minDistance = minDistance.coerceAtMost(distance)
                    }
                }
            }
        }
        
        return if (isInside) minDistance else -minDistance
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
            throw RuntimeException("SDF着色器程序链接失败: $error")
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
            throw RuntimeException("SDF着色器编译失败: $error")
        }
        
        return shader
    }
    
    /**
     * 检查字符是否在SDF图集中
     */
    fun hasCharacter(char: Char): Boolean = charInfoMap.containsKey(char)
    
    /**
     * 检查文字是否完全支持（所有字符都在图集中）
     */
    fun supportsText(text: String): Boolean {
        // 对复杂脚本/Emoji/合字做保守回退：遇到非BMP或组合标记等则返回false
        text.forEach { ch ->
            val type = Character.getType(ch)
            if (Character.isSurrogate(ch) || type == Character.NON_SPACING_MARK.toInt() || type == Character.OTHER_SYMBOL.toInt()) {
                return false
            }
            if (!hasCharacter(ch)) return false
        }
        return true
    }
    
    override fun onTextAdded(textInfo: TextInfo) {
        Log.d(TAG, "添加SDF文字: ${textInfo.content}")
    }
    
    override fun onTextsAdded(textInfos: List<TextInfo>) {
        Log.d(TAG, "批量添加SDF文字: ${textInfos.size}个")
    }
    
    override fun onTextRemoved(textInfo: TextInfo) {
        Log.d(TAG, "删除SDF文字: ${textInfo.content}")
    }
    
    override fun onAllTextsCleared() {
        Log.d(TAG, "清空所有SDF文字")
    }
    
    override fun release() {
        if (sdfAtlasTexture != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(sdfAtlasTexture), 0)
            sdfAtlasTexture = 0
        }
        
        if (sdfShaderProgram != 0) {
            GLES20.glDeleteProgram(sdfShaderProgram)
            sdfShaderProgram = 0
        }
        
        charInfoMap.clear()
        isInitialized = false
        Log.d(TAG, "SDF渲染器已释放")
    }
} 