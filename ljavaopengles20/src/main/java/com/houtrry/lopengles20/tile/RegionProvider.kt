package com.houtrry.lopengles20.tile

interface RegionProvider {
    fun obtainRegion(x: Int, y: Int, width: Int, height: Int): RegionBuffer?
}