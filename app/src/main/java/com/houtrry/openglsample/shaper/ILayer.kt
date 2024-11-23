package com.houtrry.openglsample.shaper

import android.content.Context
import android.view.MotionEvent
import com.houtrry.openglsample.data.MapMatrix

interface ILayer {

    fun onCreate(context: Context, program: Int)

    fun onSizeChange(width: Int, height: Int)

    fun doBeforeDraw()

    fun onDraw(mapMatrix: MapMatrix)

    fun doAfterDraw()

    fun onTouchEvent(event: MotionEvent): Boolean
}