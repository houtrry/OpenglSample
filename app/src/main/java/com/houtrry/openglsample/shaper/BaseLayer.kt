package com.houtrry.openglsample.shaper

import android.content.Context

open abstract class BaseLayer: ILayer {

    protected var viewWidth: Int = 0
    protected var viewHeight: Int = 0
    protected var program = 0
    protected lateinit var context: Context

    override fun onCreate(context: Context, program: Int) {
        this.program = program
        this.context = context
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
}