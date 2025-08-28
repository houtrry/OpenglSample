package com.houtrry.lopengles20.layer

import android.graphics.Color
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.houtrry.common_map.data.BitmapSize
import com.houtrry.common_map.utils.*
import com.houtrry.lopengles20.data.BitmapInfo
import com.houtrry.lopengles20.utils.OpenglUtils
import com.houtrry.lopengles20.utils.identityM
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import com.houtrry.lopengles20.tile.TileManager
import com.houtrry.lopengles20.tile.GrayscaleRegionProviderFromBitmap
import com.houtrry.lopengles20.tile.JpegPngRegionProvider
import android.graphics.BitmapRegionDecoder


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
        -.5f, .5f, 0.0f,//top left
        .5f, .5f, 0.0f,//top right
        .5f, -.5f, 0.0f,//bottom right
        -.5f, -.5f, 0.0f,//bottom left
    )

    //顶点对应的纹理坐标
    private var texVertex = floatArrayOf(
        0f, 0f,
        1f, 0f,
        1f, 1f,
        0f, 1f
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

    private val textureSizeMatrix = FloatArray(16).identityM()

    init {
        Log.d(TAG, "init start, ${mapBitmap.width}, ${mapBitmap.height}")
    }

    private var autoOverviewListener: AutoOverviewListener? = null
    fun setAutoOverviewListener(listener: AutoOverviewListener?) {
        autoOverviewListener = listener
    }

    fun notifyZoomBegin(currentRobotPoseXMeters: Float, currentRobotPoseYMeters: Float) {
        notifyZoomGestureBegin(currentRobotPoseXMeters, currentRobotPoseYMeters)
    }

    fun notifyZoomEnd(currentRobotPoseXMeters: Float, currentRobotPoseYMeters: Float) {
        notifyZoomGestureEnd(currentRobotPoseXMeters, currentRobotPoseYMeters)
    }

    fun updateRobotPose(currentRobotPoseXMeters: Float, currentRobotPoseYMeters: Float) {
        updateRobotPoseAndCheckAutoOverview(currentRobotPoseXMeters, currentRobotPoseYMeters) {
            autoOverviewListener?.onRequestAutoOverview()
        }
    }

    // Tile 管理器（最小骨架）：
    // 负责：可见瓦片计算、LRU/纹理池、按帧限速加载；
    // 策略：先绘制“整图单纹理”作为基底，再叠加“已就绪瓦片”，实现平滑过渡。
    private val tileManager = TileManager(512)

    override fun onCreate() {
        mapBitmapSize = BitmapSize(mapBitmap.width, mapBitmap.height)
        mapMatrix.updateBitmapInfo(BitmapInfo(0f, 0f, 0.05f, mapBitmap.width, mapBitmap.height))
        glMapTextureId = OpenglUtils.createTexture(
            mapBitmap,
            GLES20.GL_NEAREST, GLES20.GL_NEAREST,
            GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE
        )
        Log.d(TAG, "glTextureId: $glMapTextureId")
        Matrix.scaleM(textureSizeMatrix, 0, mapBitmap.width.toFloat(), mapBitmap.height.toFloat(), 1f)

        // 初始化 TileManager：地图尺寸 + 区域数据提供者（基于 ByteBuffer）
        tileManager.setMapSize(mapBitmap.width, mapBitmap.height)
        // Default demo: grayscale provider (GL_LUMINANCE) to reduce bandwidth
        tileManager.setRegionProvider(GrayscaleRegionProviderFromBitmap(mapBitmap))
    }

    /**
     * 运行时切换为 JPEG/PNG Provider（区域解码）。
     * - decoderFactory 由调用方提供，适配 PFD/byte[]/文件路径等来源；
     * - useCpuGray: true 则 CPU 转灰度走 LUMINANCE 上传；false 则直接 RGBA 上传。
     */
    fun useJpegPngRegionProvider(
        decoderFactory: () -> BitmapRegionDecoder,
        useCpuGray: Boolean = false
    ) {
        tileManager.setRegionProvider(JpegPngRegionProvider(decoderFactory, useCpuGray))
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
        tileManager.setViewportSize(width, height)
    }

    private val centerColor: FloatArray by lazy {
        "#c3d8ea".colorToFloatArray()
    }
    private val outerColor: FloatArray by lazy {
        "#d6dadf".colorToFloatArray()
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
    // 地图 Program 不再需要 isMap
    private var transformMatrixLocation: Int = -1
    private var textureCoordinateLocation: Int = -1

    override fun onDraw() {
        // 0) 每帧前置：确保 attribute 指针/enable，设置必要的 uniform
        if (positionLocation == -1) {
            positionLocation = program.glGetAttribLocation("vPosition")
        }
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation, COORDS_PRE_VERTEX, GLES20.GL_FLOAT,
            false, vertexStride, vertexBuffer
        )

        if (centerColorLocation == -1) centerColorLocation = program.glGetUniformLocation("center_color")
        if (outerColorLocation == -1) outerColorLocation = program.glGetUniformLocation("outer_color")
        if (wallColorLocation == -1) wallColorLocation = program.glGetUniformLocation("wall_color")
        GLES20.glUniform4fv(centerColorLocation, 1, centerColor, 0)
        GLES20.glUniform4fv(outerColorLocation, 1, outerColor, 0)
        GLES20.glUniform4fv(wallColorLocation, 1, wallColor, 0)

        if (transformMatrixLocation == -1) transformMatrixLocation = program.glGetUniformLocation("u_TransformMatrix")

        if (textureCoordinateLocation == -1) {
            textureCoordinateLocation = program.glGetAttribLocation("inputTextureCoordinate")
        }
        GLES20.glEnableVertexAttribArray(textureCoordinateLocation)
        // 地图 Program 不再需要 isMap uniform

        // 2) 基础 PV*Model（投影*视图*模型）
        val pvModel = FloatArray(16).identityM()
        Matrix.multiplyMM(pvModel, 0, mapMatrix.getProjectionViewMatrix(), 0, mapMatrix.getModelMatrix(), 0)

        // 3) 先绘制整图（基底），避免瓦片未就绪时出现空白
        Matrix.multiplyMM(mMVPMatrix, 0, pvModel, 0, textureSizeMatrix, 0)
        GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, mMVPMatrix, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glMapTextureId)
        GLES20.glVertexAttribPointer(textureCoordinateLocation, COORDS_PRE_TEXTURE_VERTEX, GLES20.GL_FLOAT, false, textVertexStride, texVertexBuffer)
        // 使用GL_TRIANGLES与索引匹配
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA) // 设置混合函数

        // 4) 计算可见瓦片 → 限速加载 → 叠加绘制“已就绪瓦片”
        //    LOD 选择说明：当前 TileManager 内部固定选择 level=0；
        //    后续可根据缩放比/屏幕像素密度选择最合适的层级，并将瓦片网格/尺寸与着色采样一并切换。
        val visibleTiles = kotlin.runCatching { tileManager.queryVisibleTiles(mapMatrix) }.getOrDefault(emptyList())
        tileManager.loadPendingOnGlThread(maxCount = 2)

        val tileSizeM = FloatArray(16)
        val tileTranslateM = FloatArray(16)
        val tileMvp = FloatArray(16)
        for (tile in visibleTiles) {
            if (!tile.isReady || tile.textureId == 0) continue
            // 4.1) tile 尺寸矩阵：单位方块放大为瓦片像素尺寸
            tileSizeM.identityM()
            Matrix.scaleM(tileSizeM, 0, tile.widthPx.toFloat(), tile.heightPx.toFloat(), 1f)
            // 4.2) tile 平移矩阵：以地图中心为原点，将瓦片中心平移到其在地图中的位置
            val centerX = tile.originXInMapPx + tile.widthPx * 0.5f - mapBitmapSize.width * 0.5f
            // tile.originYInMapPx 现为左上(y向下)，需转为以地图中心为原点、y向上
            val centerY = (mapBitmapSize.height - (tile.originYInMapPx + tile.heightPx * 0.5f)) - mapBitmapSize.height * 0.5f
            tileTranslateM.identityM()
            Matrix.translateM(tileTranslateM, 0, centerX, centerY, 0f)

            // 4.3) 最终 MVP：pvModel * tileTranslate * tileSize
            Matrix.multiplyMM(tileMvp, 0, pvModel, 0, tileTranslateM, 0)
            Matrix.multiplyMM(tileMvp, 0, tileMvp, 0, tileSizeM, 0)
            GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, tileMvp, 0)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.textureId)
            // 只采样内容区域的 UV：使用 (u0,v0)-(u1,v1)
            val uv = tileManager.getTileContentUv(tile)
            // 使用未翻转的UV，避免上下反转
            val uvBuf = floatArrayOf(
                uv.u0, uv.v0,
                uv.u1, uv.v0,
                uv.u1, uv.v1,
                uv.u0, uv.v1
            ).toBuffer()
            GLES20.glVertexAttribPointer(textureCoordinateLocation, COORDS_PRE_TEXTURE_VERTEX, GLES20.GL_FLOAT, false, textVertexStride, uvBuf)
            // 使用GL_TRIANGLES与索引匹配
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        // 地图 Program 不再需要 isMap uniform

        // 收尾：仅关闭 attribute 数组
        positionLocation.glDisableVertexAttribArray()
        textureCoordinateLocation.glDisableVertexAttribArray()
    }

    override fun onDestroy() {
        if (glMapTextureId != 0) {
            val tmp = intArrayOf(glMapTextureId)
            GLES20.glDeleteTextures(1, tmp, 0)
            glMapTextureId = 0
        }
        // 销毁 TileManager 资源
        tileManager.destroy()
    }

    /**
     * 示例：将一个以 (0,0) 为锚点、可向任意方向扩展的动态区域 AABB 增量写入到瓦片纹理中，
     * 并在绘制阶段渲染缓存中“任意网格”的可见瓦片（支持负索引）。
     * 注意：该方法仅为示例，可在手势/数据更新时调用。
     */
    fun demoApplyDynamicRegionAndDraw(minX: Int, minY: Int, maxX: Int, maxY: Int) {
        // 1) 在 GL 线程应用增量：将 AABB 拆分为若干 Tile 的局部上传
        tileManager.applyDynamicAabbOnGlThread(minX, minY, maxX, maxY)
        // 2) 查询当前视口中缓存已就绪的任何网格瓦片（含负索引）
        val tiles = tileManager.queryVisibleCachedTilesAnyGrid(mapMatrix)
        val pvModel = FloatArray(16).identityM()
        Matrix.multiplyMM(pvModel, 0, mapMatrix.getProjectionViewMatrix(), 0, mapMatrix.getModelMatrix(), 0)
        val tileSizeM = FloatArray(16)
        val tileTranslateM = FloatArray(16)
        val tileMvp = FloatArray(16)
        for (tile in tiles) {
            if (tile.textureId == 0) continue
            tileSizeM.identityM()
            Matrix.scaleM(tileSizeM, 0, tile.widthPx.toFloat(), tile.heightPx.toFloat(), 1f)
            val centerX = tile.originXInMapPx + tile.widthPx * 0.5f - mapBitmapSize.width * 0.5f
            val centerY = tile.originYInMapPx + tile.heightPx * 0.5f - mapBitmapSize.height * 0.5f
            tileTranslateM.identityM()
            Matrix.translateM(tileTranslateM, 0, centerX, centerY, 0f)
            Matrix.multiplyMM(tileMvp, 0, pvModel, 0, tileTranslateM, 0)
            Matrix.multiplyMM(tileMvp, 0, tileMvp, 0, tileSizeM, 0)
            GLES20.glUniformMatrix4fv(transformMatrixLocation, 1, false, tileMvp, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.textureId)
            GLES20.glVertexAttribPointer(textureCoordinateLocation, COORDS_PRE_TEXTURE_VERTEX, GLES20.GL_FLOAT, false, textVertexStride, texVertexBuffer)
            GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    

}