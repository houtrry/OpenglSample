package com.houtrry.common_map.text

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import com.houtrry.common_map.data.TextInfo
import com.houtrry.common_map.utils.toBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Canvas文字渲染器实现
 * 使用Android Canvas API生成文字纹理，适合动态文字和复杂排版
 */
class CanvasTextRenderer(context: Context) : BaseTextRenderer(context) {
    
    companion object {
        private const val TAG = "CanvasTextRenderer"
        
        // OpenGL着色器代码
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
    
    // 文字纹理缓存
    private val textureCache = mutableMapOf<String, TextureInfo>()
    private val maxCacheSize = 100 // 最大缓存数量
    
    override fun initialize() {
        if (isInitialized) return
        
        try {
            // 检查OpenGL ES上下文是否可用
            if (!isOpenGLContextAvailable()) {
                val error = "OpenGL ES上下文不可用，请确保在GLSurfaceView.onSurfaceCreated()或之后调用"
                Log.e(TAG, error)
                throw RuntimeException(error)
            }
            
            Log.d(TAG, "开始初始化Canvas渲染器...")
            
            // 初始化缓冲区
            vertexBuffer = vertices.toBuffer()
            texCoordBuffer = textureCoords.toBuffer()
            indexBuffer = indices.toBuffer()
            
            Log.d(TAG, "缓冲区初始化完成")
            
            // 创建着色器程序
            shaderProgram = createShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            
            // 获取着色器句柄
            mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
            positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
            texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoord")
            textureHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture")
            alphaHandle = GLES20.glGetUniformLocation(shaderProgram, "uAlpha")
            
            // 验证着色器句柄
            validateShaderHandles()
            
            isInitialized = true
            Log.d(TAG, "Canvas渲染器初始化成功，程序ID: $shaderProgram")
            
        } catch (e: Exception) {
            Log.e(TAG, "Canvas渲染器初始化失败", e)
            // 清理部分初始化的资源
            cleanup()
            throw e
        }
    }
    
    /**
     * 检查OpenGL ES上下文是否可用
     */
    private fun isOpenGLContextAvailable(): Boolean {
        return try {
            // 尝试获取OpenGL版本字符串
            val version = GLES20.glGetString(GLES20.GL_VERSION)
            val glError = GLES20.glGetError()
            
            if (glError != GLES20.GL_NO_ERROR) {
                Log.w(TAG, "检查OpenGL上下文时发现GL错误: $glError")
                false
            } else if (version == null) {
                Log.w(TAG, "无法获取OpenGL版本信息")
                false
            } else {
                Log.d(TAG, "OpenGL版本: $version")
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "检查OpenGL上下文时发生异常: ${e.message}")
            false
        }
    }
    
    /**
     * 验证着色器句柄是否有效
     */
    private fun validateShaderHandles() {
        val handles = mapOf(
            "uMVPMatrix" to mvpMatrixHandle,
            "vPosition" to positionHandle,
            "vTexCoord" to texCoordHandle,
            "uTexture" to textureHandle,
            "uAlpha" to alphaHandle
        )
        
        handles.forEach { (name, handle) ->
            if (handle < 0) {
                Log.w(TAG, "着色器句柄获取失败: $name = $handle")
            } else {
                Log.d(TAG, "着色器句柄: $name = $handle")
            }
        }
        
        // 至少position和texture句柄必须有效
        if (positionHandle < 0 || textureHandle < 0) {
            throw RuntimeException("关键着色器句柄无效: position=$positionHandle, texture=$textureHandle")
        }
    }
    
    /**
     * 清理部分初始化的资源
     */
    private fun cleanup() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }
        isInitialized = false
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
        
        // 逐个渲染每个文字
        textInfos.values.forEach { textInfo ->
            renderSingleText(textInfo, mvpMatrix)
        }
        
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glUseProgram(0)
        
