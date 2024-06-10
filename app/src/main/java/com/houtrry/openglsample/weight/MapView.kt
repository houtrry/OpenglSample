package com.houtrry.openglsample.weight

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.houtrry.openglsample.R
import com.houtrry.openglsample.render.MapRender
import com.houtrry.openglsample.shaper.IShaper
import com.houtrry.openglsample.shaper.BitmapShaper
import com.houtrry.openglsample.shaper.MapShaper
import com.houtrry.openglsample.shaper.TestMapShaper
import com.houtrry.openglsample.utils.getVectorDrawable
import com.houtrry.openglsample.utils.notNull
import com.houtrry.openglsample.utils.readRawText

class MapView(context: Context?, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val mapRender: MapRender by lazy { MapRender(context) }

    init {
        setEGLContextClientVersion(2)
        setRenderer(mapRender)
        renderMode = RENDERMODE_WHEN_DIRTY
        notNull(context, context?.resources) { ctx, resources ->
            addShaper(
                TestMapShaper(
                    BitmapFactory.decodeResource(resources, R.drawable.optemap_217k),
//                    BitmapFactory.decodeResource(it.resources, R.mipmap.ic_launcher),
                    ctx.readRawText(R.raw.map_vertex),
                    ctx.readRawText(R.raw.map_fragment)
                )
            )
//            ctx.getVectorDrawable(R.drawable.ic_launcher_foreground)?.let {
//                addShaper(BitmapShaper(it))
//            }
        }
    }

    private fun addShaper(shaper: IShaper) {
        mapRender.addShaper(shaper)
        requestRender()
    }

    fun removeShaper(shaper: IShaper) {
        mapRender.removeShaper(shaper)
    }

    fun requestRenderIfNeed() {
        if (renderMode == RENDERMODE_WHEN_DIRTY) {
            requestRender()
        }
    }
}