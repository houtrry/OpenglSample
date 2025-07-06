package com.houtrry.openglsample.activity

import android.annotation.SuppressLint
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.houtrry.common_map.utils.dp
import com.houtrry.common_map.utils.sp
import com.houtrry.openglsample.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.sqrt

class OptimizedTestActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "OptimizedTestActivity"
        private const val MAX_MARKERS = 3000
        private const val BATCH_SIZE = 100
    }

    // 批量渲染着色器
    private val batchVertexShaderCode = """
    uniform mat4 uMVPMatrix;
    attribute vec4 vPosition;
    attribute vec2 vTexCoord;
    attribute vec4 vInstanceData; // x,y:位置, z:纹理索引, w:缩放
    varying vec2 texCoord;
    varying float vTextureIndex;
    
    void main() {
        vec4 worldPos = vec4(vInstanceData.xy, 0.0, 1.0);
        gl_Position = uMVPMatrix * (worldPos + vPosition * vInstanceData.w);
        texCoord = vTexCoord;
        vTextureIndex = vInstanceData.z;
    }
""".trimIndent()

    private val batchFragmentShaderCode = """
    precision mediump float;
    uniform sampler2D uTextureAtlas;
    uniform vec2 uAtlasSize;
    varying vec2 texCoord;
    varying float vTextureIndex;
    
    void main() {
        vec2 atlasCoord = texCoord / uAtlasSize;
        atlasCoord.x += mod(vTextureIndex, uAtlasSize.x) / uAtlasSize.x;
        atlasCoord.y += floor(vTextureIndex / uAtlasSize.x) / uAtlasSize.y;
        gl_FragColor = texture2D(uTextureAtlas, atlasCoord);
    }
""".trimIndent()

    // 文字渲染着色器（SDF技术）
    private val textVertexShaderCode = """
    uniform mat4 uMVPMatrix;
    attribute vec4 vPosition;
    attribute vec2 vTexCoord;
    attribute vec4 vInstanceData; // x,y:位置, z:文字索引, w:缩放
    varying vec2 texCoord;
    varying float vTextIndex;
    
    void main() {
        vec4 worldPos = vec4(vInstanceData.xy, 0.0, 1.0);
        gl_Position = uMVPMatrix * (worldPos + vPosition * vInstanceData.w);
        texCoord = vTexCoord;
        vTextIndex = vInstanceData.z;
    }
""".trimIndent()

    private val textFragmentShaderCode = """
    precision mediump float;
    uniform sampler2D uFontAtlas;
    uniform vec2 uFontAtlasSize;
    uniform vec4 uTextColor;
    varying vec2 texCoord;
    varying float vTextIndex;
    
    void main() {
        vec2 atlasCoord = texCoord / uFontAtlasSize;
        atlasCoord.x += mod(vTextIndex, uFontAtlasSize.x) / uFontAtlasSize.x;
        atlasCoord.y += floor(vTextIndex / uFontAtlasSize.x) / uFontAtlasSize.y;
        
        float distance = texture2D(uFontAtlas, atlasCoord).r;
        float alpha = smoothstep(0.4, 0.6, distance);
        gl_FragColor = vec4(uTextColor.rgb, alpha * uTextColor.a);
    }
""".trimIndent()

    // 基础着色器（地图渲染）
    private val vertexShaderCode = """
    uniform mat4 uMVPMatrix;
    attribute vec4 vPosition;
    attribute vec2 vTexCoord;
    varying vec2 texCoord;
    
    void main() {
        gl_Position = uMVPMatrix * vPosition;
        texCoord = vTexCoord;
    }
""".trimIndent()

    private val fragmentShaderCode = """
    precision mediump float;
    uniform sampler2D uTexture;
    varying vec2 texCoord;
    
    void main() {
        gl_FragColor = texture2D(uTexture, texCoord);
    }
""".trimIndent()

    private lateinit var glSurfaceView: GLSurfaceView
    private val viewMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val projectionMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val modelMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val mvpMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    // 手势相关
    private var previousPointF = PointF()
    private var previousAngle = 0f
    private var initialDistance = 0f
    private var viewWidth = 0
    private var viewHeight = 0

    // 着色器程序
    private var mapProgram = -1
    private var batchProgram = -1
    private var textProgram = -1

    // 批量渲染相关
    private var markerVBO = -1
    private var markerInstanceVBO = -1
    private var textVBO = -1
    private var textInstanceVBO = -1
    private var markerTextureAtlas = -1
    private var fontTextureAtlas = -1
    private val markerInstanceData = FloatArray(MAX_MARKERS * 4)
    private val textInstanceData = FloatArray(MAX_MARKERS * 4)
    private var markerCount = 0
    private var textCount = 0

    // 地图相关
    private var mapTextureId = -1
    private lateinit var mapVertices: FloatArray
    private val mapSize = Point(0, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(this@OptimizedTestActivity)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
        setContentView(glSurfaceView)
        setupGestureListeners()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        initShaders()
        initBatchRendering()
        loadMapTexture()
        createTextureAtlases()
        generateTestMarkers()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)
        Matrix.orthoM(projectionMatrix, 0, -width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f, -1f, 1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawMap()
        drawMarkersBatch()
        drawTextsBatch()
    }

    private fun initShaders() {
        // 地图着色器
        mapProgram = createProgram(vertexShaderCode, fragmentShaderCode)
        // 批量渲染着色器
        batchProgram = createProgram(batchVertexShaderCode, batchFragmentShaderCode)
        // 文字渲染着色器
        textProgram = createProgram(textVertexShaderCode, textFragmentShaderCode)
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)
        
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun initBatchRendering() {
        val markerVertices = floatArrayOf(
            -0.5f, -0.5f, 0f, 1f,  // 左下
            0.5f, -0.5f, 1f, 1f,  // 右下
            -0.5f, 0.5f, 0f, 0f,  // 左上
            0.5f, 0.5f, 1f, 0f   // 右上
        )

        val vboIds = IntArray(4)
        GLES20.glGenBuffers(4, vboIds, 0)
        markerVBO = vboIds[0]
        markerInstanceVBO = vboIds[1]
        textVBO = vboIds[2]
        textInstanceVBO = vboIds[3]

        // 设置顶点数据
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, markerVBO)
        val buffer = ByteBuffer.allocateDirect(markerVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(markerVertices)
                position(0)
            }
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, markerVertices.size * 4, buffer, GLES20.GL_STATIC_DRAW)

        // 设置实例数据缓冲区
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, markerInstanceVBO)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, markerInstanceData.size * 4, null, GLES20.GL_DYNAMIC_DRAW)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textVBO)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, markerVertices.size * 4, buffer, GLES20.GL_STATIC_DRAW)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textInstanceVBO)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textInstanceData.size * 4, null, GLES20.GL_DYNAMIC_DRAW)
    }

    private fun loadMapTexture() {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.optemap_217k)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            mapSize.x = bitmap.width
            mapSize.y = bitmap.height
            bitmap.recycle()
            
            mapVertices = floatArrayOf(
                -bitmap.width * 0.5f, -bitmap.height * 0.5f, 0f, 1f,
                bitmap.width * 0.5f, -bitmap.height * 0.5f, 1f, 1f,
                -bitmap.width * 0.5f, bitmap.height * 0.5f, 0f, 0f,
                bitmap.width * 0.5f, bitmap.height * 0.5f, 1f, 0f
            )

            mapTextureId = textureHandle[0]
        }
    }

    private fun createTextureAtlases() {
        // 创建Marker纹理图集
        createMarkerTextureAtlas()
        // 创建字体纹理图集
        createFontTextureAtlas()
    }

    private fun createMarkerTextureAtlas() {
        val atlasSize = 1024
        val atlasBitmap = Bitmap.createBitmap(atlasSize, atlasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(atlasBitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val iconResources = listOf(R.mipmap.robot, R.mipmap.icon_start_point, R.mipmap.icon_target)
        val iconsPerRow = 3
        val iconSize = atlasSize / iconsPerRow

        iconResources.forEachIndexed { index, resId ->
            val bitmap = BitmapFactory.decodeResource(resources, resId)
            val x = (index % iconsPerRow) * iconSize
            val y = (index / iconsPerRow) * iconSize
            
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
            canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
            
            bitmap.recycle()
            scaledBitmap.recycle()
        }

        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        markerTextureAtlas = textureHandle[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, markerTextureAtlas)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, atlasBitmap, 0)
        
        atlasBitmap.recycle()
    }

    private fun createFontTextureAtlas() {
        val atlasSize = 512
        val atlasBitmap = Bitmap.createBitmap(atlasSize, atlasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(atlasBitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val texts = listOf("初始点0", "初始点1", "重生之我在初始点2", "位置3", "位置4", "位置5")
        val charsPerRow = 6
        val charSize = atlasSize / charsPerRow

        texts.forEachIndexed { index, text ->
            val x = (index % charsPerRow) * charSize
            val y = (index / charsPerRow) * charSize
            
            val textBitmap = createSDFText(text, charSize, charSize)
            canvas.drawBitmap(textBitmap, x.toFloat(), y.toFloat(), null)
            textBitmap.recycle()
        }

        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        fontTextureAtlas = textureHandle[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fontTextureAtlas)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, atlasBitmap, 0)
        
        atlasBitmap.recycle()
    }

    private fun createSDFText(text: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = height * 0.6f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(text, width / 2f, height * 0.7f, paint)
        return generateSimpleSDF(bitmap)
    }

    private fun generateSimpleSDF(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val sdfBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val pixel = pixels[index]
                val alpha = Color.alpha(pixel)
                
                val distance = if (alpha > 128) {
                    calculateDistanceToEdge(pixels, x, y, width, height, false)
                } else {
                    -calculateDistanceToEdge(pixels, x, y, width, height, true)
                }
                
                val sdfValue = ((distance + 10) / 20 * 255).toInt().coerceIn(0, 255)
                sdfBitmap.setPixel(x, y, Color.argb(sdfValue, sdfValue, sdfValue, sdfValue))
            }
        }
        
        return sdfBitmap
    }

    private fun calculateDistanceToEdge(pixels: IntArray, x: Int, y: Int, width: Int, height: Int, isOutside: Boolean): Float {
        val searchRadius = 10
        var minDistance = Float.MAX_VALUE
        
        for (dy in -searchRadius..searchRadius) {
            for (dx in -searchRadius..searchRadius) {
                val nx = x + dx
                val ny = y + dy
                
                if (nx in 0 until width && ny in 0 until height) {
                    val index = ny * width + nx
                    val pixel = pixels[index]
                    val alpha = Color.alpha(pixel)
                    
                    val isEdge = if (isOutside) alpha > 128 else alpha <= 128
                    
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

    private fun generateTestMarkers() {
        markerCount = 0
        textCount = 0
        
        // 生成2500个测试Marker
        for (i in 0 until 2500) {
            val x = (Math.random() * 2000 - 1000).toFloat()
            val y = (Math.random() * 2000 - 1000).toFloat()
            val iconIndex = (i % 3).toFloat()
            val textIndex = (i % 6).toFloat()
            
            // Marker实例数据
            val markerIndex = markerCount * 4
            markerInstanceData[markerIndex] = x
            markerInstanceData[markerIndex + 1] = y
            markerInstanceData[markerIndex + 2] = iconIndex
            markerInstanceData[markerIndex + 3] = 50f
            
            // 文字实例数据
            val textIndex2 = textCount * 4
            textInstanceData[textIndex2] = x
            textInstanceData[textIndex2 + 1] = y - 80f
            textInstanceData[textIndex2 + 2] = textIndex
            textInstanceData[textIndex2 + 3] = 30f
            
            markerCount++
            textCount++
        }
        
        updateInstanceBuffers()
    }

    private fun updateInstanceBuffers() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, markerInstanceVBO)
        val markerBuffer = ByteBuffer.allocateDirect(markerCount * 4 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(markerInstanceData, 0, markerCount * 4)
                position(0)
            }
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, markerCount * 4 * 4, markerBuffer)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textInstanceVBO)
        val textBuffer = ByteBuffer.allocateDirect(textCount * 4 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(textInstanceData, 0, textCount * 4)
                position(0)
            }
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, textCount * 4 * 4, textBuffer)
    }

    private fun drawMap() {
        GLES20.glUseProgram(mapProgram)

        val positionHandle = GLES20.glGetAttribLocation(mapProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(mapProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(mapProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(mapProgram, "uTexture")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        val vertexBuffer = ByteBuffer.allocateDirect(mapVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(mapVertices)
                position(0)
            }

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer.apply { position(2) })

        val mvpMatrix = mvpMatrix.identityM()
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glUseProgram(0)
    }

    private fun drawMarkersBatch() {
        GLES20.glUseProgram(batchProgram)

        val positionHandle = GLES20.glGetAttribLocation(batchProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(batchProgram, "vTexCoord")
        val instanceDataHandle = GLES20.glGetAttribLocation(batchProgram, "vInstanceData")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(batchProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(batchProgram, "uTextureAtlas")
        val atlasSizeHandle = GLES20.glGetUniformLocation(batchProgram, "uAtlasSize")

        // 设置顶点属性
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, markerVBO)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, 0)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, 8)

        // 设置实例属性
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, markerInstanceVBO)
        GLES20.glEnableVertexAttribArray(instanceDataHandle)
        GLES20.glVertexAttribPointer(instanceDataHandle, 4, GLES20.GL_FLOAT, false, 16, 0)
        GLES20.glVertexAttribDivisor(instanceDataHandle, 1)

        // MVP矩阵
        val mvpMatrix = mvpMatrix.identityM()
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 纹理图集
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, markerTextureAtlas)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glUniform2f(atlasSizeHandle, 3f, 1f)

        // 混合
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 批量绘制
        GLES20.glDrawArraysInstanced(GLES20.GL_TRIANGLE_STRIP, 0, 4, markerCount)

        // 清理
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glDisableVertexAttribArray(instanceDataHandle)
        GLES20.glVertexAttribDivisor(instanceDataHandle, 0)
        GLES20.glUseProgram(0)
    }

    private fun drawTextsBatch() {
        GLES20.glUseProgram(textProgram)

        val positionHandle = GLES20.glGetAttribLocation(textProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(textProgram, "vTexCoord")
        val instanceDataHandle = GLES20.glGetAttribLocation(textProgram, "vInstanceData")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(textProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(textProgram, "uFontAtlas")
        val atlasSizeHandle = GLES20.glGetUniformLocation(textProgram, "uFontAtlasSize")
        val textColorHandle = GLES20.glGetUniformLocation(textProgram, "uTextColor")

        // 设置顶点属性
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textVBO)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, 0)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, 8)

        // 设置实例属性
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textInstanceVBO)
        GLES20.glEnableVertexAttribArray(instanceDataHandle)
        GLES20.glVertexAttribPointer(instanceDataHandle, 4, GLES20.GL_FLOAT, false, 16, 0)
        GLES20.glVertexAttribDivisor(instanceDataHandle, 1)

        // MVP矩阵
        val mvpMatrix = mvpMatrix.identityM()
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 字体纹理图集
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fontTextureAtlas)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glUniform2f(atlasSizeHandle, 6f, 1f)
        GLES20.glUniform4f(textColorHandle, 1f, 1f, 1f, 1f)

        // 混合
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 批量绘制
        GLES20.glDrawArraysInstanced(GLES20.GL_TRIANGLE_STRIP, 0, 4, textCount)

        // 清理
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glDisableVertexAttribArray(instanceDataHandle)
        GLES20.glVertexAttribDivisor(instanceDataHandle, 0)
        GLES20.glUseProgram(0)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureListeners() {
        glSurfaceView.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    previousPointF = PointF(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        val dx = event.getX(1) - event.getX(0)
                        val dy = event.getY(1) - event.getY(0)
                        initialDistance = sqrt(dx * dx + dy * dy)
                        previousAngle = atan2(-dy.toDouble(), dx.toDouble()).toFloat()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        val point = PointF(event.x, event.y)
                        translateMap(point.x - previousPointF.x, point.y - previousPointF.y)
                        previousPointF = point
                        glSurfaceView.requestRender()
                    } else if (event.pointerCount == 2) {
                        val x1 = event.getX(0)
                        val y1 = event.getY(0)
                        val x2 = event.getX(1)
                        val y2 = event.getY(1)

                        val pivot = convertScreenToGL((x1 + x2) / 2, (y1 + y2) / 2)

                        val dx = x2 - x1
                        val dy = y2 - y1
                        val currentAngle = atan2(-dy.toDouble(), dx.toDouble()).toFloat()
                        val rotationAngle = if (previousAngle != 0f) {
                            Math.toDegrees((currentAngle - previousAngle).toDouble()).toFloat()
                        } else {
                            0f
                        }
                        previousAngle = currentAngle

                        val currentDistance = sqrt(dx * dx + dy * dy)
                        val scaleFactor = (currentDistance / initialDistance)
                        initialDistance = currentDistance
                        updateViewMatrix(pivot, rotationAngle, scaleFactor)
                        glSurfaceView.requestRender()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    previousAngle = 0f
                    initialDistance = 0f
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount == 2) {
                        val remainIndex = if (event.actionIndex == 0) 1 else 0
                        previousPointF = PointF(event.getX(remainIndex), event.getY(remainIndex))
                        previousAngle = 0f
                        initialDistance = 0f
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun translateMap(dx: Float, dy: Float) {
        synchronized(modelMatrix) {
            val p0 = convertScreenToGL(0f, 0f)
            val pxy = convertScreenToGL(dx, dy)
            Matrix.translateM(modelMatrix, 0, pxy.x - p0.x, pxy.y - p0.y, 0f)
        }
    }

    private fun convertScreenToGL(screenX: Float, screenY: Float): PointF {
        val tempMatrix = FloatArray(16).identityM()
        val invertedMatrix = FloatArray(16).identityM()
        val ndcX = screenX / (viewWidth * 0.5f) - 1.0f
        val ndcY = 1.0f - screenY / (viewHeight * 0.5f)

        Matrix.multiplyMM(tempMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
        Matrix.invertM(invertedMatrix, 0, tempMatrix, 0)

        val inVec = floatArrayOf(ndcX, ndcY, 0f, 1f)
        val outVec = FloatArray(4)
        Matrix.multiplyMV(outVec, 0, invertedMatrix, 0, inVec, 0)

        if (outVec[3] != 0f) {
            outVec[0] /= outVec[3]
            outVec[1] /= outVec[3]
        }

        return PointF(outVec[0], outVec[1])
    }

    private fun updateViewMatrix(pivot: PointF, rotate: Float, scale: Float) {
        synchronized(modelMatrix) {
            Matrix.translateM(modelMatrix, 0, pivot.x, pivot.y, 0f)
            Matrix.rotateM(modelMatrix, 0, rotate, 0f, 0f, 1f)
            Matrix.scaleM(modelMatrix, 0, scale, scale, 1f)
            Matrix.translateM(modelMatrix, 0, -pivot.x, -pivot.y, 0f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理OpenGL资源
        GLES20.glDeleteBuffers(4, intArrayOf(markerVBO, markerInstanceVBO, textVBO, textInstanceVBO), 0)
        GLES20.glDeleteTextures(3, intArrayOf(mapTextureId, markerTextureAtlas, fontTextureAtlas), 0)
    }
} 