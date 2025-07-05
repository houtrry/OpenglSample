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
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.common_map.utils.sp
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

    //mvpMatrix = projectionMatrix * viewMatrix * modelMatrix
    private val mvpMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }

    // 手势相关变量
    private var previousPointF = PointF()
    private var scaleFactor = 1f
    private var mapProgram = -1
    private var textProgram = -1
    private var viewWidth = 0
    private var viewHeight = 0
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
        initShaders()
        loadMapTexture()
        loadMarkerTextures()
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

        drawMarkers()
    }

    private fun initShaders() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mapProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        val textVertexShaper = loadShader(GLES20.GL_VERTEX_SHADER, textVertexShaderCode)
        val textFragmentShaper = loadShader(GLES20.GL_FRAGMENT_SHADER, textFragmentShaderCode)

        textProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, textVertexShaper)
            GLES20.glAttachShader(it, textFragmentShaper)
            GLES20.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private val mapSize = Point(0, 0)

    private fun loadMapTexture() {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            // 绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            // 设置纹理过滤
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            // 加载位图到纹理
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.optemap_217k)
//            val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.t1)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            mapSize.x = bitmapWidth
            mapSize.y = bitmapHeight
            bitmap.recycle()
            mapVertices = floatArrayOf(
                // 位置坐标     // 纹理坐标
                -bitmapWidth * 0.5f, -bitmapHeight * 0.5f, 0f, 1f,
                bitmapWidth * 0.5f, -bitmapHeight * 0.5f, 1f, 1f,
                -bitmapWidth * 0.5f, bitmapHeight * 0.5f, 0f, 0f,
                bitmapWidth * 0.5f, bitmapHeight * 0.5f, 1f, 0f
            )

            mapTextureId = textureHandle[0]
        }
    }

    private fun drawMap() {
        // 使用地图着色器程序
        GLES20.glUseProgram(mapProgram)

        // 获取着色器变量位置
        val positionHandle = GLES20.glGetAttribLocation(mapProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(mapProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(mapProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(mapProgram, "uTexture")

        // 启用顶点属性数组
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // 准备顶点数据
        val vertexBuffer = ByteBuffer.allocateDirect(mapVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(mapVertices)
                position(0)
            }

        // 设置顶点属性指针
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            16,
            vertexBuffer.apply { position(2) })

        // 计算MVP矩阵
        val mvpMatrix = mvpMatrix.identityM()
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        // 传递MVP矩阵
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mapTextureId)
        GLES20.glUniform1i(textureHandle, 0)

        // 绘制地图
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 禁用顶点属性数组
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glUseProgram(0)
    }

    // 成员变量
    private var previousAngle = 0f

    // 成员变量
    private var initialDistance = 0f

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureListeners() {
        glSurfaceView.setOnTouchListener { _, event ->
            return@setOnTouchListener when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // 单指按下，记录初始位置
                    Log.d(
                        TAG,
                        "ACTION_DOWN ${event.pointerCount} point, (${event.getX(0)}, ${event.getY(0)}) -> (${event.x}, ${event.y})"
                    )
                    previousPointF = PointF(event.x, event.y)
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    Log.d(
                        TAG,
                        "ACTION_POINTER_DOWN ${event.pointerCount} point, (${event.getX(0)}, ${
                            event.getY(0)
                        }) -> (${event.x}, ${event.y})"
                    )
                    if (event.pointerCount == 2) {
                        // 双指按下，初始化旋转/缩放参数
                        val dx = event.getX(1) - event.getX(0)
                        val dy = event.getY(1) - event.getY(0)
                        initialDistance = sqrt(dx * dx + dy * dy)
                        previousAngle = atan2(-dy.toDouble(), dx.toDouble()).toFloat()

                        // ✅ 计算并存储当前中心点（转换为OpenGL坐标）
//                        convertScreenToGL((event.getX(0) + event.getX(1)) / 2, (event.getY(0) + event.getY(1)) / 2)  // 转换到OpenGL坐标系
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        Log.d(TAG, "ACTION_MOVE only one point")
                        // 单指拖动
                        val point = PointF(event.x, event.y)
                        translateMap(
                            point.x - previousPointF.x,
                            point.y - previousPointF.y
                        )  // Y轴需反向
                        previousPointF = point
                        glSurfaceView.requestRender()
                    } else if (event.pointerCount == 2) {
                        // 双指操作
                        val x1 = event.getX(0)
                        val y1 = event.getY(0)
                        val x2 = event.getX(1)
                        val y2 = event.getY(1)

                        // ✅ 实时更新中心点（并转换坐标系）
                        val pivot = convertScreenToGL((x1 + x2) / 2, (y1 + y2) / 2)

                        // 计算旋转
                        val dx = x2 - x1
                        val dy = y2 - y1
                        val currentAngle = atan2(-dy.toDouble(), dx.toDouble()).toFloat()
                        val rotationAngle = if (previousAngle != 0f) {
                            Math.toDegrees((currentAngle - previousAngle).toDouble())
                                .toFloat()
                        } else {
                            0f
                        }
                        previousAngle = currentAngle

                        // 计算缩放
                        val currentDistance = sqrt(dx * dx + dy * dy)
                        val scaleFactor = (currentDistance / initialDistance)
                        initialDistance = currentDistance
                        updateViewMatrix(pivot, rotationAngle, scaleFactor)
                        glSurfaceView.requestRender()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 重置状态
                    Log.d(
                        TAG,
                        "ACTION_UP ${event.pointerCount} point, (${event.getX(0)}, ${event.getY(0)}) -> (${event.x}, ${event.y})"
                    )
                    previousAngle = 0f
                    initialDistance = 0f
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    Log.d(
                        TAG,
                        "ACTION_POINTER_UP ${event.pointerCount} point, (${event.getX(0)}, ${
                            event.getY(0)
                        }) -> (${event.x}, ${event.y})"
                    )
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
        val ndcY = 1.0f - screenY / (viewHeight * 0.5f) // Y轴翻转

        // 2. 创建MVP矩阵
        Matrix.multiplyMM(tempMatrix, 0, projectionMatrix, 0, modelMatrix, 0)

        // 3. 求逆矩阵
        Matrix.invertM(invertedMatrix, 0, tempMatrix, 0)

        // 4. 变换坐标
        val inVec = floatArrayOf(ndcX, ndcY, 0f, 1f)
        val outVec = FloatArray(4)
        Matrix.multiplyMV(outVec, 0, invertedMatrix, 0, inVec, 0)

        // 5. 透视除法
        if (outVec[3] != 0f) {
            outVec[0] /= outVec[3]
            outVec[1] /= outVec[3]
        }

        return PointF(outVec[0], outVec[1])
    }

    private fun updateViewMatrix(pivot: PointF, rotate: Float, scale: Float) {
//        Log.d(TAG, "updateViewMatrix start, pivot: $pivot, rotate: $rotate, scale: $scale")
        synchronized(modelMatrix) {
            // 2. 移动到当前操作中心点
            Matrix.translateM(modelMatrix, 0, pivot.x, pivot.y, 0f)

            // 3. 应用旋转和缩放
            Matrix.rotateM(modelMatrix, 0, rotate, 0f, 0f, 1f)
            Matrix.scaleM(modelMatrix, 0, scale, scale, 1f)
            // 4. 移回原点
            Matrix.translateM(modelMatrix, 0, -pivot.x, -pivot.y, 0f)
        }
    }

    // 标记点数据类
    data class MapMarker(
        val x: Float,
        val y: Float,
        val iconResId: Int,
        val text: String,
        val textColor: Int = Color.WHITE,
        val iconSize: Int = 400,
        val iconFontGapSize: Float = 3.dp,
        val followRotate: Boolean = false,
        val modelMatrix: FloatArray = FloatArray(16),
        val glCenter: PointF = PointF()
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MapMarker

            if (x != other.x) return false
            if (y != other.y) return false
            if (iconResId != other.iconResId) return false
            if (text != other.text) return false
            if (textColor != other.textColor) return false
            if (iconSize != other.iconSize) return false
            if (followRotate != other.followRotate) return false
            if (!modelMatrix.contentEquals(other.modelMatrix)) return false

            return true
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

    private fun MapMarker.reCalcModelMatrixOfMarker(
        mapModelMatrix: FloatArray,
        width: Float,
        height: Float
    ): FloatArray {
//        // 2. 创建标记点的局部平移矩阵（相对于地图中心）
        val localTranslation = FloatArray(16).identityM()
        Matrix.translateM(localTranslation, 0, glCenter.x, glCenter.y, 0f)
//
//        // 3. 将标记点位置转换到世界空间
        val worldPosition = FloatArray(16).identityM()
        Matrix.multiplyMM(worldPosition, 0, mapModelMatrix, 0, localTranslation, 0)

        // 5. 创建标记点缩放矩阵（转换为像素大小）
        val markerScaleMatrix = FloatArray(16).identityM()
        Matrix.scaleM(markerScaleMatrix, 0, 1f, 1f, 1f)

        // 6. 组合最终矩阵
        val resultMatrix = FloatArray(16)
        Matrix.multiplyMM(resultMatrix, 0, worldPosition, 0, markerScaleMatrix, 0)
        Log.d(TAG, "reCalcModelMatrixOfMarker start ---------------------------------------------------")
        Log.d(TAG, "reCalcModelMatrixOfMarker worldPosition: ${worldPosition.formatMatrixString()}")
        Log.d(TAG, "reCalcModelMatrixOfMarker resultMatrix: ${resultMatrix.formatMatrixString()}")

        val result = if (followRotate) {
            resultMatrix.getTransformMatrixWithoutScale(width, height, modelMatrix)
        } else {
            modelMatrix.apply {
                this.identityM()
                Matrix.translateM(this, 0, resultMatrix.getTranslation()[0],
                    resultMatrix.getTranslation()[1], resultMatrix.getTranslation()[2])
                Matrix.scaleM(this, 0, width, height, 0f)
            }
        }
        Log.d(TAG, "reCalcModelMatrixOfMarker followRotate: $followRotate, result: ${result.formatMatrixString()}")
        Log.d(TAG, "reCalcModelMatrixOfMarker end   ---------------------------------------------------")
        return result
    }

    // 标记点列表
    private val markers = listOf(
        MapMarker(0f, 0f, R.mipmap.robot, "", iconSize = 400, followRotate = true),
        MapMarker(200.0f, 40.005f, R.mipmap.icon_start_point, "初始点0", iconSize = 100),
        MapMarker(150.015f, 70.555f, R.mipmap.icon_start_point, "初始点1", iconSize = 100),
        MapMarker(220.212f, 140.9450f, R.mipmap.icon_target, "重生之我在初始点2", iconSize = 100),
//        MapMarker(300f, 400f, R.drawable.ic_launcher_background, "位置2"),
        // 添加更多标记点...
    )

    // 标记点纹理
    private val markerTextures = mutableMapOf<Int, Int>()
    private val markerFontTextures = mutableMapOf<String, FontTextureInfo>()

    private fun loadMarkerTextures() {
        markers.forEach { marker ->
            if (!markerTextures.containsKey(marker.iconResId)) {
                val textureHandle = IntArray(1)
                GLES20.glGenTextures(1, textureHandle, 0)

                if (textureHandle[0] != 0) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_LINEAR
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_LINEAR
                    )

                    val bitmap = BitmapFactory.decodeResource(
                        resources,
                        marker.iconResId,
                    )
                    Log.e(TAG, "bitmap: $bitmap, ${marker.text}")
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                    bitmap?.recycle()

                    markerTextures[marker.iconResId] = textureHandle[0]
                }
            }
            if (marker.text.isNotEmpty() && !markerFontTextures.containsKey(marker.text)) {
                val textureHandle = IntArray(1)
                GLES20.glGenTextures(1, textureHandle, 0)

                if (textureHandle[0] != 0) {
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_LINEAR
                    )
                    GLES20.glTexParameteri(
                        GLES20.GL_TEXTURE_2D,
                        GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_LINEAR
                    )

                    val bitmap = marker.text.generateBitmap(
                        20.sp,
                        resources.getColor(R.color.white),
                    )
                    Log.e(TAG, "bitmap: $bitmap, ${marker.text}")
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                    markerFontTextures[marker.text] = FontTextureInfo(textureHandle[0], marker.text, bitmap.width, bitmap.height)
                    bitmap.recycle()

                }
            }
        }
        Log.d(TAG, "markerTextures: $markerTextures, markers: $markers")
    }

