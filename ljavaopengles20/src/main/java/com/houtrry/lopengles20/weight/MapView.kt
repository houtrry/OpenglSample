package com.houtrry.lopengles20.weight

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.content.res.TypedArray
import com.houtrry.lopengles20.render.MapRender
import com.houtrry.common_map.utils.getAssertBitmap
import com.houtrry.common_map.utils.notNull
import com.houtrry.lopengles20.R
import com.houtrry.lopengles20.layer.*

open class MapView(context: Context?, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    private val mapRender: MapRender by lazy { MapRender(context) { requestRenderIfNeed() } }
    private var useDefaultLayersFlag: Boolean = true
    private var defaultLayersAdded: Boolean = false
    private var useCameraControlLayerFlag: Boolean = true

    init {
        setEGLContextClientVersion(2)
        setRenderer(mapRender)
        renderMode = RENDERMODE_WHEN_DIRTY
        // 允许通过XML属性关闭默认图层
        if (attrs != null && context != null) {
            val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.MapView)
            useDefaultLayersFlag = a.getBoolean(R.styleable.MapView_useDefaultLayers, true)
            useCameraControlLayerFlag = a.getBoolean(R.styleable.MapView_useCameraControlLayer, true)
            a.recycle()
        }

        // 延迟到消息队列，允许代码创建后立刻调用 setUseDefaultLayers(false)
        post { setupDefaultLayersIfNeeded() }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return event?.let { mapRender.onTouchEvent(event) } ?: false
    }

    open fun addLayer(shaper: ILayer) {
        mapRender.addLayer(shaper)
        requestRender()
    }

    fun addCameraControlLayer() {
        mapRender.addLayer(CameraControlLayer(mapRender))
        requestRender()
    }

    fun addLayers(vararg layers: ILayer) {
        layers.forEach { mapRender.addLayer(it) }
        requestRender()
    }

    fun removeLayer(shaper: ILayer) {
        mapRender.removeLayer(shaper)
    }

    fun clearLayers() {
        mapRender.clearLayers()
        requestRender()
    }

    fun replaceLayers(vararg layers: ILayer) {
        mapRender.clearLayers()
        layers.forEach { mapRender.addLayer(it) }
        requestRender()
    }

    /**
     * 代码创建 MapView 时，可在构造后立刻调用以关闭默认图层。
     * 需在默认图层实际添加前调用（即首次 frame 前）。
     */
    fun setUseDefaultLayers(useDefault: Boolean) {
        useDefaultLayersFlag = useDefault
        // 若还未添加过且改为 true，可以立即添加
        if (useDefault && !defaultLayersAdded) {
            setupDefaultLayersIfNeeded()
        }
    }

    private fun setupDefaultLayersIfNeeded() {
        if (defaultLayersAdded || !useDefaultLayersFlag) return
        notNull(context, context?.resources) { ctx, resources ->
            if (useCameraControlLayerFlag) {
                addLayer(CameraControlLayer(mapRender))
            }
        }
        defaultLayersAdded = true
    }

    /**
     * 控制是否自动添加相机控制层（默认 true）。
     * 若在默认层尚未添加前调用且设为 true，会立即添加；设为 false 则不添加。
     */
    fun setUseCameraControlLayer(use: Boolean) {
        useCameraControlLayerFlag = use
        if (use && !defaultLayersAdded) {
            setupDefaultLayersIfNeeded()
        }
    }

    fun requestRenderIfNeed() {
        if (renderMode == RENDERMODE_WHEN_DIRTY) {
            requestRender()
        }
    }

    override fun onDetachedFromWindow() {
        // 确保在 GL 线程执行销毁
        queueEvent {
            mapRender.destroy()
        }
        super.onDetachedFromWindow()
    }
}