        lastRenderTime = (System.currentTimeMillis() - startTime).toFloat()
    }
    
    private fun renderSingleText(textInfo: TextInfo, mvpMatrix: FloatArray) {
        val textureInfo = getOrCreateTexture(textInfo)
        
        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureInfo.textureId)
        GLES20.glUniform1i(textureHandle, 0)
        
        // 设置MVP矩阵（这里需要根据文字位置调整）
        val textMVPMatrix = calculateTextMVPMatrix(textInfo, mvpMatrix)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, textMVPMatrix, 0)
        
        // 设置透明度
        GLES20.glUniform1f(alphaHandle, textInfo.style.alpha)
        
        // 绘制
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
    
    private fun getOrCreateTexture(textInfo: TextInfo): TextureInfo {
        return textureCache.getOrPut(textInfo.id) {
            createTextTexture(textInfo)
        }
    }
    
    private fun createTextTexture(textInfo: TextInfo): TextureInfo {
        // 创建Paint对象
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textInfo.style.fontSize * context.resources.displayMetrics.scaledDensity
            color = textInfo.style.textColor
            typeface = textInfo.style.typeface
        }
        
        // 测量文字尺寸
        val textBounds = Rect()
        paint.getTextBounds(textInfo.content, 0, textInfo.content.length, textBounds)
        
        val width = (textBounds.width() + 20).coerceAtLeast(64) // 最小64像素宽度
        val height = (textBounds.height() + 20).coerceAtLeast(32) // 最小32像素高度
        
        // 创建Bitmap并绘制文字
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 绘制文字（居中对齐）
        val x = width / 2f
        val y = height / 2f - textBounds.exactCenterY()
        
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(textInfo.content, x, y, paint)
        
        // 创建OpenGL纹理
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        
        // 回收Bitmap
        bitmap.recycle()
        
        // 更新内存使用统计
        memoryUsage += (width * height * 4) // ARGB_8888 = 4字节/像素
        
        Log.d(TAG, "创建文字纹理: ${textInfo.content}, 尺寸: ${width}x${height}")
        
        return TextureInfo(textureId, width, height)
    }
    
    private fun calculateTextMVPMatrix(textInfo: TextInfo, baseMVPMatrix: FloatArray): FloatArray {
        // 这里需要根据textInfo.position计算实际的MVP矩阵
        // 简化实现，实际使用时需要根据地图坐标系进行转换
        return baseMVPMatrix
    }
    
    private fun createShaderProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        try {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            
            val program = GLES20.glCreateProgram()
            if (program == 0) {
                val glError = GLES20.glGetError()
                Log.e(TAG, "创建着色器程序失败, GL错误码: $glError")
                throw RuntimeException("创建着色器程序失败: GL错误码 $glError")
            }
            
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            
            // 检查链接状态
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(program)
                val glError = GLES20.glGetError()
                
                Log.e(TAG, "========== 着色器程序链接失败 ==========")
                Log.e(TAG, "链接错误信息: $error")
                Log.e(TAG, "GL错误码: $glError")
                Log.e(TAG, "=====================================")
                
                GLES20.glDeleteProgram(program)
                GLES20.glDeleteShader(vertexShader)
                GLES20.glDeleteShader(fragmentShader)
                
                val detailedError = "着色器程序链接失败\n" +
                        "错误信息: $error\n" +
                        "GL错误码: $glError\n" +
                        "可能原因：顶点着色器和片段着色器之间的varying变量不匹配"
                
                throw RuntimeException(detailedError)
            }
            
            // 清理着色器对象（程序已链接完成）
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            
            Log.d(TAG, "着色器程序创建成功，程序ID: $program")
            return program
            
        } catch (e: Exception) {
            Log.e(TAG, "创建着色器程序过程中发生异常", e)
            throw e
        }
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shaderTypeStr = if (type == GLES20.GL_VERTEX_SHADER) "顶点着色器" else "片段着色器"
        
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            val glError = GLES20.glGetError()
            Log.e(TAG, "创建着色器失败, type=$type, GL错误码: $glError")
            throw RuntimeException("创建${shaderTypeStr}失败: GL错误码 $glError")
        }
        
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        // 检查编译状态
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            val glError = GLES20.glGetError()
            
            Log.e(TAG, "========== ${shaderTypeStr}编译失败 ==========")
            Log.e(TAG, "错误信息: $error")
            Log.e(TAG, "GL错误码: $glError")
            Log.e(TAG, "着色器代码:")
            shaderCode.lines().forEachIndexed { index, line ->
                Log.e(TAG, "${(index + 1).toString().padStart(3, ' ')}: $line")
            }
            Log.e(TAG, "=======================================")
            
            GLES20.glDeleteShader(shader)
            
            val detailedError = "${shaderTypeStr}编译失败\n" +
                    "错误信息: $error\n" +
                    "GL错误码: $glError\n" +
                    "请检查OpenGL ES 2.0兼容性"
            
            throw RuntimeException(detailedError)
        }
        
        Log.d(TAG, "${shaderTypeStr}编译成功")
        return shader
    }
    
    override fun onTextAdded(textInfo: TextInfo) {
        // 清理缓存如果超过限制
        if (textureCache.size >= maxCacheSize) {
            clearOldestTextures()
        }
        Log.d(TAG, "添加文字: ${textInfo.content}")
    }
    
    override fun onTextsAdded(textInfos: List<TextInfo>) {
        Log.d(TAG, "批量添加文字: ${textInfos.size}个")
    }
    
    override fun onTextRemoved(textInfo: TextInfo) {
        textureCache.remove(textInfo.id)?.let { textureInfo ->
            // 删除OpenGL纹理
            GLES20.glDeleteTextures(1, intArrayOf(textureInfo.textureId), 0)
            memoryUsage -= (textureInfo.width * textureInfo.height * 4)
            Log.d(TAG, "删除文字纹理: ${textInfo.content}")
        }
    }
    
    override fun onAllTextsCleared() {
        // 清理所有纹理缓存
        textureCache.values.forEach { textureInfo ->
            GLES20.glDeleteTextures(1, intArrayOf(textureInfo.textureId), 0)
        }
        textureCache.clear()
        memoryUsage = 0L
        Log.d(TAG, "清空所有文字纹理")
    }
    
    private fun clearOldestTextures() {
        // 简化实现：清理一半缓存
        val toRemove = textureCache.keys.take(maxCacheSize / 2)
        toRemove.forEach { textId ->
            textureCache.remove(textId)?.let { textureInfo ->
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
        Log.d(TAG, "Canvas渲染器已释放")
    }
    
    /**
     * 纹理信息数据类
     */
    private data class TextureInfo(
        val textureId: Int,
        val width: Int,
        val height: Int
    )
} 