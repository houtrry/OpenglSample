package com.houtrry.lopengles20.weight

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.houtrry.lopengles20.render.MapRender
import com.houtrry.common_map.utils.getAssertBitmap
import com.houtrry.common_map.utils.notNull
import com.houtrry.lopengles20.layer.*

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
//                    BitmapFactory.decodeResource(rXesources, R.mipmap.t4),
//                    ctx.getAssertBitmap("4.png")
                    ctx.getAssertBitmap("optemap_22k.png")
                )
            )
            addLayer(
                RobotLayer(
                    ctx.getAssertBitmap("robot.png")
                )
            )
            addLayer(TextLayer())
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