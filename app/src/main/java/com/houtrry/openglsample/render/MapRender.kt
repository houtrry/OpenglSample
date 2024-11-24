package com.houtrry.openglsample.render

import android.content.Context
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView.Renderer
import android.util.Log
import android.view.MotionEvent
import com.houtrry.openglsample.R
import com.houtrry.openglsample.data.MapMatrix
import com.houtrry.openglsample.data.Point
import com.houtrry.openglsample.shaper.ILayer
import com.houtrry.openglsample.utils.OpenglUtils
import com.houtrry.openglsample.utils.readRawText
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

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        if (context == null) {
            return
        }
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
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        layers.forEach { it.onSizeChange(width, height) }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        synchronized(mapMatrix) {
            layers.forEach { it.doBeforeDraw() }
            layers.forEach { it.onDraw() }
            layers.forEach { it.doAfterDraw() }
        }
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    fun addLayer(layer :ILayer) {
        layers.add(layer)
    }

    fun getMapMatrix() = mapMatrix

    fun removeLayer(layer:ILayer) {
        layers.remove(layer)
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
}