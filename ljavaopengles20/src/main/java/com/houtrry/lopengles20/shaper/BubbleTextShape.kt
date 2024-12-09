package com.houtrry.lopengles20.shaper

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.data.BitmapSize
import com.houtrry.common_map.data.Size
import com.houtrry.common_map.data.Vector3
import com.houtrry.common_map.utils.formatMatrixString
import com.houtrry.common_map.utils.glGetUniformLocation
import com.houtrry.common_map.utils.toBuffer
import com.houtrry.lopengles20.data.*
import com.houtrry.lopengles20.utils.OpenglUtils
import java.nio.ShortBuffer

class BubbleTextShape {

    companion object {
        private const val TAG = "BubbleTextShape"
    }

    private var glArrowTextureId: Int = 0

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )
    private val mMVPMatrix = FloatArray(16) // MVP 矩阵
    private val transformMatrix: FloatArray = FloatArray(16)
    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()
    private lateinit var arrowBitmapSize: BitmapSize

    private val bubbleTextBitmapMap = mutableMapOf<BubbleText, Bitmap>()

    fun updateText(vector3: Vector3 ?= null, text: String? = null, color: Int? = null) {
//        bubbleText = bubbleText.update(vector3, text, color)
    }

    private var scaleFactor = 0.08f
    private var size: Size = Size(1024, 1024)

    fun setSize(width: Int, height: Int) {
        size = size.copy(width, height)
    }

    fun load(bubbleTextList: MutableList<BubbleText>) {
        bubbleTextBitmapMap.clear()
        bubbleTextList.forEach {
            val bitmap = it.generateBitmap()
            bubbleTextBitmapMap[it] = bitmap
        }
    }


    fun draw(program: Int, mapMatrix: MapMatrix) {
        bubbleTextBitmapMap.forEach { (bubbleText, bitmap) ->
            drawTextShape(program, mapMatrix, bubbleText.vector3, bitmap)
        }
    }

    private fun drawTextShape(program: Int, mapMatrix: MapMatrix,
                              vector3: Vector3, textBitmap: Bitmap) {
        val glArrowTextureId = OpenglUtils.createTexture(
            textBitmap,
            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )
        Matrix.setIdentityM(transformMatrix, 0)
        Matrix.translateM(transformMatrix, 0, vector3.x.toFloat(), vector3.y.toFloat(), vector3.z.toFloat())
//        mapMatrix.getTransformMatrixWithoutScale(scaleFactor, transformMatrix)
        Matrix.multiplyMM(transformMatrix, 0, mapMatrix.getTransformMatrix(), 0, transformMatrix, 0)
        transformMatrix[0] = 1f
        transformMatrix[1] = 0f
        transformMatrix[4] = 0f
        transformMatrix[5] = 1f
        Matrix.scaleM(transformMatrix, 0, textBitmap.width.toFloat() / size.height, textBitmap.height.toFloat() / size.height, 1f)
//        Matrix.translateM(transformMatrix, 0, 0f, 0f, 0f)
        Log.d(TAG, "text $vector3 scale: ${textBitmap.height.toFloat() / size.height}")
        Log.d(TAG, "transformMatrix: ${mapMatrix.getTransformMatrix().formatMatrixString()}")
        Log.d(TAG, "translateX: ${mapMatrix.getTransformMatrix()[3]}, translateY: ${mapMatrix.getTransformMatrix()[7]}, matrix: ${mapMatrix.getTransformMatrix().contentToString()}")
        Matrix.setIdentityM(mMVPMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getViewMatrix(), 0, transformMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getProjectionMatrix(), 0, mMVPMatrix, 0)
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