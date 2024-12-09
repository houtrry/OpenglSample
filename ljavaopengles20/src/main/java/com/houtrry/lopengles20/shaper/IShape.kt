package com.houtrry.lopengles20.shaper

import com.houtrry.lopengles20.data.MapMatrix

interface IShape {
    fun draw(program: Int, mapMatrix: MapMatrix)
}