package com.houtrry.openglsample.shaper

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.openglsample.data.BitmapSize
import com.houtrry.openglsample.data.MapMatrix
import com.houtrry.openglsample.utils.OpenglUtils
import com.houtrry.openglsample.utils.formatMatrixString
import com.houtrry.openglsample.utils.glGetUniformLocation
import com.houtrry.openglsample.utils.toBuffer
import java.nio.ShortBuffer

class PointLayer: BaseLayer() {

    companion object {
        private const val TAG = "PointLayer"
    }

    private var glArrowTextureId: Int = 0

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
//        arrowBitmapSize = BitmapSize(arrowBitmap.width, arrowBitmap.height)
//        glArrowTextureId = OpenglUtils.createTexture(
//            arrowBitmap,
//            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
//            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
//        )
//
//        Log.d(TAG, "glArrowTextureId: $glArrowTextureId, ${arrowBitmapSize.width} * ${arrowBitmapSize.height}")
    }
    private val mMVPMatrix = FloatArray(16) // MVP 矩阵

    override fun onDraw() {
//        val transformMatrix = OpenglUtils.getTargetMatrix(
//            mapMatrix.getTranslateX(),
//            mapMatrix.getTranslateY(),
//            240f.toFloat() / viewHeight,
//            240f.toFloat() / viewHeight,
//            0f
//        )
        mapMatrix.getTransformMatrixWithoutRotate(300f.toFloat() / viewHeight, transformMatrix)
        Log.d(TAG, "transformMatrix: ${mapMatrix.getTransformMatrix().formatMatrixString()}")
        Log.d(TAG, "translateX: ${mapMatrix.getTransformMatrix()[3]}, translateY: ${mapMatrix.getTransformMatrix()[7]}, matrix: ${mapMatrix.getTransformMatrix().contentToString()}")
        Matrix.setIdentityM(mMVPMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getViewMatrix(), 0, transformMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getProjectionMatrix(), 0, mMVPMatrix, 0);
        val transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")
        GLES20.glActiveTexture(glArrowTextureId)
        GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glArrowTextureId)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}