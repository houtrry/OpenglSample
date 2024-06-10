package com.houtrry.lopengl.view

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer {

    companion object {
        // Used to load the 'lopengl' library on application startup.
        init {
            System.loadLibrary("lopengl")
        }
    }

    init {
//        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        ndkCreate()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        ndkResize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        ndkDraw()
    }

    private external fun ndkCreate()

    private external fun ndkResize(width: Int, height: Int)

    private external fun ndkDraw()
}