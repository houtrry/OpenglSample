package com.houtrry.lopengles20.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView.Renderer
import android.util.Log
import android.view.MotionEvent
import com.houtrry.lopengles20.R
import com.houtrry.lopengles20.data.MapMatrix
import com.houtrry.lopengles20.layer.ILayer
import com.houtrry.lopengles20.utils.PerfMetrics
import com.houtrry.lopengles20.utils.OpenglUtils
import com.houtrry.common_map.utils.readRawText
import java.util.concurrent.CopyOnWriteArrayList
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MapRender(val context: Context?, private val renderCallback: () -> Unit) : Renderer {

    companion object {
        private const val TAG = "MapRender"
    }

    private val layers = CopyOnWriteArrayList<ILayer>()
    private var mPrograms: Int = 0

    private val mapMatrix = MapMatrix()
    private var viewportWidth: Int = 0
    private var viewportHeight: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 1.0f, 1.0f, 1f)
        if (context == null) {
            return
        }
        PerfMetrics.init(context)
        val vertexShaderCode = context.readRawText(R.raw.map_vertex)
        val fragmentShaderCode = context.readRawText(R.raw.map_fragment)
        //编译顶点着色器
        val vertexShaper: Int = OpenglUtils.loadShaper(
            GLES20.GL_VERTEX_SHADER, vertexShaderCode
        ) ?: kotlin.run {
            Log.e(TAG, "compile vertex shape failure")
            return
        }
        Log.d(TAG, "vertexShaper: $vertexShaper")
        //编译片源着色器
        val fragmentShaper: Int = OpenglUtils.loadShaper(
            GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode
        ) ?: kotlin.run {
            Log.e(TAG, "compile fragment shape failure")
            return
        }
        Log.d(TAG, "fragmentShaper: $fragmentShaper")
        //创建着色器程序
        mPrograms = OpenglUtils.linkProgram(
            vertexShaper, fragmentShaper
        ) ?: kotlin.run {
            Log.e(TAG, "link program failure")
            return
        }
        Log.d(TAG, "mPrograms: $mPrograms")
        if (!OpenglUtils.isValidateProgram(mPrograms)) {
            Log.e(TAG, "program status isn`t validate")
            return
        }
        GLES20.glUseProgram(mPrograms)
        context.let { ctx ->
            layers.forEach { it.onCreate(ctx, mPrograms, mapMatrix) }
        }
        Log.d(TAG, "onSurfaceCreated end")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        Log.d(TAG, "onSurfaceChanged, width: $width, height: $height")
        viewportWidth = width
        viewportHeight = height
        layers.forEach { it.onSizeChange(width, height) }
    }

    override fun onDrawFrame(gl: GL10?) {
        if (PerfMetrics.enabled) {
            PerfMetrics.onFrameStart()
        }
        val frameStartNs = System.nanoTime()
        val cpuStartNs = android.os.Debug.threadCpuTimeNanos()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        synchronized(mapMatrix) {
            layers.forEach {
                it.onDraw()
                if (PerfMetrics.enabled) {
                    PerfMetrics.incrementDrawCalls()
                }
            }
        }
        GLES20.glDisable(GLES20.GL_BLEND)
        if (PerfMetrics.enabled) {
            PerfMetrics.onFrameEnd(frameStartNs, cpuStartNs)
        }
    }

    fun addLayer(layer : ILayer) {
        layers.add(layer)

        // 为EnhancedMapLayer设置重绘回调
        if (layer is com.houtrry.lopengles20.layer.EnhancedMapLayer) {
            layer.setRenderCallback { requestRender() }
        }

        // 如果GL上下文已就绪，立即初始化该图层
        if (mPrograms != 0 && context != null) {
            try {
                layer.onCreate(context, mPrograms, mapMatrix)
                if (viewportWidth > 0 && viewportHeight > 0) {
                    layer.onSizeChange(viewportWidth, viewportHeight)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "addLayer onCreate error: ${t.message}")
            }
        }
    }

    fun getMapMatrix() = mapMatrix

    fun removeLayer(layer: ILayer) {
        try {
            layer.onDestroy()
        } catch (t: Throwable) {
            Log.w(TAG, "onDestroy error: ${t.message}", t)
        }
        layers.remove(layer)
    }

    fun clearLayers() {
        layers.forEach {
            try {
                it.onDestroy()
            } catch (t: Throwable) {
                Log.w(TAG, "onDestroy error: ${t.message}")
            }
        }
        layers.clear()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        run loop@ {
            layers.forEach {
                if (it.onTouchEvent(event)) {
                    return true
                }
            }
        }
        return false
    }

    fun requestRender() {
        renderCallback.invoke()
    }

    fun destroy() {
        // 在 GL 线程销毁所有 Layer
        layers.forEach {
            try {
                it.onDestroy()
            } catch (t: Throwable) {
                Log.w(TAG, "onDestroy error: ${t.message}")
            }
        }
        layers.clear()
        if (mPrograms != 0) {
            GLES20.glUseProgram(0)
            GLES20.glDeleteProgram(mPrograms)
            mPrograms = 0
        }
        if (PerfMetrics.enabled) {
            PerfMetrics.flush()
            PerfMetrics.shutdown()
        }
    }
}