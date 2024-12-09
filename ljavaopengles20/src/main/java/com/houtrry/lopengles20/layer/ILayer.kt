package com.houtrry.lopengles20.layer

import android.content.Context
import android.view.MotionEvent
import com.houtrry.lopengles20.data.MapMatrix

interface ILayer {

    fun onCreate(context: Context, program: Int, mapMatrix: MapMatrix)

    fun onSizeChange(width: Int, height: Int)

    fun doBeforeDraw()

    fun onDraw()

    fun doAfterDraw()

    fun onTouchEvent(event: MotionEvent): Boolean
}