package com.houtrry.lopengles20.utils

import android.opengl.Matrix
import com.houtrry.common_map.data.Vector3
import kotlin.math.sqrt

fun FloatArray.identityM(offset: Int = 0): FloatArray {
    Matrix.setIdentityM(this, offset)
    return this
}

fun FloatArray.getTranslation(): FloatArray {
    return floatArrayOf(this[12], this[13], this[14])
}

fun FloatArray.getScale(): FloatArray {
    val scaleX = sqrt(this[0] * this[0] + this[1] * this[1] + this[2] * this[2]) // X轴基向量长度
    val scaleY = sqrt(this[4] * this[4] + this[5] * this[5] + this[6] * this[6]) // Y轴基向量长度
    val scaleZ = sqrt(this[8] * this[8] + this[9] * this[9] + this[10] * this[10]) // Z轴基向量长度
    return floatArrayOf(scaleX, scaleY, scaleZ)
}

// 获取矩阵中的旋转分量
fun FloatArray.getRotation(): FloatArray {
    val scale = this.getScale()
    return floatArrayOf(
        this[0] / scale[0], this[1] / scale[0], this[2] / scale[0], 0f,
        this[4] / scale[1], this[5] / scale[1], this[6] / scale[1], 0f,
        this[8] / scale[2], this[9] / scale[2], this[10] / scale[2], 0f,
        0f, 0f, 0f, 1f
    )
}

fun FloatArray.getTransformMatrixWithoutScale(scale: Float, matrix: FloatArray): FloatArray {
    this.copyInto(matrix)
    val sx = calcFloatArraySqrt(matrix[0], matrix[4])
    val sy = calcFloatArraySqrt(matrix[1], matrix[5])
    matrix[0] *= scale / sx
    matrix[4] *= scale / sx
    matrix[1] *= scale / sy
    matrix[5] *= scale / sy
    return matrix
}

fun FloatArray.getTransformMatrixOnlyTranslate(vector3:Vector3, matrix: FloatArray): FloatArray {
    matrix.identityM()
    Matrix.translateM(matrix, 0, vector3.x.toFloat(), vector3.y.toFloat(), vector3.z.toFloat())
    Matrix.multiplyMM(matrix, 0, this, 0, matrix, 0)
    matrix[0] = 1f
    matrix[1] = 0f
    matrix[4] = 0f
    matrix[5] = 1f
    return matrix
}

//变长数组的每项平方求和后，取其开根值
private fun calcFloatArraySqrt(vararg args: Float): Float {
    return sqrt(args.sumOf { it.toDouble() * it }).toFloat()
}
