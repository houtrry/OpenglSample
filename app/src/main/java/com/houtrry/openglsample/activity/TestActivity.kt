package com.houtrry.openglsample.activity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.lopengles20.utils.*
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
        private const val MAP_DEPTH = -1f
        private const val MARKER_DEPTH_BASE = 0f
        private const val TEXT_DEPTH = 10f
    }

    // 顶点着色器代码
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

    // 片段着色器代码
    private val fragmentShaderCode = """
    precision mediump float;
    uniform sampler2D uTexture;
    varying vec2 texCoord;
    
    void main() {
        gl_FragColor = texture2D(uTexture, texCoord);
    }
""".trimIndent()

    // 文字着色器
    private val textVertexShaderCode = """
    uniform mat4 uMVPMatrix;
    attribute vec4 vPosition;
    attribute vec2 vTexCoord;
    varying vec2 texCoord;
    
    void main() {
        gl_Position = uMVPMatrix * vPosition;
        texCoord = vTexCoord;
    }
""".trimIndent()

    private val textFragmentShaderCode = """
    precision mediump float;
    uniform sampler2D uTexture;
    uniform vec4 uTextColor;
    varying vec2 texCoord;
    
    void main() {
        vec4 texColor = texture2D(uTexture, texCoord);
        gl_FragColor = vec4(uTextColor.rgb, texColor.a * uTextColor.a);
    }
""".trimIndent()

    // OpenGL相关变量
    private var mapTextureId = -1
    private lateinit var mapVertices: FloatArray
    private lateinit var glSurfaceView: GLSurfaceView
    private var mapProgram = -1
    private var textProgram = -1
    private var viewWidth = 0
    private var viewHeight = 0

    // 矩阵
    private val viewMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
        Matrix.setLookAtM(it, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
    }
    private val projectionMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val modelMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val mvpMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    // 手势相关变量
    private var previousPointF = PointF()
    private var scaleFactor = 1f
    private var previousAngle = 0f
    private var initialDistance = 0f

    // 地图信息
    private val mapSize = Point(0, 0)
    private val bitmapInfo = BitmapInfo(0f, 0f, 0.05f)

    // 标记点相关
    private val markers = listOf(
        MapMarker(0f, 0f, R.mipmap.robot, "", iconSize = 400, followRotate = true),
        MapMarker(100f, 100f, R.mipmap.icon_start_point, "初始点0", iconSize = 200),
        MapMarker(-100f, -100f, R.mipmap.icon_start_point, "初始点1", iconSize = 200),
        MapMarker(200f, -200f, R.mipmap.icon_target, "目标点", iconSize = 200),
    )
    private val markerTextures = mutableMapOf<Int, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(this@TestActivity)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
        setContentView(glSurfaceView)
        setupGestureListeners()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        initShaders()
        loadMapTexture()
        loadMarkerTextures()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)
        Matrix.orthoM(
            projectionMatrix, 0,
            -width * 0.5f, width * 0.5f,
            -height * 0.5f, height * 0.5f,
            1f, 100f
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        // 按深度顺序绘制：地图 -> 标记点 -> 文字
        drawMap()
        drawMarkers()
    }

    private fun initShaders() {
        mapProgram = createShaderProgram(vertexShaderCode, fragmentShaderCode)
        textProgram = createShaderProgram(textVertexShaderCode, textFragmentShaderCode)
    }

    private fun createShaderProgram(vertexCode: String, fragmentCode: String): Int {
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

    private fun loadMapTexture() {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.optemap_217k)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            
            mapSize.x = bitmap.width
            mapSize.y = bitmap.height
            bitmap.recycle()
            
            mapVertices = floatArrayOf(
                // 位置坐标     // 纹理坐标
                -bitmap.width * 0.5f, -bitmap.height * 0.5f, MAP_DEPTH, 0f, 1f,
                bitmap.width * 0.5f, -bitmap.height * 0.5f, MAP_DEPTH, 1f, 1f,
                -bitmap.width * 0.5f, bitmap.height * 0.5f, MAP_DEPTH, 0f, 0f,
                bitmap.width * 0.5f, bitmap.height * 0.5f, MAP_DEPTH, 1f, 0f
            )

            mapTextureId = textureHandle[0]
        }
    }

    private fun loadMarkerTextures() {
        markers.forEach { marker ->
            if (!markerTextures.containsKey(marker.iconResId)) {
                val textureHandle = IntArray(1)
                GLES20.glGenTextures(1, textureHandle, 0)

                if (textureHandle[0] != 0) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

                    val bitmap = BitmapFactory.decodeResource(resources, marker.iconResId)
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                    bitmap?.recycle()

                    markerTextures[marker.iconResId] = textureHandle[0]
                }
            }
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

        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer.apply { position(3) })

        // 计算MVP矩阵
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

    private fun drawMarkers() {
        // 先绘制所有Marker图标
        drawAllMarkerIcons()
        // 再绘制所有文字
        drawAllMarkerTexts()
    }

    private fun drawAllMarkerIcons() {
        GLES20.glUseProgram(mapProgram)

        val positionHandle = GLES20.glGetAttribLocation(mapProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(mapProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(mapProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(mapProgram, "uTexture")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // 所有Marker使用相同的深度值，按绘制顺序渲染
        markers.forEachIndexed { index, marker ->
            val textureId = markerTextures[marker.iconResId] ?: return@forEachIndexed

            val markerCenter = worldToGl(marker.x, marker.y)
            marker.glCenter.x = markerCenter.x
            marker.glCenter.y = markerCenter.y

            // 添加调试日志
            Log.d(TAG, "Marker $index: world(${marker.x}, ${marker.y}) -> gl(${markerCenter.x}, ${markerCenter.y}), size=${marker.iconSize}")

            val vertices = floatArrayOf(
                // 顶点坐标     // 纹理坐标
                -0.5f, -0.5f, MARKER_DEPTH_BASE + index * 0.1f, 0f, 1f,
                0.5f, -0.5f, MARKER_DEPTH_BASE + index * 0.1f, 1f, 1f,
                -0.5f, 0.5f, MARKER_DEPTH_BASE + index * 0.1f, 0f, 0f,
                0.5f, 0.5f, MARKER_DEPTH_BASE + index * 0.1f, 1f, 0f
            )

            val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(vertices)
                    position(0)
                }

            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer.apply { position(3) })

            val markerModelMatrix = marker.reCalcModelMatrixOfMarker(modelMatrix, mapSize.x, mapSize.y)
            val mvpMatrix = FloatArray(16)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, markerModelMatrix, 0)

            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(textureHandle, 0)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glUseProgram(0)
    }

    private fun drawAllMarkerTexts() {
        val textMarkers = markers.filter { it.text.isNotEmpty() }
        if (textMarkers.isEmpty()) return

        GLES20.glUseProgram(textProgram)

        val positionHandle = GLES20.glGetAttribLocation(textProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(textProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(textProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(textProgram, "uTexture")
        val textColorHandle = GLES20.glGetUniformLocation(textProgram, "uTextColor")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        textMarkers.forEach { marker ->
            // 添加调试日志
            Log.d(TAG, "Drawing text for marker: ${marker.text} at (${marker.x}, ${marker.y})")
            
            val textSize = 32f  // 使用固定大小
            val textPadding = 20f

            val paint = Paint().apply {
                color = marker.textColor
                this.textSize = textSize
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            val textWidth = paint.measureText(marker.text)
            val textHeight = paint.descent() - paint.ascent()

            val bitmap = Bitmap.createBitmap(
                textWidth.toInt() + 4,  // 增加边距
                textHeight.toInt() + 4,
                Bitmap.Config.ARGB_8888
            )

            Canvas(bitmap).apply {
                drawText(
                    marker.text,
                    width / 2f,
                    height / 2f - (paint.ascent() + paint.descent()) / 2,
                    paint
                )
            }

            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)

            if (textureIds[0] != 0) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            }
            bitmap.recycle()

            val vertices = floatArrayOf(
                // 顶点坐标       // 纹理坐标
                -textWidth / 2, -textHeight / 2, TEXT_DEPTH, 0f, 1f,
                textWidth / 2, -textHeight / 2, TEXT_DEPTH, 1f, 1f,
                -textWidth / 2, textHeight / 2, TEXT_DEPTH, 0f, 0f,
                textWidth / 2, textHeight / 2, TEXT_DEPTH, 1f, 0f
            )

            val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(vertices)
                    position(0)
                }

            vertexBuffer.position(0)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            vertexBuffer.position(3)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)

            // 简化文字位置计算
            val markerCenter = worldToGl(marker.x, marker.y)
            val textOffsetY = marker.iconSize * 0.5f + textPadding + textHeight * 0.5f
            
            // 创建文字专用的模型矩阵
            val textModelMatrix = FloatArray(16).identityM()
            Matrix.translateM(textModelMatrix, 0, markerCenter.x, markerCenter.y + textOffsetY, 0f)
            
            // 应用地图的变换到文字
            val finalTextMatrix = FloatArray(16)
            Matrix.multiplyMM(finalTextMatrix, 0, modelMatrix, 0, textModelMatrix, 0)

            val mvpMatrix = FloatArray(16)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, finalTextMatrix, 0)

            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
            GLES20.glUniform1i(textureHandle, 0)
            
            // 设置文字颜色
            val color = marker.textColor
            val red = Color.red(color) / 255f
            val green = Color.green(color) / 255f
            val blue = Color.blue(color) / 255f
            val alpha = Color.alpha(color) / 255f
            GLES20.glUniform4f(textColorHandle, red, green, blue, alpha)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDeleteTextures(1, textureIds, 0)
        }

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glUseProgram(0)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureListeners() {
        glSurfaceView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
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
                        scaleFactor = currentDistance / initialDistance
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

    private fun MapMarker.reCalcModelMatrixOfMarker(
        mapModelMatrix: FloatArray,
        mapWidth: Int,
        mapHeight: Int
    ): FloatArray {
        // 简化矩阵计算
        val resultMatrix = FloatArray(16).identityM()
        
        // 应用地图的变换
        Matrix.multiplyMM(resultMatrix, 0, mapModelMatrix, 0, resultMatrix, 0)
        
        // 移动到marker位置
        Matrix.translateM(resultMatrix, 0, glCenter.x, glCenter.y, 0f)
        
        // 应用缩放
        Matrix.scaleM(resultMatrix, 0, iconSize.toFloat(), iconSize.toFloat(), 1f)
        
        return resultMatrix
    }

    private fun worldToGl(x: Float, y: Float): PointF {
        // 简化坐标转换：直接使用世界坐标，因为地图已经居中显示
        return PointF(x, y)
    }

    // 数据类
    data class MapMarker(
        val x: Float,
        val y: Float,
        val iconResId: Int,
        val text: String,
        val textColor: Int = Color.WHITE,
        val iconSize: Int = 400,
        val followRotate: Boolean = false,
        val modelMatrix: FloatArray = FloatArray(16),
        val glCenter: PointF = PointF()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as MapMarker
            return x == other.x && y == other.y && iconResId == other.iconResId &&
                    text == other.text && textColor == other.textColor &&
                    iconSize == other.iconSize && followRotate == other.followRotate &&
                    modelMatrix.contentEquals(other.modelMatrix)
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + iconResId
            result = 31 * result + text.hashCode()
            result = 31 * result + textColor
            result = 31 * result + iconSize
            result = 31 * result + followRotate.hashCode()
            result = 31 * result + modelMatrix.contentHashCode()
            return result
        }
    }

    private data class BitmapInfo(
        val originX: Float,
        val originY: Float,
        val resolution: Float,
    )
}