//    private fun loadVectorDrawableAsBitmap(
//        context: Context,
//        @DrawableRes resId: Int,
//        width: Int,
//        height: Int
//    ): Bitmap? {
//        val vectorDrawable = ContextCompat.getDrawable(context, resId) as? VectorDrawable
//            ?: return null
//
//        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
//        vectorDrawable.draw(canvas)
//
//        return bitmap
//    }

    private fun drawMarkers() {
        // 使用地图着色器程序
        GLES20.glUseProgram(mapProgram)
        markers.forEach { marker ->
            drawMarkerIcon(marker)
            drawMarkerText(marker)
        }
        // 使用地图着色器程序
        GLES20.glUseProgram(0)
    }

    private fun drawMarkerIcon(marker: MapMarker) {
        val textureId = markerTextures[marker.iconResId]
        Log.d(TAG, "drawMarkerIcon $textureId start for $marker")
        if (textureId == null) {
            Log.d(TAG, "no found textureId for $marker")
            return
        }
        // 图标大小
        val iconSize = marker.iconSize

        val markerCenter = worldToGl(marker.x, marker.y)
        marker.glCenter.x = markerCenter.x
        marker.glCenter.y = markerCenter.y
        // 计算图标顶点
//        val left = markerCenter.x - iconSize / 2
//        val right = markerCenter.x + iconSize / 2
//        val top = markerCenter.y - iconSize / 2
//        val bottom = markerCenter.y + iconSize / 2

        Log.d(TAG, "drawMarkerIcon ${marker.text} -> $markerCenter -> $iconSize -> ($viewWidth, $viewHeight)")
        val vertices = floatArrayOf(
            // 顶点坐标     // 纹理坐标（修正）
            -0.5f, -0.5f, 0f, 1f,  // 左上 → 左下
            0.5f, -0.5f, 1f, 1f,  // 右上 → 右下
            -0.5f, 0.5f, 0f, 0f,  // 左下 → 左上
            0.5f, 0.5f, 1f, 0f   // 右下 → 右上
        )

        Log.d(TAG, "${marker.text} -> vertices: ${vertices.formatMatrixString()}")


        // 获取着色器变量位置
        val positionHandle = GLES20.glGetAttribLocation(mapProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(mapProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(mapProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(mapProgram, "uTexture")

        // 启用顶点属性数组
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // 准备顶点数据
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }

        // 设置顶点属性指针
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            16,
            vertexBuffer.apply { position(2) })

        // 计算MVP矩阵
        val modelMatrix1 = marker.reCalcModelMatrixOfMarker(mapModelMatrix = modelMatrix, marker.iconSize.toFloat(), marker.iconSize.toFloat()).copyOf()

        val modelMatrix = marker.modelMatrix.identityM()
        Log.d(TAG, "${marker.text} -> modelMatrix: ${modelMatrix.formatMatrixString()}, modelMatrix1: ${modelMatrix1.formatMatrixString()}, modelMatrix: ${modelMatrix.formatMatrixString()}")
        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix1, 0)

        // 传递MVP矩阵
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        // 绘制图标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 禁用顶点属性数组
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun drawMarkerText(marker: MapMarker) {
        val fontTextureInfo = markerFontTextures[marker.text]
        Log.d(TAG, "drawMarkerIcon $fontTextureInfo start for $marker")
        if (fontTextureInfo == null) {
            Log.d(TAG, "no found textureId for $marker")
            return
        }
        // 图标大小
        val iconSize = marker.iconSize

        val markerCenter = worldToGl(marker.x, marker.y)
        marker.glCenter.x = markerCenter.x
        marker.glCenter.y = markerCenter.y
        // 计算图标顶点
//        val left = markerCenter.x - iconSize / 2
//        val right = markerCenter.x + iconSize / 2
//        val top = markerCenter.y - iconSize / 2
//        val bottom = markerCenter.y + iconSize / 2

        Log.d(TAG, "drawMarkerIcon ${marker.text} -> $markerCenter -> $iconSize -> ($viewWidth, $viewHeight)")
        val vertices = floatArrayOf(
            // 顶点坐标     // 纹理坐标（修正）
            -0.5f, -0.5f, 0f, 1f,  // 左上 → 左下
            0.5f, -0.5f, 1f, 1f,  // 右上 → 右下
            -0.5f, 0.5f, 0f, 0f,  // 左下 → 左上
            0.5f, 0.5f, 1f, 0f   // 右下 → 右上
        )

        Log.d(TAG, "${marker.text} -> vertices: ${vertices.formatMatrixString()}")
        // 使用地图着色器程序

        // 获取着色器变量位置
        val positionHandle = GLES20.glGetAttribLocation(mapProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(mapProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(mapProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(mapProgram, "uTexture")

        // 启用顶点属性数组
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // 准备顶点数据
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }

        // 设置顶点属性指针
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            16,
            vertexBuffer.apply { position(2) })

        // 计算MVP矩阵
        val modelMatrix1 = marker.reCalcModelMatrixOfMarker(mapModelMatrix = modelMatrix, fontTextureInfo.width.toFloat(), fontTextureInfo.height.toFloat()).copyOf()

        val offsetMatrix = FloatArray(16).identityM()
        Matrix.translateM(offsetMatrix, 0, 0f, -marker.iconFontGapSize - (marker.iconSize + fontTextureInfo.height) * 0.5f, 0f)

        val modelMatrix = marker.modelMatrix.identityM()
        Log.d(TAG, "${marker.text} -> modelMatrix: ${modelMatrix.formatMatrixString()}, modelMatrix1: ${modelMatrix1.formatMatrixString()}, modelMatrix: ${modelMatrix.formatMatrixString()}")
        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, offsetMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix1, 0)

        // 传递MVP矩阵
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fontTextureInfo.textureId)
        GLES20.glUniform1i(textureHandle, 0)

        // 绘制图标
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 禁用顶点属性数组
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

//    private fun drawMarkerText(marker: MapMarker) {
//        if (marker.text.isNotEmpty()) {
//            return
//        }
////        // 1. 准备文字绘制参数
////        val textSize = 24f * scaleFactor // 根据缩放调整文字大小
////        val textPadding = 5f * scaleFactor // 文字与图标的间距
////
////        // 2. 计算文字位置（在图标上方）
////        val textX = marker.x
////        val textY = marker.y - (32f * scaleFactor / 2) - textPadding // 32f是图标高度
////
////        // 3. 创建Paint对象设置文字样式
////        val paint = Paint().also {
////            it.color = marker.textColor
////            it.textSize = textSize
////            it.isAntiAlias = true
////            it.textAlign = Paint.Align.CENTER
////            it.typeface = Typeface.DEFAULT_BOLD
////            it.strokeWidth = 10f
////        }
////
////        // 4. 测量文字尺寸
////        val textWidth = paint.measureText(marker.text)
////        val textHeight = paint.descent() - paint.ascent()
////
////        // 5. 创建文字位图（带透明背景）
////        val bitmap = Bitmap.createBitmap(
////            textWidth.toInt() + 2, // 加2避免边缘裁剪
////            textHeight.toInt() + 2,
////            Bitmap.Config.ARGB_8888
////        )
////
////        // 6. 绘制文字到位图
////        Canvas(bitmap).apply {
////            drawText(
////                marker.text,
////                width / 2f,
////                height / 2f - (paint.ascent() + paint.descent()) / 2,
////                paint
////            )
////        }
////
////        // 7. 生成OpenGL纹理
////        val textureIds = IntArray(1)
////        GLES20.glGenTextures(1, textureIds, 0)
//
//        val textureId = markerFontTextures[marker.text]
//        if (textureId == null) {
//            return
//        }
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
//        GLES20.glTexParameteri(
//            GLES20.GL_TEXTURE_2D,
//            GLES20.GL_TEXTURE_MIN_FILTER,
//            GLES20.GL_LINEAR
//        )
//        GLES20.glTexParameteri(
//            GLES20.GL_TEXTURE_2D,
//            GLES20.GL_TEXTURE_MAG_FILTER,
//            GLES20.GL_LINEAR
//        )
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
//
//        // 8. 计算文字渲染的顶点坐标
//        val left = textX - textWidth / 2
//        val right = textX + textWidth / 2
//        val top = textY - textHeight / 2
//        val bottom = textY + textHeight / 2
//
//        val vertices = floatArrayOf(
//            // 顶点坐标       // 纹理坐标
//            left, bottom, 0f, 0f,  // 左下
//            right, bottom, 1f, 0f,  // 右下
//            left, top, 0f, 1f,  // 左上
//            right, top, 1f, 1f   // 右上
//        )
//
//        // 9. 准备顶点缓冲区
//        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
//            .order(ByteOrder.nativeOrder())
//            .asFloatBuffer()
//            .apply {
//                put(vertices)
//                position(0)
//            }
//
//        // 10. 使用文字着色器程序
//        GLES20.glUseProgram(textProgram)
//
//        // 11. 获取着色器变量位置
//        val positionHandle = GLES20.glGetAttribLocation(textProgram, "vPosition")
//        val texCoordHandle = GLES20.glGetAttribLocation(textProgram, "vTexCoord")
//        val mvpMatrixHandle = GLES20.glGetUniformLocation(textProgram, "uMVPMatrix")
//        val textureHandle = GLES20.glGetUniformLocation(textProgram, "uTexture")
//        val textColorHandle = GLES20.glGetUniformLocation(textProgram, "uTextColor")
//
//        // 12. 启用顶点属性
//        GLES20.glEnableVertexAttribArray(positionHandle)
//        GLES20.glEnableVertexAttribArray(texCoordHandle)
//
//        // 13. 设置顶点数据
//        vertexBuffer.position(0)
//        GLES20.glVertexAttribPointer(
//            positionHandle, 2,
//            GLES20.GL_FLOAT, false,
//            16, vertexBuffer
//        )
//
//        vertexBuffer.position(2)
//        GLES20.glVertexAttribPointer(
//            texCoordHandle, 2,
//            GLES20.GL_FLOAT, false,
//            16, vertexBuffer
//        )
//
//        // 14. 计算MVP矩阵
//        val modelMatrix = marker.reCalcModelMatrixOfMarker(mapModelMatrix = modelMatrix, mapSize.x, mapSize.y)
//
//        val mvpMatrix = FloatArray(16)
//        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
//        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)
//
//        // 15. 传递统一变量
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
//        GLES20.glUniform1i(textureHandle, 0)
//
//        // 设置文字颜色（使用原始颜色，不修改透明度）
//        GLES20.glUniform4f(
//            textColorHandle,
//            1f, 1f, 1f, 1f // 保持原始颜色，因为位图已经包含颜色信息
//        )
//
//        // 16. 启用混合实现透明效果
//        GLES20.glEnable(GLES20.GL_BLEND)
//        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
//
//        // 17. 绘制文字
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
//
//        // 18. 清理状态
//        GLES20.glDisableVertexAttribArray(positionHandle)
//        GLES20.glDisableVertexAttribArray(texCoordHandle)
//        GLES20.glDisable(GLES20.GL_BLEND)
//
//        // 19. 删除临时纹理
//        GLES20.glDeleteTextures(1, textureIds, 0)
//        GLES20.glUseProgram(0)
//    }

//    private val bitmapInfo = BitmapInfo(
//        -13.55f,
//        -3.5f,
//        0.05f
//    )
    private val bitmapInfo = BitmapInfo(
        -0f,
        -0f,
        0.05f
    )
    private fun worldToGl(x: Float, y: Float): PointF {
//        return convertScreenToGL(
//            (x - bitmapInfo.resolution * mapSize.x * 0.5f - bitmapInfo.originX) / bitmapInfo.resolution,
//            (y - bitmapInfo.resolution * mapSize.y * 0.5f - bitmapInfo.originY) / bitmapInfo.resolution,
//        )

        return PointF(
            (x - bitmapInfo.resolution * mapSize.x * 0.5f - bitmapInfo.originX) / bitmapInfo.resolution,
            (y - bitmapInfo.resolution * mapSize.y * 0.5f - bitmapInfo.originY) / bitmapInfo.resolution,
        )
    }

    private data class BitmapInfo(
        val originX: Float,
        val originY: Float,
        val resolution: Float,
    )

    private data class FontTextureInfo(
        val textureId: Int,
        val text: String,
        val width: Int,
        val height: Int,
    )
}

private fun String.generateBitmap(textSize: Float,
                                  textColor: Int,
                                  textPaddingStart: Float = 2.dp,
                                  textPaddingTop: Float = 2.dp): Bitmap {
    // 1. 准备文字绘制参数

    // 2. 计算文字位置（在图标上方）

    // 3. 创建Paint对象设置文字样式
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
        it.color = textColor
        it.textSize = textSize
        it.strokeWidth = 10f
    }

    // 4. 测量文字尺寸
    val rect = Rect()
    paint.getTextBounds(this, 0, this.length, rect)
    val textWidth = textPaddingStart * 2 + rect.width()
    val textHeight = textPaddingTop * 2 + rect.height()

    // 5. 创建文字位图（带透明背景）
    val bitmap = Bitmap.createBitmap(
        textWidth.toInt(),
        textHeight.toInt(),
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    canvas.drawText(this,
        0, length,
        textPaddingStart, textHeight - textPaddingTop,
        paint)
    return bitmap
}
