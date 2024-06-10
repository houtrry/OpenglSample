package com.houtrry.openglsample.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView.Renderer
import com.houtrry.openglsample.shaper.IShaper
import java.util.concurrent.CopyOnWriteArrayList
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MapRender(val context: Context?) : Renderer {

    private val shapers = CopyOnWriteArrayList<IShaper>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        context?.let { ctx ->
            shapers.forEach { it.onCreate(ctx) }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        shapers.forEach { it.onSizeChange() }
    }

    override fun onDrawFrame(gl: GL10?) {
        shapers.forEach { it.onDraw() }
    }

    fun addShaper(shaper:IShaper) {
        shapers.add(shaper)
    }

    fun removeShaper(shaper:IShaper) {
        shapers.remove(shaper)
    }

}