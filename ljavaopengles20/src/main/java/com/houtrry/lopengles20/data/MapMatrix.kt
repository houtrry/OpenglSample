package com.houtrry.lopengles20.data

import android.graphics.PointF
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.utils.formatMatrixString
import kotlin.math.sqrt

/**
 * @author: houtrry
 * @time: 2024-11-19
 * @desc: [屏幕显示] = [投影矩阵] × [视图矩阵] × [分辨率矩阵] × [世界矩阵] × [对象矩阵]
 * mvpMatrix = projectionMatrix * viewMatrix * resolutionMatrix * worldMatrix * modelMatrix
 *
 */
class MapMatrix {
    companion object {
        private const val TAG = "MapMatrix"
    }

    private val modelMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16) // 用于变换的矩阵
    private val viewMatrix = FloatArray(16) //用户手势操作的变换
    private val worldMatrix = FloatArray(16) // 地图本身的变换
    private val resolutionMatrix = FloatArray(16)

    init {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 1f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
    }

    @Synchronized
    fun translate(translateX: Float, translateY: Float) {
        val vMatrix = floatArrayOf(translateX, translateY, 0f, 0f)
        val translateInverse = FloatArray(16)
        val translateMatrix = FloatArray(4)
        Matrix.invertM(translateInverse, 0, modelMatrix, 0)
        Matrix.multiplyMV(translateMatrix, 0, translateInverse, 0, vMatrix, 0)

        Matrix.translateM(modelMatrix, 0, translateMatrix[0], translateMatrix[1], 0f)
        Log.d(TAG, "translate, transformMatrix: ${modelMatrix.contentToString()}")
    }

    @Synchronized
    fun zoom(zoom: Float, focusX: Float = 0f, focusY: Float = 0f) {
        Matrix.translateM(modelMatrix, 0, focusX, focusY, 0f)
        Matrix.scaleM(modelMatrix, 0, zoom, zoom, 1f)
        Matrix.translateM(modelMatrix, 0, -focusX, -focusY, 0f)
    }

    @Synchronized
    fun scale(scaleX: Float, scaleY: Float, focusX: Float = 0f, focusY: Float = 0f) {
        if (scaleX == 0f && scaleY == 0f) {
            Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1f)
        } else {
            Matrix.translateM(modelMatrix, 0, focusX, focusY, 0f)
            Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1f)
            Matrix.translateM(modelMatrix, 0, -focusX, -focusY, 0f)
        }
    }

    @Synchronized
    fun rotate(rotate: Float, focusX: Float = 0f, focusY: Float = 0f) {
        Matrix.translateM(modelMatrix, 0, focusX, focusY, 0f)
        Matrix.rotateM(modelMatrix, 0, rotate, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, -focusX, -focusY, 0f)
    }

    @Synchronized
    fun rotateWithZoom(scale: Float, rotate: Float, focusX: Float = 0f, focusY: Float = 0f) {
        Matrix.translateM(modelMatrix, 0, focusX, focusY, 0f)
        Matrix.rotateM(modelMatrix, 0, rotate, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, scale, scale, 1f)
        Matrix.translateM(modelMatrix, 0, -focusX, -focusY, 0f)
    }

    fun getTransformMatrix() = modelMatrix

    fun getTranslateX() = modelMatrix[12]
    fun getTranslateY() = modelMatrix[13]
    fun getTranslateZ() = modelMatrix[14]

    fun testAutoRotate() {
        Matrix.rotateM(modelMatrix, 0, 0.5f, 0f, 0f, 1f)
    }

    fun orthoM(width: Int, height: Int) {
        val aspectRatio = width * 1f / height
        Matrix.orthoM(
            projectionMatrix, 0,
            -aspectRatio, aspectRatio,
            -1f, 1f,
            1f, 100.0f
        )
    }

    fun getViewMatrix() = viewMatrix

    fun getProjectionMatrix() = projectionMatrix

    fun getTransformMatrixWithoutScale(scale: Float, matrix: FloatArray) {
        Log.d(
            TAG,
            "scale: $scale, transformMatrix: ${modelMatrix.formatMatrixString()}"
        )

//        Matrix.setIdentityM(matrix, 0)
        modelMatrix.copyInto(matrix)
        val sx = calcFloatArraySqrt(matrix[0], matrix[4])
        val sy = calcFloatArraySqrt(matrix[1], matrix[5])
        matrix[0] *= scale / sx
        matrix[4] *= scale / sx
        matrix[1] *= scale / sy
        matrix[5] *= scale / sy
        Log.d(TAG, "sx:$sx, sy: $sy, transformMatrix: ${matrix.formatMatrixString()}")

    }

    //变长数组的每项平方求和后，取其开根值
    private fun calcFloatArraySqrt(vararg args: Float): Float {
        return sqrt(args.sumOf { it.toDouble() * it }).toFloat()
    }

    // 屏幕坐标转世界坐标
    fun screenToWorld(screenX: Float, screenY: Float, viewWidth: Int, viewHeight: Int): PointF {
        // 归一化设备坐标
        val ndcX = 2 * screenX / viewWidth - 1
        val ndcY = 1 - 2 * screenY / viewHeight

        // 创建逆MVP矩阵
        val mvp = FloatArray(16)
        Matrix.setIdentityM(mvp, 0)
        Matrix.multiplyMM(mvp, 0, worldMatrix, 0, mvp, 0)
        Matrix.multiplyMM(mvp, 0, viewMatrix, 0, mvp, 0)
        Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, mvp, 0)

        val invMvp = FloatArray(16)
        Matrix.invertM(invMvp, 0, mvp, 0)

        // 转换坐标
        val point = floatArrayOf(ndcX, ndcY, 0f, 1f)
        Matrix.multiplyMV(point, 0, invMvp, 0, point, 0)

        return PointF(point[0], point[1])
    }

}