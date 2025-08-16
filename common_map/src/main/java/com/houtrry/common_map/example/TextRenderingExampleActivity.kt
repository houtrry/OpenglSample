package com.houtrry.common_map.example

import android.app.Activity
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.houtrry.common_map.data.*
import com.houtrry.common_map.text.*
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

/**
 * 文字渲染示例Activity
 * 演示混合文字渲染器的使用方法
 */
class TextRenderingExampleActivity : Activity() {
    
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: ExampleRenderer
    private lateinit var addButton: Button
    private lateinit var clearButton: Button
    private lateinit var statusText: TextView
    private lateinit var inputText: EditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupUI()
        setupRenderer()
        startStatusMonitoring()
    }
    
    private fun setupUI() {
        // 创建GLSurfaceView
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        
        // 创建控制面板
        val controlPanel = createControlPanel()
        
        // 设置布局（简化示例，实际使用时建议用XML布局）
        val mainLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(glSurfaceView, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            addView(controlPanel, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        
        setContentView(mainLayout)
    }
    
    private fun createControlPanel(): View {
        val panel = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.LTGRAY)
        }
        
        // 输入框
        inputText = EditText(this).apply {
            hint = "输入文字内容"
            setText("地标${Random.nextInt(100)}")
        }
        
        // 添加按钮
        addButton = Button(this).apply {
            text = "添加文字"
            setOnClickListener { addRandomText() }
        }
        
        // 清空按钮
        clearButton = Button(this).apply {
            text = "清空"
            setOnClickListener { clearAllTexts() }
        }
        
        // 状态显示
        statusText = TextView(this).apply {
            text = "状态: 初始化中..."
            textSize = 12f
        }
        
        val buttonLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(addButton)
            addView(clearButton)
        }
        
        panel.addView(inputText, android.widget.LinearLayout.LayoutParams(0, 
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        panel.addView(buttonLayout)
        panel.addView(statusText)
        
        return panel
    }
    
    private fun setupRenderer() {
        // 创建示例场景（这里演示两种场景的选择）
        val scenario = if (shouldUseExistingMapScenario()) {
            // 扩展地图场景：预设一些文字
            val existingTexts = listOf(
                "北京", "上海", "广州", "深圳", "杭州",
                "苏州", "成都", "重庆", "西安", "武汉"
            )
            RenderingScenario.ExtendMapScenario(existingTexts)
        } else {
            // 新建地图场景
            RenderingScenario.NewMapScenario
        }
        
        // 根据设备性能获取推荐配置
        val config = TextRenderingUtils.getRecommendedConfig(this)
        
        // 创建渲染器
        renderer = ExampleRenderer(this, scenario, config)
        glSurfaceView.setRenderer(renderer)
    }
    
    private fun shouldUseExistingMapScenario(): Boolean {
        // 简化的场景选择逻辑，实际项目中应该根据业务需求决定
        return Random.nextBoolean()
    }
    
    private fun addRandomText() {
        val content = inputText.text.toString().ifEmpty { "随机文字${Random.nextInt(1000)}" }
        val x = Random.nextDouble(-10.0, 10.0)
        val y = Random.nextDouble(-10.0, 10.0)
        
        renderer.addText(content, x, y)
        
        // 更新输入框为下一个随机文字
        inputText.setText("地标${Random.nextInt(100)}")
    }
    
    private fun clearAllTexts() {
        renderer.clearAllTexts()
    }
    
    private fun startStatusMonitoring() {
        // 定期更新状态显示
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateStatusDisplay()
                }
            }
        }, 1000, 2000) // 每2秒更新一次
    }
    
    private fun updateStatusDisplay() {
        val status = renderer.getRenderingStatus()
        val stats = renderer.getPerformanceStats()
        
        val statusInfo = """
            策略: ${status.strategy} | 总数: ${stats.textCount}
            Canvas: ${status.canvasTextCount} | SDF: ${status.sdfTextCount} | 待迁移: ${status.pendingMigration}
            渲染: ${String.format("%.1f", stats.renderTime)}ms | 内存: ${stats.memoryUsage / 1024 / 1024}MB
        """.trimIndent().replace("\n", " ")
        
        statusText.text = statusInfo
    }
    
    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        renderer.release()
    }
}

/**
 * 示例渲染器
 * 演示如何在GLSurfaceView.Renderer中使用混合文字渲染器
 */
class ExampleRenderer(
    private val context: Activity,
    scenario: RenderingScenario,
    config: HybridConfig
) : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "ExampleRenderer"
    }
    
    private lateinit var textRenderer: HybridTextRenderer
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    
    // 文字样式
    private val defaultStyle = TextStyle(
        fontSize = 18f,
        textColor = Color.WHITE,
        alpha = 1f
    )
    
    private val highlightStyle = TextStyle(
        fontSize = 22f,
        textColor = Color.YELLOW,
        alpha = 1f
    )
    
    init {
        // 创建混合文字渲染器
        textRenderer = TextRenderingFactory.createRecommendedRenderer(
            context = context,
            scenario = scenario,
            config = config
        )
        
        Log.d(TAG, "示例渲染器创建完成")
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清除颜色
        GLES20.glClearColor(0.2f, 0.3f, 0.4f, 1.0f)
        
        // 启用深度测试
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        
        Log.d(TAG, "OpenGL Surface 创建完成")
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        // 设置投影矩阵
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 100f)
        
        // 设置视图矩阵
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 20f,  // 相机位置
            0f, 0f, 0f,   // 看向的点
            0f, 1f, 0f    // 上方向
        )
        
        // 计算MVP矩阵
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        
        Log.d(TAG, "Surface 尺寸变更: ${width}x${height}")
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // 清除颜色和深度缓冲
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        // 渲染文字
        textRenderer.render(mvpMatrix)
    }
    
    /**
     * 添加文字
     */
    fun addText(content: String, x: Double, y: Double) {
        val style = if (Random.nextFloat() < 0.2f) highlightStyle else defaultStyle
        
        val textInfo = TextRenderingUtils.createTextInfo(
            content = content,
            x = x, y = y, z = 0.0,
            style = style
        )
        
        // 在GL线程中执行
        (context.findViewById<GLSurfaceView>(android.R.id.content) as? GLSurfaceView)?.queueEvent {
            textRenderer.addText(textInfo)
            Log.d(TAG, "添加文字: $content 位置: ($x, $y)")
        }
    }
    
    /**
     * 清空所有文字
     */
    fun clearAllTexts() {
        (context.findViewById<GLSurfaceView>(android.R.id.content) as? GLSurfaceView)?.queueEvent {
            textRenderer.clear()
            Log.d(TAG, "清空所有文字")
        }
    }
    
    /**
     * 获取渲染状态
     */
    fun getRenderingStatus(): RenderingStatus {
        return textRenderer.getRenderingStatus()
    }
    
    /**
     * 获取性能统计
     */
    fun getPerformanceStats(): PerformanceStats {
        return textRenderer.getPerformanceStats()
    }
    
    /**
     * 释放资源
     */
    fun release() {
        textRenderer.release()
        Log.d(TAG, "渲染器资源已释放")
    }
} 