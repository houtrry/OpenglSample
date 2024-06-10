package com.houtrry.openglsample.shaper

import android.content.Context

interface IShaper {

    fun onCreate(context: Context)

    fun onSizeChange()

    fun onDraw()
}