package com.houtrry.common_map.example

import android.app.Activity
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.houtrry.common_map.data.*
import com.houtrry.common_map.text.*
import com.houtrry.common_map.utils.TextMeasureUtils
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 多行文字和RTL支持示例Activity
 * 演示文字换行和从右到左文字的使用方法
 */
class MultiLineRTLExampleActivity : Activity() {
    
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: MultiLineRTLRenderer
    
    // UI控件
    private lateinit var textInput: EditText
    private lateinit var maxWidthSlider: SeekBar
    private lateinit var maxWidthLabel: TextView
    private lateinit var languageSpinner: Spinner
    private lateinit var alignmentSpinner: Spinner
    private lateinit var addButton: Button
    private lateinit var clearButton: Button
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupUI()
        setupRenderer()
        setupControls()
        
        // 添加示例文字
        addExampleTexts()
    }
    
    private fun setupUI() {
        // 创建GLSurfaceView
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        
        // 创建控制面板
        val controlPanel = createControlPanel()
        
        // 主布局
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(glSurfaceView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            addView(controlPanel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        
        setContentView(mainLayout)
    }
    
    private fun createControlPanel(): View {
        val scrollView = ScrollView(this)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.LTGRAY)
        }
        
        // 文字输入
        textInput = EditText(this).apply {
            hint = "输入文字内容"
            setText("这是一段长文字，用于测试自动换行功能。This is a long text for testing line wrapping.")
        }
        
        // 最大宽度控制
        val widthLabel = TextView(this).apply { 
            text = "最大宽度控制:" 
            textSize = 14f
        }
        
        maxWidthLabel = TextView(this).apply {
            text = "无限制"
            textSize = 12f
            setTextColor(Color.BLUE)
        }
        
        maxWidthSlider = SeekBar(this).apply {
            max = 500
            progress = 0 // 0表示无限制
        }
        
        // 语言选择
        val langLabel = TextView(this).apply { 
            text = "语言/方向:" 
            textSize = 14f
        }
        
        languageSpinner = Spinner(this).apply {
            adapter = ArrayAdapter.createFromResource(
                this@MultiLineRTLExampleActivity,
                android.R.array.select_dialog_items, // 临时使用系统数组
                android.R.layout.simple_spinner_item
            )
        }
        
        // 对齐方式
        val alignLabel = TextView(this).apply { 
            text = "对齐方式:" 
            textSize = 14f
        }
        
        alignmentSpinner = Spinner(this)
        
        // 按钮
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        addButton = Button(this).apply {
            text = "添加文字"
            layoutParams = LinearLayout.LayoutParams(0, 
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        clearButton = Button(this).apply {
            text = "清空"
            layoutParams = LinearLayout.LayoutParams(0, 
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        buttonLayout.addView(addButton)
        buttonLayout.addView(clearButton)
        
        // 状态显示
        statusText = TextView(this).apply {
            text = "状态: 准备就绪"
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }
        
        // 添加到面板
        panel.addView(widthLabel)
        panel.addView(maxWidthLabel)
        panel.addView(maxWidthSlider)
        panel.addView(langLabel)
        panel.addView(languageSpinner)
        panel.addView(alignLabel)
        panel.addView(alignmentSpinner)
        panel.addView(TextView(this).apply { text = "文字内容:"; textSize = 14f })
        panel.addView(textInput)
        panel.addView(buttonLayout)
        panel.addView(statusText)
        
        scrollView.addView(panel)
        return scrollView
    }
    
    private fun setupRenderer() {
        renderer = MultiLineRTLRenderer(this)
        glSurfaceView.setRenderer(renderer)
    }
    
    private fun setupControls() {
        // 最大宽度滑块
        maxWidthSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    maxWidthLabel.text = if (progress == 0) "无限制" else "${progress}px"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 语言选择器
        setupLanguageSpinner()
        
        // 对齐选择器
        setupAlignmentSpinner()
        
        // 添加按钮
        addButton.setOnClickListener {
            addCustomText()
        }
        
        // 清空按钮
        clearButton.setOnClickListener {
            renderer.clearAllTexts()
            updateStatus("已清空所有文字")
        }
        
        // 启动状态监控
        startStatusMonitoring()
    }
    
    private fun setupLanguageSpinner() {
        val languages = arrayOf("自动检测", "中文(LTR)", "英文(LTR)", "阿拉伯文(RTL)", "希伯来文(RTL)", "波斯文(RTL)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
    }
    
    private fun setupAlignmentSpinner() {
        val alignments = arrayOf("居中", "左对齐", "右对齐", "起始", "结束")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, alignments)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        alignmentSpinner.adapter = adapter
        alignmentSpinner.setSelection(0) // 默认居中
    }
    
    private fun addCustomText() {
        val content = textInput.text.toString()
        if (content.isEmpty()) {
            updateStatus("请输入文字内容")
            return
        }
        
        val maxWidth = if (maxWidthSlider.progress == 0) -1f else maxWidthSlider.progress.toFloat()
        val textDirection = getSelectedTextDirection()
        val textAlign = getSelectedTextAlign()
        
        val textInfo = createTextInfo(content, maxWidth, textDirection, textAlign)
        renderer.addText(textInfo)
        
        updateStatus("已添加文字: $content (${if (maxWidth > 0) "换行" else "不换行"})")
    }
    
    private fun getSelectedTextDirection(): TextDirection {
        return when (languageSpinner.selectedItemPosition) {
            0 -> TextDirection.AUTO
            1, 2 -> TextDirection.LTR
            3, 4, 5 -> TextDirection.RTL
            else -> TextDirection.AUTO
        }
    }
    
    private fun getSelectedTextAlign(): TextAlign {
        return when (alignmentSpinner.selectedItemPosition) {
            0 -> TextAlign.CENTER
            1 -> TextAlign.LEFT
            2 -> TextAlign.RIGHT
            3 -> TextAlign.START
            4 -> TextAlign.END
            else -> TextAlign.CENTER
        }
    }
    
    private fun createTextInfo(
        content: String, 
        maxWidth: Float, 
        direction: TextDirection, 
        align: TextAlign
    ): TextInfo {
        val style = TextStyle(
            fontSize = 16f,
            textColor = Color.BLACK,
            maxWidth = maxWidth,
            lineSpacing = 1.2f,
            textDirection = direction,
            textAlign = align
        )
        
        val x = kotlin.random.Random.nextDouble(-15.0, 15.0)
        val y = kotlin.random.Random.nextDouble(-10.0, 10.0)
        
        return if (direction == TextDirection.RTL) {
            TextRenderingUtils.createRTLTextInfo(
                content = content,
                x = x, y = y,
                style = style,
                forceRTL = true
            )
        } else if (maxWidth > 0) {
            TextRenderingUtils.createWrappedTextInfo(
                content = content,
                x = x, y = y,
                maxWidth = maxWidth,
                style = style
            )
        } else {
            TextRenderingUtils.createTextInfo(
                content = content,
                x = x, y = y,
                style = style
            )
        }
    }
    
    private fun addExampleTexts() {
        // 示例文字集合
        val examples = listOf(
            // 中文换行示例
            Triple(
                "这是一段用于测试中文自动换行功能的长文字内容，当文字超过指定宽度时会自动换行。",
                300f,
                TextDirection.LTR
            ),
            
            // 英文换行示例
            Triple(
                "This is a long English text used for testing automatic line wrapping functionality when text exceeds the specified width.",
                250f,
                TextDirection.LTR
            ),
            
            // 阿拉伯文示例
            Triple(
                "هذا نص باللغة العربية من اليمين إلى اليسار",
                -1f,
                TextDirection.RTL
            ),
            
            // 希伯来文示例
            Triple(
                "זה טקסט בעברית מימין לשמאל",
                -1f,
                TextDirection.RTL
            ),
            
            // 混合方向文字
            Triple(
                "Mixed text مختلط النص with different directions",
                200f,
                TextDirection.AUTO
            )
        )
        
        examples.forEachIndexed { index, (text, maxWidth, direction) ->
            val textInfo = createTextInfo(
                content = text,
                maxWidth = maxWidth,
                direction = direction,
                align = TextAlign.START
            )
            
            // 调整位置避免重叠
            val adjustedTextInfo = textInfo.copy(
                position = Vector3(
                    x = -10.0 + (index % 2) * 20.0,
                    y = -15.0 + index * 6.0,
                    z = 0.0
                )
            )
            
            renderer.addText(adjustedTextInfo)
        }
        
        updateStatus("已添加${examples.size}个示例文字")
    }
    
    private fun updateStatus(message: String) {
        statusText.text = "状态: $message"
    }
    
    private fun startStatusMonitoring() {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val stats = renderer.getPerformanceStats()
                    val status = renderer.getRenderingStatus()
                    
                    val statusInfo = """
                        文字: ${stats.textCount} | 复杂: ${stats.complexTextCount}
                        策略: ${status.strategy} | 渲染: ${String.format("%.1f", stats.renderTime)}ms
                    """.trimIndent().replace("\n", " ")
                    
                    statusText.text = "状态: $statusInfo"
                }
            }
        }, 2000, 3000) // 2秒后开始，每3秒更新
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
 * 多行文字和RTL示例渲染器
 */
class MultiLineRTLRenderer(
    private val context: Activity
) : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "MultiLineRTLRenderer"
    }
    
    private lateinit var textRenderer: HybridTextRenderer
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.9f, 0.9f, 0.95f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        
        // 创建支持多行和RTL的混合渲染器
        textRenderer = TextRenderingFactory.createRecommendedRenderer(
            context = context,
            scenario = RenderingScenario.NewMapScenario,
            config = TextRenderingUtils.getRecommendedConfig(context)
        )
        
        Log.d(TAG, "多行RTL渲染器创建完成")
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 100f)
        
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 30f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
        
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        
        Log.d(TAG, "Surface变更: ${width}x${height}")
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        // 渲染所有文字（自动处理换行和RTL）
        textRenderer.render(mvpMatrix)
        
        // 性能监控
        val stats = textRenderer.getPerformanceStats()
        if (stats.renderTime > 20f) {
            Log.w(TAG, "渲染时间较长: ${stats.renderTime}ms")
        }
    }
    
    fun addText(textInfo: TextInfo) {
        glSurfaceView.queueEvent {
            textRenderer.addText(textInfo)
            
            Log.d(TAG, """
                添加文字: ${textInfo.content}
                - 类型: ${textInfo.displayType}
                - 换行: ${textInfo.needsLineWrap()}
                - RTL: ${textInfo.isRTL()}
                - 最大宽度: ${textInfo.style.maxWidth}
                - 方向: ${textInfo.style.textDirection}
                - 对齐: ${textInfo.style.textAlign}
            """.trimIndent())
        }
    }
    
    fun clearAllTexts() {
        glSurfaceView.queueEvent {
            textRenderer.clear()
            Log.d(TAG, "清空所有文字")
        }
    }
    
    fun getPerformanceStats(): PerformanceStats {
        return textRenderer.getPerformanceStats()
    }
    
    fun getRenderingStatus(): RenderingStatus {
        return textRenderer.getRenderingStatus()
    }
    
    fun release() {
        textRenderer.release()
        Log.d(TAG, "渲染器资源已释放")
    }
    
    private val glSurfaceView: GLSurfaceView
        get() = context.findViewById(android.R.id.content)
} 