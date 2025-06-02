package com.houtrry.lopengles20.utils

import android.opengl.Matrix
import com.houtrry.lopengles20.data.MapMatrix

fun FloatArray.identityM(offset: Int = 0): FloatArray {
    Matrix.setIdentityM(this, offset)
    return this
}