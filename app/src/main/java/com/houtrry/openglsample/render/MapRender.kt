package com.houtrry.openglsample.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView.Renderer
import com.houtrry.openglsample.shaper.BaseShaper
import java.util.concurrent.CopyOnWriteArrayList
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MapRender : Renderer {

    private val shapers = CopyOnWriteArrayList<BaseShaper>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        shapers.forEach { it.onCreate() }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        shapers.forEach { it.onSizeChange() }
    }

    override fun onDrawFrame(gl: GL10?) {
        shapers.forEach { it.onDraw() }
    }

    fun addShaper(shaper:BaseShaper) {
        shapers.add(shaper)
    }

    fun removeShaper(shaper:BaseShaper) {
        shapers.remove(shaper)
    }

}