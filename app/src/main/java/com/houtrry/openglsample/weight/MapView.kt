package com.houtrry.openglsample.weight

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.houtrry.openglsample.R
import com.houtrry.openglsample.render.MapRender
import com.houtrry.openglsample.shaper.BaseShaper
import com.houtrry.openglsample.shaper.MapShaper
import com.houtrry.openglsample.utils.readRawText

class MapView(context: Context?, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val mapRender: MapRender by lazy { MapRender() }

    init {
        setEGLContextClientVersion(2)
        setRenderer(mapRender)
        renderMode = RENDERMODE_WHEN_DIRTY
        context?.let {
            addShaper(
                MapShaper(
                    BitmapFactory.decodeResource(it.resources, R.drawable.optemap_217k),
                    it.readRawText(R.raw.map_vertex),
                    it.readRawText(R.raw.map_fragment)
                )
            )
        }
    }

    fun addShaper(shaper: BaseShaper) {
        mapRender.addShaper(shaper)
        requestRender()
    }

    fun removeShaper(shaper: BaseShaper) {
        mapRender.removeShaper(shaper)
    }

    fun requestRenderIfNeed() {
        if (renderMode == RENDERMODE_WHEN_DIRTY) {
            requestRender()
        }
    }
}