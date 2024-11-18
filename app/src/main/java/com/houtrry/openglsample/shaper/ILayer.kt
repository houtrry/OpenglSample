package com.houtrry.openglsample.shaper

import android.content.Context

interface ILayer {

    fun onCreate(context: Context, program: Int)

    fun onSizeChange(width: Int, height: Int)

    fun doBeforeDraw()

    fun onDraw()

    fun doAfterDraw()
}