package com.houtrry.openglsample.weight

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.houtrry.openglsample.R
import com.houtrry.openglsample.render.MapRender
import com.houtrry.openglsample.shaper.CameraControlLayer
import com.houtrry.openglsample.shaper.ILayer
import com.houtrry.openglsample.shaper.RobotLayer
import com.houtrry.openglsample.shaper.MapLayer
import com.houtrry.openglsample.utils.getAssertBitmap
import com.houtrry.openglsample.utils.notNull

class MapView(context: Context?, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val mapRender: MapRender by lazy { MapRender(context) { requestRenderIfNeed() } }

    init {
        setEGLContextClientVersion(2)
        setRenderer(mapRender)
        renderMode = RENDERMODE_WHEN_DIRTY
        notNull(context, context?.resources) { ctx, resources ->
            addLayer(CameraControlLayer(mapRender))
            addLayer(
                MapLayer(
//                    BitmapFactory.decodeResource(resources, R.drawable.optemap_22k),
//                    BitmapFactory.decodeResource(resources, R.mipmap.t4),
//                    ctx.getAssertBitmap("4.png")
                    ctx.getAssertBitmap("optemap_22k.png")
                )
            )
            addLayer(
                RobotLayer(
                    BitmapFactory.decodeResource(resources, R.mipmap.robot),
                )
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return event?.let { mapRender.onTouchEvent(event) } ?: false
    }

    private fun addLayer(shaper: ILayer) {
        mapRender.addLayer(shaper)
        requestRender()
    }

    fun removeLayer(shaper: ILayer) {
        mapRender.removeLayer(shaper)
    }

    fun requestRenderIfNeed() {
        if (renderMode == RENDERMODE_WHEN_DIRTY) {
            requestRender()
        }
    }
}