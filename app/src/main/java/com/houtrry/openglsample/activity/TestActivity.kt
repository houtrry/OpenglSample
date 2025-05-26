package com.houtrry.openglsample.activity

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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.houtrry.common_map.utils.dp
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
    private val mvpMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }

    // 手势相关变量
    private var previousX = 0f
    private var previousY = 0f
    private var scaleFactor = 1f
    private var rotationAngle = 0f
    private var mapProgram = -1
    private var textProgram = -1
    private var mapWidth = 1024f // 根据实际地图尺寸设置
    private var mapHeight = 768f // 根据实际地图尺寸设置
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setRenderer(this@TestActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
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
        GLES20.glViewport(0, 0, width, height)
        Matrix.orthoM(
            projectionMatrix, 0, 0f,
            width.toFloat(), 0f, height.toFloat(), -1f, 1f
        )
        mapVertices = floatArrayOf(
            // 位置坐标     // 纹理坐标
            0f, 0f, 0f, 1f,
            width.toFloat(), 0f, 1f, 1f,
            0f, height.toFloat(), 0f, 0f,
            width.toFloat(), height.toFloat(), 1f, 0f
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
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.optemap_22k)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()

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
        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
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
    }

    // 成员变量
    private var previousAngle = 0f
    private var previousVectorLength = 0f
    private var pivotX = 0f // 旋转中心 X
    private var pivotY = 0f // 旋转中心 Y
    private var translateX = 0f // 平移 X
    private var translateY = 0f // 平移 Y
    // 成员变量
    private var initialDistance = 0f
    private var initialScaleFactor = 1f  // 初始缩放值

    private fun setupGestureListeners() {
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    // 单指平移
                    if (e2.pointerCount == 1) {
                        translateMap(-distanceX, distanceY)
                        return true
                    }
                    return false
                }
            })

        val scaleGestureDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    // 双指缩放
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f) // 限制缩放范围
                    updateViewMatrix()
                    return true
                }
            })

        glSurfaceView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // 单指按下，记录初始位置
                    previousX = event.x
                    previousY = event.y
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        // 双指按下，初始化旋转/缩放参数
                        val dx = event.getX(1) - event.getX(0)
                        val dy = event.getY(1) - event.getY(0)
                        initialDistance = sqrt(dx * dx + dy * dy)
                        initialScaleFactor = scaleFactor
                        previousAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()

                        // ✅ 计算并存储当前中心点（转换为OpenGL坐标）
                        pivotX = (event.getX(0) + event.getX(1)) / 2
                        pivotY = (event.getY(0) + event.getY(1)) / 2
                        convertScreenToGL(pivotX, pivotY)  // 转换到OpenGL坐标系
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        // 单指拖动
                        val dx = event.x - previousX
                        val dy = event.y - previousY
                        translateMap(dx, -dy)  // Y轴需反向
                        previousX = event.x
                        previousY = event.y
                    } else if (event.pointerCount == 2) {
                        // 双指操作
                        val x1 = event.getX(0)
                        val y1 = event.getY(0)
                        val x2 = event.getX(1)
                        val y2 = event.getY(1)

                        // ✅ 实时更新中心点（并转换坐标系）
                        pivotX = (x1 + x2) / 2
                        pivotY = (y1 + y2) / 2
                        convertScreenToGL(pivotX, pivotY)

                        // 计算旋转
                        val dx = x2 - x1
                        val dy = y2 - y1
                        val currentAngle = atan2(-dy.toDouble(), dx.toDouble()).toFloat()
                        if (previousAngle != 0f) {
                            rotationAngle += Math.toDegrees((currentAngle - previousAngle).toDouble()).toFloat()
                        }
                        previousAngle = currentAngle

                        // 计算缩放
                        val currentDistance = sqrt(dx * dx + dy * dy)
                        scaleFactor = initialScaleFactor * (currentDistance / initialDistance)

                        updateViewMatrix()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    // 重置状态
                    previousAngle = 0f
                    initialDistance = 0f
                    true
                }
                else -> false
            }
        }

    }

    private fun translateMap(dx: Float, dy: Float) {
        translateX += dx
        translateY += dy
        updateViewMatrix()
    }

    private fun convertScreenToGL(screenX: Float, screenY: Float) {
        // 假设 orthoM 投影范围为 (0, width, 0, height)
        pivotX = screenX  // X方向一致
        pivotY = glSurfaceView.height - screenY  // Y轴翻转（屏幕坐标系 → OpenGL坐标系）
    }

    private fun updateViewMatrix() {
        Matrix.setIdentityM(viewMatrix, 0)

        // 1. 应用全局平移（用户拖拽）
        Matrix.translateM(viewMatrix, 0, translateX, translateY, 0f)

        // 2. 移动到当前操作中心点
        Matrix.translateM(viewMatrix, 0, pivotX, pivotY, 0f)

        // 3. 应用旋转和缩放
        Matrix.rotateM(viewMatrix, 0, rotationAngle, 0f, 0f, 1f)
        Matrix.scaleM(viewMatrix, 0, scaleFactor, scaleFactor, 1f)

        // 4. 移回原点
        Matrix.translateM(viewMatrix, 0, -pivotX, -pivotY, 0f)

        glSurfaceView.requestRender()
    }

    // 标记点数据类
    data class MapMarker(
        val x: Float,
        val y: Float,
        val iconResId: Int,
        val text: String,
        val textColor: Int = Color.WHITE
    )

    // 标记点列表
    private val markers = listOf(
        MapMarker(100f, 200f, R.drawable.ic_launcher_background, "位置1"),
        MapMarker(300f, 400f, R.drawable.ic_launcher_background, "位置2"),
        // 添加更多标记点...
    )

    // 标记点纹理
    private val markerTextures = mutableMapOf<Int, Int>()

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

                    val bitmap = loadVectorDrawableAsBitmap(this@TestActivity, marker.iconResId, 24.dp.toInt(), 24.dp.toInt())
                    Log.e(TAG, "bitmap: $bitmap")
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                    bitmap?.recycle()

                    markerTextures[marker.iconResId] = textureHandle[0]
                }
            }
        }
    }

    private fun loadVectorDrawableAsBitmap(context: Context, @DrawableRes resId: Int, width: Int, height: Int): Bitmap? {
        val vectorDrawable = ContextCompat.getDrawable(context, resId) as? VectorDrawable
            ?: return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)

        return bitmap
    }

    private fun drawMarkers() {
        markers.forEach { marker ->
            drawMarkerIcon(marker)
            drawMarkerText(marker)
        }
    }

    private fun drawMarkerIcon(marker: MapMarker) {
        val textureId = markerTextures[marker.iconResId] ?: return

        // 图标大小
        val iconSize = 32f * scaleFactor

        // 计算图标顶点
        val left = marker.x - iconSize / 2
        val right = marker.x + iconSize / 2
        val top = marker.y - iconSize / 2
        val bottom = marker.y + iconSize / 2

        val vertices = floatArrayOf(
            // 顶点坐标     // 纹理坐标（修正）
            left,  top,    0f, 1f,  // 左上 → 左下
            right, top,    1f, 1f,  // 右上 → 右下
            left,  bottom, 0f, 0f,  // 左下 → 左上
            right, bottom, 1f, 0f   // 右下 → 右上
        )

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
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, 0f)

        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

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
        // 1. 准备文字绘制参数
        val textSize = 24f * scaleFactor // 根据缩放调整文字大小
        val textPadding = 5f * scaleFactor // 文字与图标的间距

        // 2. 计算文字位置（在图标上方）
        val textX = marker.x
        val textY = marker.y - (32f * scaleFactor / 2) - textPadding // 32f是图标高度

        // 3. 创建Paint对象设置文字样式
        val paint = Paint().also {
            it.color = marker.textColor
            it.textSize = textSize
            it.isAntiAlias = true
            it.textAlign = Paint.Align.CENTER
            it.typeface = Typeface.DEFAULT_BOLD
        }

        // 4. 测量文字尺寸
        val textWidth = paint.measureText(marker.text)
        val textHeight = paint.descent() - paint.ascent()

        // 5. 创建文字位图（带透明背景）
        val bitmap = Bitmap.createBitmap(
            textWidth.toInt() + 2, // 加2避免边缘裁剪
            textHeight.toInt() + 2,
            Bitmap.Config.ARGB_8888
        )

        // 6. 绘制文字到位图
        Canvas(bitmap).apply {
            drawText(
                marker.text,
                width / 2f,
                height / 2f - (paint.ascent() + paint.descent()) / 2,
                paint
            )
        }

        // 7. 生成OpenGL纹理
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)

        if (textureIds[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        }
        bitmap.recycle()

        // 8. 计算文字渲染的顶点坐标
        val left = textX - textWidth / 2
        val right = textX + textWidth / 2
        val top = textY - textHeight / 2
        val bottom = textY + textHeight / 2

        val vertices = floatArrayOf(
            // 顶点坐标       // 纹理坐标
            left,  bottom,  0f, 0f,  // 左下
            right, bottom,  1f, 0f,  // 右下
            left,  top,     0f, 1f,  // 左上
            right, top,     1f, 1f   // 右上
        )

        // 9. 准备顶点缓冲区
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }

        // 10. 使用文字着色器程序
        GLES20.glUseProgram(textProgram)

        // 11. 获取着色器变量位置
        val positionHandle = GLES20.glGetAttribLocation(textProgram, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(textProgram, "vTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(textProgram, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(textProgram, "uTexture")
        val textColorHandle = GLES20.glGetUniformLocation(textProgram, "uTextColor")

        // 12. 启用顶点属性
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // 13. 设置顶点数据
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle, 2,
            GLES20.GL_FLOAT, false,
            16, vertexBuffer
        )

        vertexBuffer.position(2)
        GLES20.glVertexAttribPointer(
            texCoordHandle, 2,
            GLES20.GL_FLOAT, false,
            16, vertexBuffer
        )

        // 14. 计算MVP矩阵
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        // 15. 传递统一变量
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
        GLES20.glUniform1i(textureHandle, 0)

        // 设置文字颜色（使用原始颜色，不修改透明度）
        GLES20.glUniform4f(
            textColorHandle,
            1f, 1f, 1f, 1f // 保持原始颜色，因为位图已经包含颜色信息
        )

        // 16. 启用混合实现透明效果
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 17. 绘制文字
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 18. 清理状态
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glDisable(GLES20.GL_BLEND)

        // 19. 删除临时纹理
        GLES20.glDeleteTextures(1, textureIds, 0)
    }

}