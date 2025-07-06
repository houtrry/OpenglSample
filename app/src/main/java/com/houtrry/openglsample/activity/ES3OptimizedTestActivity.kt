package com.houtrry.openglsample.activity

import android.annotation.SuppressLint
import android.graphics.*
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.houtrry.common_map.utils.dp
import com.houtrry.common_map.utils.sp
import com.houtrry.lopengles20.utils.identityM
import com.houtrry.openglsample.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.sqrt

class ES3OptimizedTestActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ES3OptimizedTestActivity"
        private const val MAX_MARKERS = 3000
    }

    // OpenGL ES 3.0 实例渲染着色器
    private val es3VertexShaderCode = """
    #version 300 es
    uniform mat4 uMVPMatrix;
    in vec4 vPosition;
    in vec2 vTexCoord;
    in vec4 vInstanceData; // x,y:位置, z:纹理索引, w:缩放
    out vec2 texCoord;
    out float vTextureIndex;
    
    void main() {
        vec4 worldPos = vec4(vInstanceData.xy, 0.0, 1.0);
        gl_Position = uMVPMatrix * (worldPos + vPosition * vInstanceData.w);
        texCoord = vTexCoord;
        vTextureIndex = vInstanceData.z;
    }
""".trimIndent()

    private val es3FragmentShaderCode = """
    #version 300 es
    precision mediump float;
    uniform sampler2D uTextureAtlas;
    uniform vec2 uAtlasSize;
    in vec2 texCoord;
    in float vTextureIndex;
    out vec4 fragColor;
    
    void main() {
        vec2 atlasCoord = texCoord / uAtlasSize;
        atlasCoord.x += mod(vTextureIndex, uAtlasSize.x) / uAtlasSize.x;
        atlasCoord.y += floor(vTextureIndex / uAtlasSize.x) / uAtlasSize.y;
        fragColor = texture(uTextureAtlas, atlasCoord);
    }
""".trimIndent()

    // 文字渲染着色器（SDF技术）- ES3版本
    private val es3TextVertexShaderCode = """
    #version 300 es
    uniform mat4 uMVPMatrix;
    in vec4 vPosition;
    in vec2 vTexCoord;
    in vec4 vInstanceData; // x,y:位置, z:文字索引, w:缩放
    out vec2 texCoord;
    out float vTextIndex;
    
    void main() {
        vec4 worldPos = vec4(vInstanceData.xy, 0.0, 1.0);
        gl_Position = uMVPMatrix * (worldPos + vPosition * vInstanceData.w);
        texCoord = vTexCoord;
        vTextIndex = vInstanceData.z;
    }
""".trimIndent()

    private val es3TextFragmentShaderCode = """
    #version 300 es
    precision mediump float;
    uniform sampler2D uFontAtlas;
    uniform vec2 uFontAtlasSize;
    uniform vec4 uTextColor;
    in vec2 texCoord;
    in float vTextIndex;
    out vec4 fragColor;
    
    void main() {
        vec2 atlasCoord = texCoord / uFontAtlasSize;
        atlasCoord.x += mod(vTextIndex, uFontAtlasSize.x) / uFontAtlasSize.x;
        atlasCoord.y += floor(vTextIndex / uFontAtlasSize.x) / uFontAtlasSize.y;
        
        float distance = texture(uFontAtlas, atlasCoord).r;
        float alpha = smoothstep(0.4, 0.6, distance);
        fragColor = vec4(uTextColor.rgb, alpha * uTextColor.a);
    }
""".trimIndent()

    // 基础着色器（地图渲染）- ES3版本
    private val es3MapVertexShaderCode = """
    #version 300 es
    uniform mat4 uMVPMatrix;
    in vec4 vPosition;
    in vec2 vTexCoord;
    out vec2 texCoord;
    
    void main() {
        gl_Position = uMVPMatrix * vPosition;
        texCoord = vTexCoord;
    }
""".trimIndent()

    private val es3MapFragmentShaderCode = """
    #version 300 es
    precision mediump float;
    uniform sampler2D uTexture;
    in vec2 texCoord;
    out vec4 fragColor;
    
    void main() {
        fragColor = texture(uTexture, texCoord);
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
    private var markerProgram = -1
    private var textProgram = -1

    // OpenGL ES 3.0 实例渲染相关
    private var markerVAO = -1
    private var markerVBO = -1
    private var markerInstanceVBO = -1
    private var textVAO = -1
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
            setEGLContextClientVersion(3) // 使用OpenGL ES 3.0
            setRenderer(this@ES3OptimizedTestActivity)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
        setContentView(glSurfaceView)
        setupGestureListeners()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        initShaders()
        initES3Rendering()
        loadMapTexture()
        createTextureAtlases()
        generateTestMarkers()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES30.glViewport(0, 0, width, height)
        Matrix.orthoM(projectionMatrix, 0, -width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f, -1f, 1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawMap()
        drawMarkersES3()
        drawTextsES3()
    }

    private fun initShaders() {
        // 地图着色器
        mapProgram = createProgram(es3MapVertexShaderCode, es3MapFragmentShaderCode)
        // Marker实例渲染着色器
        markerProgram = createProgram(es3VertexShaderCode, es3FragmentShaderCode)
        // 文字渲染着色器
        textProgram = createProgram(es3TextVertexShaderCode, es3TextFragmentShaderCode)
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)
        
        return GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }
    }

    private fun initES3Rendering() {
        val markerVertices = floatArrayOf(
            -0.5f, -0.5f, 0f, 1f,  // 左下
            0.5f, -0.5f, 1f, 1f,  // 右下
            -0.5f, 0.5f, 0f, 0f,  // 左上
            0.5f, 0.5f, 1f, 0f   // 右上
        )

        // 创建Marker VAO和VBO
        val vaoIds = IntArray(2)
        val vboIds = IntArray(4)
        GLES30.glGenVertexArrays(2, vaoIds, 0)
        GLES30.glGenBuffers(4, vboIds, 0)
        
        markerVAO = vaoIds[0]
        textVAO = vaoIds[1]
        markerVBO = vboIds[0]
        markerInstanceVBO = vboIds[1]
        textVBO = vboIds[2]
        textInstanceVBO = vboIds[3]

        // 设置Marker VAO
        GLES30.glBindVertexArray(markerVAO)
        
        // 顶点数据
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, markerVBO)
        val buffer = ByteBuffer.allocateDirect(markerVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(markerVertices)
                position(0)
            }
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, markerVertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)

        // 顶点属性
        GLES30.glEnableVertexAttribArray(0) // vPosition
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1) // vTexCoord
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)

        // 实例数据
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, markerInstanceVBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, markerInstanceData.size * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(2) // vInstanceData
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glVertexAttribDivisor(2, 1) // 每个实例更新一次

        // 设置Text VAO
        GLES30.glBindVertexArray(textVAO)
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, textVBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, markerVertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)

        GLES30.glEnableVertexAttribArray(0) // vPosition
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1) // vTexCoord
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, textInstanceVBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, textInstanceData.size * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(2) // vInstanceData
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glVertexAttribDivisor(2, 1) // 每个实例更新一次

        GLES30.glBindVertexArray(0)
    }

    private fun loadMapTexture() {
        val textureHandle = IntArray(1)
        GLES30.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0])
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.optemap_217k)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
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
        GLES30.glGenTextures(1, textureHandle, 0)
        markerTextureAtlas = textureHandle[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, markerTextureAtlas)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, atlasBitmap, 0)
        
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
        GLES30.glGenTextures(1, textureHandle, 0)
        fontTextureAtlas = textureHandle[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fontTextureAtlas)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, atlasBitmap, 0)
        
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
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, markerInstanceVBO)
        val markerBuffer = ByteBuffer.allocateDirect(markerCount * 4 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(markerInstanceData, 0, markerCount * 4)
                position(0)
            }
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, markerCount * 4 * 4, markerBuffer)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, textInstanceVBO)
        val textBuffer = ByteBuffer.allocateDirect(textCount * 4 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(textInstanceData, 0, textCount * 4)
                position(0)
            }
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, textCount * 4 * 4, textBuffer)
    }

    private fun drawMap() {
        GLES30.glUseProgram(mapProgram)

        val positionHandle = GLES30.glGetAttribLocation(mapProgram, "vPosition")
        val texCoordHandle = GLES30.glGetAttribLocation(mapProgram, "vTexCoord")
        val mvpMatrixHandle = GLES30.glGetUniformLocation(mapProgram, "uMVPMatrix")
        val textureHandle = GLES30.glGetUniformLocation(mapProgram, "uTexture")

        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glEnableVertexAttribArray(texCoordHandle)

        val vertexBuffer = ByteBuffer.allocateDirect(mapVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(mapVertices)
                position(0)
            }

        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer)
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer.apply { position(2) })

        val mvpMatrix = mvpMatrix.identityM()
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mapTextureId)
        GLES30.glUniform1i(textureHandle, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(texCoordHandle)
        GLES30.glUseProgram(0)
    }

    private fun drawMarkersES3() {
        GLES30.glUseProgram(markerProgram)

        val mvpMatrixHandle = GLES30.glGetUniformLocation(markerProgram, "uMVPMatrix")
        val textureHandle = GLES30.glGetUniformLocation(markerProgram, "uTextureAtlas")
        val atlasSizeHandle = GLES30.glGetUniformLocation(markerProgram, "uAtlasSize")

        // 使用VAO进行实例渲染
        GLES30.glBindVertexArray(markerVAO)

        // MVP矩阵
        val mvpMatrix = mvpMatrix.identityM()
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 纹理图集
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, markerTextureAtlas)
        GLES30.glUniform1i(textureHandle, 0)
        GLES30.glUniform2f(atlasSizeHandle, 3f, 1f)

        // 混合
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // OpenGL ES 3.0 实例绘制
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, markerCount)

        // 清理
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glBindVertexArray(0)
        GLES30.glUseProgram(0)
    }

    private fun drawTextsES3() {
        GLES30.glUseProgram(textProgram)

        val mvpMatrixHandle = GLES30.glGetUniformLocation(textProgram, "uMVPMatrix")
        val textureHandle = GLES30.glGetUniformLocation(textProgram, "uFontAtlas")
        val atlasSizeHandle = GLES30.glGetUniformLocation(textProgram, "uFontAtlasSize")
        val textColorHandle = GLES30.glGetUniformLocation(textProgram, "uTextColor")

        // 使用VAO进行实例渲染
        GLES30.glBindVertexArray(textVAO)

        // MVP矩阵
        val mvpMatrix = mvpMatrix.identityM()
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 字体纹理图集
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fontTextureAtlas)
        GLES30.glUniform1i(textureHandle, 0)
        GLES30.glUniform2f(atlasSizeHandle, 6f, 1f)
        GLES30.glUniform4f(textColorHandle, 1f, 1f, 1f, 1f)

        // 混合
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // OpenGL ES 3.0 实例绘制
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, textCount)

        // 清理
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glBindVertexArray(0)
        GLES30.glUseProgram(0)
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
        // 清理OpenGL ES 3.0资源
        GLES30.glDeleteVertexArrays(2, intArrayOf(markerVAO, textVAO), 0)
        GLES30.glDeleteBuffers(4, intArrayOf(markerVBO, markerInstanceVBO, textVBO, textInstanceVBO), 0)
        GLES30.glDeleteTextures(3, intArrayOf(mapTextureId, markerTextureAtlas, fontTextureAtlas), 0)
    }
} 