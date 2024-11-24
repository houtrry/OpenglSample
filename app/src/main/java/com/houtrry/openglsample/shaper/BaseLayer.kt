package com.houtrry.openglsample.shaper

import android.content.Context
import android.view.MotionEvent
import com.houtrry.openglsample.data.MapMatrix

abstract class BaseLayer: ILayer {

    protected var viewWidth: Int = 0
    protected var viewHeight: Int = 0
    protected var program = 0
    protected lateinit var context: Context
    protected lateinit var mapMatrix: MapMatrix

    override fun onCreate(context: Context, program: Int, mapMatrix: MapMatrix) {
        this.program = program
        this.context = context
        this.mapMatrix = mapMatrix
        onCreate()
    }

    abstract fun onCreate()

    override fun onSizeChange(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
    }

    override fun doBeforeDraw() {

    }

    override fun doAfterDraw() {

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }
}