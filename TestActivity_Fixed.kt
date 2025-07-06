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
import com.houtrry.openglsample.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.sqrt

class TestActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "TestActivity"
        private const val MAX_MARKERS = 3000
    }

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

    // 批量渲染着色器（OpenGL ES 2.0兼容）
    private val batchVertexShaderCode = """
    uniform mat4 uMVPMatrix;
    attribute vec4 vPosition;
    attribute vec2 vTexCoord;
    varying vec2 texCoord;
    
    void main() {
        gl_Position = uMVPMatrix * vPosition;
        texCoord = vTexCoord;
    }
""".trimIndent()

    private val batchFragmentShaderCode = """
    precision mediump float;
    uniform sampler2D uTextureAtlas;
    uniform vec2 uAtlasSize;
    uniform float uTextureIndex;
    varying vec2 texCoord;
    
    void main() {
        vec2 atlasCoord = texCoord / uAtlasSize;
        atlasCoord.x += mod(uTextureIndex, uAtlasSize.x) / uAtlasSize.x;
        atlasCoord.y += floor(uTextureIndex / uAtlasSize.x) / uAtlasSize.y;
        gl_FragColor = texture2D(uTextureAtlas, atlasCoord);
    }
""".trimIndent()

    // 地图纹理和顶点数据
    private var mapTextureId = -1
    private lateinit var mapVertices: FloatArray

    private lateinit var glSurfaceView: GLSurfaceView
    private val viewMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }
    private val projectionMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }
    private val modelMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }
    private val mvpMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }

    // 手势相关变量
    private var previousPointF = PointF()
    private var scaleFactor = 1f
    private var mapProgram = -1
    private var batchProgram = -1
    private var viewWidth = 0
    private var viewHeight = 0

    // 批量渲染相关
    private var markerVBO = -1
    private var markerTextureAtlas = -1
    private val markerInstanceData = FloatArray(MAX_MARKERS * 4) // x,y,textureIndex,scale
    private var markerCount = 0

    // 批量顶点数据（OpenGL ES 2.0兼容）
    private val markerBatchVertices = mutableListOf<Float>()
    private val mapSize = Point(0, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2) // 使用OpenGL ES 2.0
            setRenderer(this@TestActivity)
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
        createMarkerTextureAtlas()
        generateTestMarkers()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.viewWidth = width
        this.viewHeight = height
        GLES20.glViewport(0, 0, width, height)
        Matrix.orthoM(
            projectionMatrix, 0,
            -width * 0.5f, width * 0.5f,
            -height * 0.5f, height * 0.5f,
            -1f, 1f
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        drawMap()
        drawMarkersBatch()
    }

    private fun initShaders() {
        // 地图着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mapProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        // 批量渲染着色器
        val batchVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, batchVertexShaderCode)
        val batchFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, batchFragmentShaderCode)

        batchProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, batchVertexShader)
            GLES20.glAttachShader(it, batchFragmentShader)
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
        val vboIds = IntArray(1)
        GLES20.glGenBuffers(1, vboIds, 0)
        markerVBO = vboIds[0]
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

    private fun generateTestMarkers() {
        markerCount = 0
        markerBatchVertices.clear()
        
        // 生成2500个测试Marker
        for (i in 0 until 2500) {
            val x = (Math.random() * 2000 - 1000).toFloat()
            val y = (Math.random() * 2000 - 1000).toFloat()
            val iconIndex = (i % 3).toFloat()
            
            // 添加Marker实例数据
            val markerIndex = markerCount * 4
            markerInstanceData[markerIndex] = x
            markerInstanceData[markerIndex + 1] = y
            markerInstanceData[markerIndex + 2] = iconIndex
            markerInstanceData[markerIndex + 3] = 50f
            
            markerCount++
        }
        
        generateBatchVertices()
    }

    private fun generateBatchVertices() {
        markerBatchVertices.clear()
        
        // 为每个Marker生成顶点数据
        for (i in 0 until markerCount) {
            val index = i * 4
            val x = markerInstanceData[index]
            val y = markerInstanceData[index + 1]
            val scale = markerInstanceData[index + 3]
            
            // 计算四个顶点
            val halfSize = scale * 0.5f
            val vertices = floatArrayOf(
                x - halfSize, y - halfSize, 0f, 1f,  // 左下
                x + halfSize, y - halfSize, 1f, 1f,  // 右下
                x - halfSize, y + halfSize, 0f, 0f,  // 左上
                x + halfSize, y + halfSize, 1f, 0f   // 右上
            )
            markerBatchVertices.addAll(vertices.toList())
        }
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
        if (markerBatchVertices.isEmpty()) return
        
        GLES20.glUseProgram(batchProgram)

        val positionHandle = GLES20.glGetAttribLocation(batchProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(batchProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(batchProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(batchProgram, "uTextureAtlas")
        val atlasSizeHandle = GLES20.glGetUniformLocation(batchProgram, "uAtlasSize")
        val textureIndexHandle = GLES20.glGetUniformLocation(batchProgram, "uTextureIndex")

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

        // 批量绘制所有Marker（OpenGL ES 2.0兼容方式）
        val batchBuffer = ByteBuffer.allocateDirect(markerBatchVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(markerBatchVertices.toFloatArray())
                position(0)
            }
        
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0) // 使用客户端数组
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, batchBuffer)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, batchBuffer.apply { position(2) })
        
        // 分批次绘制，每批100个Marker
        val batchSize = 100
        val verticesPerMarker = 4
        val floatsPerVertex = 4
        val floatsPerMarker = verticesPerMarker * floatsPerVertex
        
        for (i in 0 until markerCount step batchSize) {
            val count = minOf(batchSize, markerCount - i)
            val startIndex = i * floatsPerMarker
            val vertexCount = count * verticesPerMarker
            
            // 设置纹理索引
            val textureIndex = markerInstanceData[i * 4 + 2]
            GLES20.glUniform1f(textureIndexHandle, textureIndex)
            
            batchBuffer.position(startIndex)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, batchBuffer)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, batchBuffer.apply { position(startIndex + 2) })
            
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount)
        }

        // 清理
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glUseProgram(0)
    }

    // 成员变量
    private var previousAngle = 0f
    private var initialDistance = 0f

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
        GLES20.glDeleteBuffers(1, intArrayOf(markerVBO), 0)
        GLES20.glDeleteTextures(2, intArrayOf(mapTextureId, markerTextureAtlas), 0)
    }
} 