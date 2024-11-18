package com.houtrry.openglsample.weight

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.houtrry.openglsample.R
import com.houtrry.openglsample.render.MapRender
import com.houtrry.openglsample.shaper.ILayer
import com.houtrry.openglsample.shaper.RobotLayer
import com.houtrry.openglsample.shaper.TestMapLayer
import com.houtrry.openglsample.utils.notNull
import com.houtrry.openglsample.utils.readRawText

class MapView(context: Context?, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val mapRender: MapRender by lazy { MapRender(context) }

    init {
        setEGLContextClientVersion(2)
        setRenderer(mapRender)
        renderMode = RENDERMODE_CONTINUOUSLY
        notNull(context, context?.resources) { ctx, resources ->
            addLayer(
                TestMapLayer(
                    BitmapFactory.decodeResource(resources, R.drawable.optemap_217k),
                )
            )
            addLayer(
                RobotLayer(
                    BitmapFactory.decodeResource(resources, R.mipmap.robot),
                )
            )
        }
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