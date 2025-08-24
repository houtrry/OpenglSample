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
import com.houtrry.common_map.utils.glGetAttribLocation
import com.houtrry.common_map.utils.toBuffer
import com.houtrry.lopengles20.data.*
import com.houtrry.lopengles20.utils.MatrixUtils
import com.houtrry.lopengles20.utils.OpenglUtils
import com.houtrry.lopengles20.utils.identityM
import java.nio.ShortBuffer

class BubbleTextShape {

    companion object {
        private const val TAG = "BubbleTextShape"
    }

    private var glArrowTextureId: Int = 0
    // 顶点与纹理坐标（单位方块，中心在原点）
    private val vertexCoords = floatArrayOf(
        -0.5f, 0.5f, 0.0f,
        0.5f, 0.5f, 0.0f,
        0.5f, -0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f
    )
    private val texCoords = floatArrayOf(
        0f, 0f,
        1f, 0f,
        1f, 1f,
        0f, 1f
    )
    private val vertexBuffer = vertexCoords.toBuffer()
    private val texBuffer = texCoords.toBuffer()
    private val vertexStride = 3 * 4
    private val texStride = 2 * 4

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )
    private val mMVPMatrix = FloatArray(16) // MVP 矩阵
    private val transformMatrix: FloatArray = FloatArray(16)
    private val scaleMatrix: FloatArray = FloatArray(16)
    private val offsetMatrix: FloatArray = FloatArray(16).identityM().apply {
        // 添加向上偏移，让气泡底部对准目标位置
        // 在标准化纹理坐标空间中，向上偏移0.5个单位（即半个高度）
        Matrix.translateM(this, 0, 0f, 0.5f, 0f)
    }
    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()
    private lateinit var arrowBitmapSize: BitmapSize

    // 位置缓存（按 program）
    private var cachedProgram: Int = -1
    private var positionLocation: Int = -1
    private var texCoordLocation: Int = -1
    private var transformMatrixLocation: Int = -1
    private var isMapUniformLocation: Int = -1

    private val bubbleTextBitmapMap = mutableMapOf<BubbleText, Bitmap>()
    private val textureCache = mutableMapOf<String, Int>()  // 纹理缓存：文本内容 -> 纹理ID

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

    private fun ensureLocations(program: Int) {
        if (cachedProgram != program) {
            cachedProgram = program
            positionLocation = program.glGetAttribLocation("vPosition")
            texCoordLocation = program.glGetAttribLocation("inputTextureCoordinate")
            transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")
            isMapUniformLocation = program.glGetUniformLocation("isMap")
        }
    }

    private fun drawTextShape(program: Int, mapMatrix: MapMatrix,
                              position: Vector3, textBitmap: Bitmap) {
        ensureLocations(program)
        
        // 生成缓存key（基于位图内容哈希）
        val cacheKey = "${textBitmap.width}x${textBitmap.height}_${textBitmap.hashCode()}"
        
        // 尝试从缓存获取纹理ID
        val glArrowTextureId = textureCache[cacheKey] ?: run {
            // 智能选择纹理过滤方式：小图标用NEAREST保持锐利，大图标用LINEAR平滑
            val minFilter = if (textBitmap.width <= 64 && textBitmap.height <= 64) {
                GLES20.GL_NEAREST  // 小图标保持像素级清晰
            } else {
                GLES20.GL_LINEAR   // 大图标使用平滑过滤
            }
            val magFilter = GLES20.GL_LINEAR  // 放大时始终使用LINEAR避免锯齿
            
            val textureId = OpenglUtils.createTexture(
                textBitmap,
                minFilter, magFilter,
                GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
            )
            
            // 缓存纹理ID
            textureCache[cacheKey] = textureId
            textureId
        }
        val vector3 = mapMatrix.worldToGl(position.x.toFloat(), position.y.toFloat())
        Log.d(TAG, "drawTextShape, $textBitmap, vector3: $vector3 -> $position")
        transformMatrix.identityM()
        Matrix.translateM(transformMatrix, 0, vector3.x, vector3.y, 0f)
        scaleMatrix.identityM()
        Matrix.scaleM(scaleMatrix, 0, textBitmap.width.toFloat(), textBitmap.height.toFloat(), 1f)

        mMVPMatrix.identityM()

        //注意，获取分量这个操作，应该是modelMatrix与transformMatrix计算后的结果获取分量
        //而不是先获取modelMatrix的分量，再与transformMatrix计算
        //不然结果就是错的
        Matrix.multiplyMM(transformMatrix, 0, mapMatrix.getModelMatrix(), 0, transformMatrix, 0)
        MatrixUtils.multiplyMatrices(mMVPMatrix,
            mapMatrix.getProjectionViewMatrix(),
            MatrixUtils.getComponentOfMatrix(transformMatrix, FloatArray(16).identityM(), true, false, false),
            scaleMatrix,
            offsetMatrix
        )

        // 设置 uniform
        if (transformMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0)
        }
        if (isMapUniformLocation >= 0) {
            GLES20.glUniform1i(isMapUniformLocation, 0)
        }

        // 启用 attribute 并指向数据
        if (positionLocation >= 0) {
            GLES20.glEnableVertexAttribArray(positionLocation)
            GLES20.glVertexAttribPointer(positionLocation, 3, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)
        }
        if (texCoordLocation >= 0) {
            GLES20.glEnableVertexAttribArray(texCoordLocation)
            GLES20.glVertexAttribPointer(texCoordLocation, 2, GLES20.GL_FLOAT, false, texStride, texBuffer)
        }

        GLES20.glActiveTexture(glArrowTextureId)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glArrowTextureId)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        // 收尾：关闭 attribute
        if (positionLocation >= 0) GLES20.glDisableVertexAttribArray(positionLocation)
        if (texCoordLocation >= 0) GLES20.glDisableVertexAttribArray(texCoordLocation)
    }

    fun release() {
        // 释放纹理缓存
        textureCache.values.forEach { textureId ->
            val textures = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, textures, 0)
        }
        textureCache.clear()
        
        // 释放生成的文本位图
        bubbleTextBitmapMap.values.forEach { bmp ->
            if (!bmp.isRecycled) bmp.recycle()
        }
        bubbleTextBitmapMap.clear()
    }
}