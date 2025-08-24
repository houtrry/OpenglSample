package com.houtrry.openglsample.activity

import android.app.Activity
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.houtrry.common_map.data.PerformanceStats
import com.houtrry.common_map.data.RenderingScenario
import com.houtrry.common_map.data.TextStyle
import com.houtrry.common_map.text.HybridTextRenderer
import com.houtrry.common_map.text.TextRenderingFactory
import com.houtrry.common_map.text.TextRenderingUtils
import com.houtrry.openglsample.R
import java.util.Timer
import java.util.TimerTask
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.ceil
import kotlin.math.sqrt

class TextBenchmarkActivity : Activity() {

	private lateinit var glSurfaceView: GLSurfaceView
	private lateinit var renderer: BenchRenderer
	private lateinit var statusText: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		glSurfaceView = GLSurfaceView(this).apply {
			setEGLContextClientVersion(2)
		}

		statusText = TextView(this).apply {
			text = "准备中..."
			textSize = 12f
		}

		val btn100 = Button(this).apply { text = "100" }
		val btn500 = Button(this).apply { text = "500" }
		val btn1000 = Button(this).apply { text = "1000" }
		val btn2000 = Button(this).apply { text = "2000" }

		val toolbar = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			setBackgroundColor(Color.LTGRAY)
			setPadding(16, 16, 16, 16)
			addView(btn100)
			addView(btn500)
			addView(btn1000)
			addView(btn2000)
			addView(statusText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
		}

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			addView(glSurfaceView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
			addView(toolbar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
		}

		setContentView(root)

		val config = TextRenderingUtils.getRecommendedConfig(this)
		renderer = BenchRenderer(this, RenderingScenario.NewMapScenario, config)
		glSurfaceView.setRenderer(renderer)
		glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

		btn100.setOnClickListener { setCountAndRender(100) }
		btn500.setOnClickListener { setCountAndRender(500) }
		btn1000.setOnClickListener { setCountAndRender(1000) }
		btn2000.setOnClickListener { setCountAndRender(2000) }

		startStatusTimer()
	}

	private fun setCountAndRender(count: Int) {
		glSurfaceView.queueEvent {
			renderer.setMarkerCount(count)
		}
		glSurfaceView.requestRender()
	}

	private fun startStatusTimer() {
		Timer().schedule(object : TimerTask() {
			override fun run() {
				runOnUiThread {
					val stats: PerformanceStats = renderer.getPerformanceStats()
					val status = renderer.getRenderingStatus()
					statusText.text = "数量:${stats.textCount} | 策略:${status.strategy} | Canvas:${status.canvasTextCount} | SDF:${status.sdfTextCount} | 渲染:${"%.1f".format(stats.renderTime)}ms"
				}
			}
		}, 1200, 1500)
	}

	override fun onResume() {
		super.onResume()
		glSurfaceView.onResume()
	}

	override fun onPause() {
		super.onPause()
		glSurfaceView.onPause()
	}
}

private class BenchRenderer(
	private val activity: Activity,
	private val scenario: RenderingScenario,
	private val config: com.houtrry.common_map.text.HybridConfig
) : GLSurfaceView.Renderer {

	private val projectionMatrix = FloatArray(16)
	private val viewMatrix = FloatArray(16)
	private val mvpMatrix = FloatArray(16)
	private lateinit var textRenderer: HybridTextRenderer

	override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
		GLES20.glClearColor(0.15f, 0.18f, 0.22f, 1f)
		
		// 在OpenGL上下文准备好后初始化文字渲染器
		textRenderer = TextRenderingFactory.createRecommendedRenderer(activity, scenario)
	}

	override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
		GLES20.glViewport(0, 0, width, height)
		val ratio = width.toFloat() / height.toFloat()
		Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 100f)
		Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 20f, 0f, 0f, 0f, 0f, 1f, 0f)
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
	}

	override fun onDrawFrame(gl: GL10?) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
		if (::textRenderer.isInitialized) {
			textRenderer.render(mvpMatrix)
		}
	}

	fun setMarkerCount(count: Int) {
		if (::textRenderer.isInitialized) {
			textRenderer.clear()
			val items = generateBubbleIconTexts(count)
			textRenderer.addTexts(items)
		}
	}

	private fun generateBubbleIconTexts(count: Int): List<com.houtrry.common_map.data.TextInfo> {
		val side = ceil(sqrt(count.toDouble())).toInt().coerceAtLeast(1)
		val gap = 0.6
		val startOffset = -side / 2.0 * gap
		val style = TextStyle(fontSize = 14f, textColor = Color.WHITE)
		val bubbleStyle = TextRenderingUtils.BubbleStyles.INFO
		val list = ArrayList<com.houtrry.common_map.data.TextInfo>(count)
		var added = 0
		for (r in 0 until side) {
			for (c in 0 until side) {
				if (added >= count) break
				val x = startOffset + c * gap
				val y = startOffset + r * gap
				list.add(
					TextRenderingUtils.createBubbleIconTextInfo(
						content = "POI-" + added,
						x = x,
						y = y,
						iconResourceId = R.mipmap.robot,
						bubbleStyle = bubbleStyle,
						iconSize = Pair(20, 20),
						style = style
					)
				)
				added++
			}
		}
		return list
	}

	fun getRenderingStatus() = if (::textRenderer.isInitialized) {
		textRenderer.getRenderingStatus()
	} else {
		com.houtrry.common_map.text.RenderingStatus("Initializing", 0, 0, 0, 0)
	}
	
	fun getPerformanceStats(): PerformanceStats = if (::textRenderer.isInitialized) {
		textRenderer.getPerformanceStats()
	} else {
		PerformanceStats(0f, 0L, 0, 0)
	}
}


