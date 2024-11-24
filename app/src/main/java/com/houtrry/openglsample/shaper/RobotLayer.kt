package com.houtrry.openglsample.shaper

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.openglsample.data.BitmapSize
import com.houtrry.openglsample.data.MapMatrix
import com.houtrry.openglsample.utils.OpenglUtils
import com.houtrry.openglsample.utils.glGetUniformLocation
import com.houtrry.openglsample.utils.toBuffer
import java.nio.ShortBuffer

class RobotLayer(
    private val arrowBitmap: Bitmap,
) : BaseLayer() {

    companion object {
        private const val TAG = "RobotLayer"
    }

    private var glArrowTextureId: Int = 0

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )

    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()

    private lateinit var arrowBitmapSize: BitmapSize

    init {
        Log.d(TAG, "init start")
    }

    override fun onCreate() {
        arrowBitmapSize = BitmapSize(arrowBitmap.width, arrowBitmap.height)
        glArrowTextureId = OpenglUtils.createTexture(
            arrowBitmap,
            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )

        Log.d(TAG, "glArrowTextureId: $glArrowTextureId")
    }

    override fun onDraw() {
        val transformMatrix = OpenglUtils.getTargetMatrix(
            mapMatrix.getTranslateX(),
            mapMatrix.getTranslateY(),
            240f.toFloat() / viewWidth,
            240f.toFloat() / viewHeight,
            0f
        )
        Log.d(TAG, "translateX: ${mapMatrix.getTransformMatrix()[3]}, translateY: ${mapMatrix.getTransformMatrix()[7]}, matrix: ${mapMatrix.getTransformMatrix().contentToString()}")
        val transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")
        GLES20.glActiveTexture(glArrowTextureId)
        GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, transformMatrix, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glArrowTextureId)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}