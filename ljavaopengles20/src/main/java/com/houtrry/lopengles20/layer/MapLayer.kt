package com.houtrry.lopengles20.layer

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.data.BitmapSize
import com.houtrry.common_map.utils.*
import com.houtrry.lopengles20.utils.OpenglUtils
import java.nio.FloatBuffer
import java.nio.ShortBuffer


class MapLayer(private val mapBitmap: Bitmap) : BaseLayer() {

    companion object {
        private const val TAG = "MapLayer"

        //每个顶点的坐标数
        private const val COORDS_PRE_VERTEX = 3

        //每个纹理顶点的坐标数
        private const val COORDS_PRE_TEXTURE_VERTEX = 2
    }

    private var glMapTextureId: Int = 0

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
    private lateinit var mapBitmapSize: BitmapSize

    private val mMVPMatrix = FloatArray(16) // MVP 矩阵

    init {
        Log.d(TAG, "init start, ${mapBitmap.width}, ${mapBitmap.height}")
    }

    override fun onCreate() {
        mapBitmapSize = BitmapSize(mapBitmap.width, mapBitmap.height)
        glMapTextureId = OpenglUtils.createTexture(
            mapBitmap,
            GLES20.GL_NEAREST, GLES20.GL_NEAREST,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )
        Log.d(TAG, "glTextureId: $glMapTextureId")
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

    private var aspectRatio = 1f
    override fun onSizeChange(width: Int, height: Int) {
        super.onSizeChange(width, height)
        aspectRatio = width * 1f / height
        Log.d(TAG, "onSizeChange $viewWidth, $viewHeight, $width, $height, ${mapBitmapSize.width}, ${mapBitmapSize.height}")
    }

    private val centerColor: FloatArray by lazy {
        "#c3d8ea".colorToFloatArray()
    }
    private val outerColor: FloatArray by lazy {
        "#ffffff".colorToFloatArray()
    }
    private val wallColor by lazy {
        "#0072ff".colorToFloatArray()
    }

    private fun initColorValue(name: String, color: FloatArray): Int {
        val centerColorUniformLocation = program.glGetUniformLocation(name)
        GLES20.glUniform4fv(
            centerColorUniformLocation, 1,
            color,
            0
        )
        return centerColorUniformLocation
    }

    private var positionLocation: Int = -1
    private var centerColorLocation: Int = -1
    private var outerColorLocation: Int = -1
    private var wallColorLocation: Int = -1
    private var isMapUniformLocation: Int = -1
    private var transformMatrixLocation: Int = -1
    private var textureCoordinateLocation: Int = -1

    override fun doBeforeDraw() {
        super.doBeforeDraw()
        positionLocation = program.glGetAttribLocation("vPosition")
//        Log.d(TAG, "positionLocation: $positionLocation")
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation, COORDS_PRE_VERTEX, GLES20.GL_FLOAT,
            false, vertexStride, vertexBuffer
        )
        centerColorLocation = initColorValue("center_color", centerColor)
        outerColorLocation = initColorValue("outer_color", outerColor)
        wallColorLocation = initColorValue("wall_color", wallColor)
        isMapUniformLocation = program.glGetUniformLocation("isMap")
//        Log.d(TAG, "isMapUniformLocation: $isMapUniformLocation")
        transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")
        GLES20.glEnableVertexAttribArray(transformMatrixLocation)

        textureCoordinateLocation = program.glGetAttribLocation("inputTextureCoordinate")
        GLES20.glEnableVertexAttribArray(textureCoordinateLocation)
    }

    override fun onDraw() {
        GLES20.glUniform1i(
            isMapUniformLocation, 1
        )
//        mapMatrix.testAutoRotate()

        // Apply a ModelView Projection transformation
        Matrix.setIdentityM(mMVPMatrix, 0)
        // 计算缩放因子
        aspectRatio = mapBitmapSize.width * 1f / mapBitmapSize.height
        val scaleX = mapBitmapSize.width * 1f / viewHeight // 根据宽高比计算缩放
        val scaleY = mapBitmapSize.height * 1f / viewHeight

        val mvpMatrix = mapMatrix.getTransformMatrix().copyOf()
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f)

        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getViewMatrix(), 0, mvpMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mapMatrix.getProjectionMatrix(), 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0);
//        GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mapMatrix.getTransformMatrix(), 0);

        GLES20.glActiveTexture(glMapTextureId)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glMapTextureId)

        GLES20.glVertexAttribPointer(
            textureCoordinateLocation, COORDS_PRE_TEXTURE_VERTEX, GLES20.GL_FLOAT,
            false, textVertexStride, texVertexBuffer
        )
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA) // 设置混合函数

        GLES20.glUniform1i(
            isMapUniformLocation, 0
        )
    }

    override fun doAfterDraw() {
        super.doAfterDraw()
        positionLocation.glDisableVertexAttribArray()
        centerColorLocation.glDisableVertexAttribArray()
        outerColorLocation.glDisableVertexAttribArray()
        wallColorLocation.glDisableVertexAttribArray()
        isMapUniformLocation.glDisableVertexAttribArray()
        transformMatrixLocation.glDisableVertexAttribArray()
        textureCoordinateLocation.glDisableVertexAttribArray()
    }

}