package com.houtrry.lopengles20.layer

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.common_map.utils.glGetAttribLocation
import com.houtrry.common_map.utils.glGetUniformLocation
import com.houtrry.common_map.utils.toBuffer
import java.nio.ShortBuffer

class PointLayer: BaseLayer() {

    companion object {
        private const val TAG = "PointLayer"
    }

    private var glArrowTextureId: Int = 0
    private var positionLocation: Int = -1
    private var textureCoordinateLocation: Int = -1
    private var transformMatrixLocation: Int = -1

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )

    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()

//    private lateinit var arrowBitmapSize: BitmapSize
    private val transformMatrix: FloatArray = FloatArray(16)

    init {
        Log.d(TAG, "init start")
    }

    override fun onCreate() {
        // 仅缓存位置，若后续绘制点位网格再填充具体数据
        positionLocation = program.glGetAttribLocation("vPosition")
        textureCoordinateLocation = program.glGetAttribLocation("inputTextureCoordinate")
        transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")
    }
    private val mMVPMatrix = FloatArray(16) // MVP 矩阵

    override fun onDraw() {
        mapMatrix.getTransformMatrixWithoutScale(300f.toFloat() / viewHeight, transformMatrix)
        Log.d(TAG, "transformMatrix: ${mapMatrix.getModelMatrix().formatMatrixString()}")
        Log.d(TAG, "translateX: ${mapMatrix.getModelMatrix()[3]}, translateY: ${mapMatrix.getModelMatrix()[7]}, matrix: ${mapMatrix.getModelMatrix().contentToString()}")
        Matrix.setIdentityM(mMVPMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getViewMatrix(), 0, transformMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getProjectionMatrix(), 0, mMVPMatrix, 0);
        if (transformMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0)
        }
        GLES20.glActiveTexture(glArrowTextureId)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glArrowTextureId)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}