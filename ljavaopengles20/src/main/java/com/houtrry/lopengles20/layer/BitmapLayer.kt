package com.houtrry.lopengles20.layer

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20
import android.util.Log
import com.houtrry.common_map.utils.glGetAttribLocation
import com.houtrry.lopengles20.utils.OpenglUtils
import com.houtrry.common_map.utils.glGetUniformLocation
import com.houtrry.common_map.utils.toBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class BitmapLayer(private val bitmap: Bitmap) : BaseLayer() {

    companion object {
        private const val TAG = "BitmapShaper"

        //每个顶点的坐标数
        private const val COORDS_PRE_VERTEX = 3

        //每个纹理顶点的坐标数
        private const val COORDS_PRE_TEXTURE_VERTEX = 2
    }

    private var glTextureId: Int = 0
    private var positionLocation: Int = -1
    private var textureCoordinateLocation: Int = -1
    private var centerColorLocation: Int = -1
    private var outerColorLocation: Int = -1
    private var originOuterColorLocation: Int = -1
    private var wallColorLocation: Int = -1
    private var transformMatrixLocation: Int = -1
    private var isMapUniformLocation: Int = -1

    //顶点坐标
    private var squareCoords = floatArrayOf(
        -1.0f, 1.0f, 0.0f,//top left
        -1.0f, -1.0f, 0.0f,//bottom left
        1.0f, -1.0f, 0.0f,//bottom right
        1.0f, 1.0f, 0.0f,//top right
    )

    //顶点对应的纹理坐标
    private var texVertex = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 1f,
        1f, 0f
    )

    //四个顶点的绘制顺序数组
    private val drawOrder = shortArrayOf(
        0, 1, 2,
        0, 2, 3
    )

    //四个顶点的缓冲数组
    private val vertexBuffer: FloatBuffer = squareCoords.toBuffer()

    private val texVertexBuffer: FloatBuffer = texVertex.toBuffer()

    //四个顶点的绘制顺序数组的缓冲数组
    private val drawListBuffer: ShortBuffer = drawOrder.toBuffer()

    private val vertexStride: Int = COORDS_PRE_VERTEX * 4
    private val textVertexStride: Int = COORDS_PRE_TEXTURE_VERTEX * 4

    init {
        Log.d(TAG, "init start")
    }

    override fun onCreate() {
        //编译顶点着色器
        glTextureId = OpenglUtils.createTexture(
            bitmap,
            GLES20.GL_NEAREST, GLES20.GL_LINEAR,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )
        Log.d(TAG, "glTextureId: $glTextureId")

        // 缓存 attribute/uniform 位置
        positionLocation = program.glGetAttribLocation("vPosition")
        textureCoordinateLocation = program.glGetAttribLocation("inputTextureCoordinate")
        centerColorLocation = program.glGetUniformLocation("center_color")
        outerColorLocation = program.glGetUniformLocation("outer_color")
        originOuterColorLocation = program.glGetUniformLocation("origin_outer_color")
        wallColorLocation = program.glGetUniformLocation("wall_color")
        transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")
        // Plain Program 不需要 isMap
    }

    private fun String.colorToFloatArray(): FloatArray {
        val color = Color.parseColor(this)
        return floatArrayOf(
            Color.red(color) / 255f,
            Color.green(color) / 255f,
            Color.blue(color) / 255f,
            Color.alpha(color) / 255f
        )
    }

    private val centerColor: FloatArray by lazy {
        "#c3d8ea".colorToFloatArray()
    }
    private val outerColor: FloatArray by lazy {
        "#ffffff".colorToFloatArray()
    }
    private val originOuterColor: FloatArray by lazy {
        "#808080".colorToFloatArray()
    }
    private val wallColor by lazy {
        "#0072ff".colorToFloatArray()
    }

    private fun setColorValue(location: Int, color: FloatArray) {
        if (location >= 0) {
            GLES20.glUniform4fv(location, 1, color, 0)
        }
    }

    override fun onDraw() {
        // 设置矩阵：全屏 NDC 顶点，直接使用单位矩阵
        if (transformMatrixLocation >= 0) {
            val identity = floatArrayOf(
                1f,0f,0f,0f,
                0f,1f,0f,0f,
                0f,0f,1f,0f,
                0f,0f,0f,1f
            )
            GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, identity, 0)
        }
        if (isMapUniformLocation >= 0) {
            GLES20.glUniform1i(isMapUniformLocation, 0)
        }

        // 颜色 uniform
        setColorValue(centerColorLocation, centerColor)
        setColorValue(outerColorLocation, outerColor)
        setColorValue(originOuterColorLocation, originOuterColor)
        setColorValue(wallColorLocation, wallColor)

        // 顶点/纹理坐标 attribute
        if (positionLocation >= 0) {
            GLES20.glEnableVertexAttribArray(positionLocation)
            GLES20.glVertexAttribPointer(
                positionLocation, COORDS_PRE_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, vertexBuffer
            )
        }
        if (textureCoordinateLocation >= 0) {
            GLES20.glEnableVertexAttribArray(textureCoordinateLocation)
            GLES20.glVertexAttribPointer(
                textureCoordinateLocation, COORDS_PRE_TEXTURE_VERTEX, GLES20.GL_FLOAT,
                false, textVertexStride, texVertexBuffer
            )
        }

        // 纹理绑定
        GLES20.glActiveTexture(glTextureId)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTextureId)

        // 绘制
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )

        // 收尾：关闭 attribute，解绑纹理
        if (positionLocation >= 0) GLES20.glDisableVertexAttribArray(positionLocation)
        if (textureCoordinateLocation >= 0) GLES20.glDisableVertexAttribArray(textureCoordinateLocation)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun onDestroy() {
        if (glTextureId != 0) {
            val tmp = intArrayOf(glTextureId)
            GLES20.glDeleteTextures(1, tmp, 0)
            glTextureId = 0
        }
    }
}