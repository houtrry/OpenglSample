package com.houtrry.lopengles20.tile

import android.opengl.GLES20
import com.houtrry.lopengles20.data.MapMatrix
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Minimal TileManager with visible-tile computation and tiny LRU+TexturePool.
 * This is a scaffold; loading/updates/LOD generation will be added later.
 */
class TileManager(
    private val tileSizePx: Int = 512,
    lruCapacity: Int = 256,
    texturePoolCapacity: Int = 256
) {

    private var mapWidthPx: Int = 0
    private var mapHeightPx: Int = 0
    private var viewportWidthPx: Int = 0
    private var viewportHeightPx: Int = 0

    // LRU 缓存：管理 TileCoord → Tile 的映射（占位或已就绪）
    private val cache = LruCache<TileCoord, Tile>(lruCapacity)
    // 纹理池：预分配/复用 GL 纹理，减少 glGenTextures 频率
    private val texturePool = TexturePool(texturePoolCapacity)
    private val pendingLoads = LinkedHashSet<TileCoord>()
    private var regionProvider: RegionProvider? = null

    fun setRegionProvider(provider: RegionProvider?) {
        regionProvider = provider
    }

    fun setMapSize(widthPx: Int, heightPx: Int) {
        mapWidthPx = widthPx
        mapHeightPx = heightPx
    }

    fun setViewportSize(widthPx: Int, heightPx: Int) {
        viewportWidthPx = widthPx
        viewportHeightPx = heightPx
    }

    /**
     * LOD 层级选择（占位框架）：
     * - 思路1：根据屏幕像素密度与当前 model 缩放，计算地图像素到屏幕像素的映射比例；
     *   比例接近 1:1 的层级为最佳；
     * - 思路2：将投影到屏幕的 1px 反推到 GL/Map 空间的长度，据此估算层级；
     * - 这里暂时固定为 0，后续接入时：return estimateLevel(mapMatrix, viewportWidthPx, viewportHeightPx)
     */
    private fun selectLodLevel(mapMatrix: MapMatrix): Int {
        return 0
    }

    /**
     * 计算可见瓦片（当前固定 level=0）。
     * 步骤：屏幕四角 → GL 坐标 → Map 像素坐标 → AABB 裁剪 → 像素转瓦片索引范围。
     */
    fun queryVisibleTiles(mapMatrix: MapMatrix): List<Tile> {
        if (mapWidthPx <= 0 || mapHeightPx <= 0 || viewportWidthPx <= 0 || viewportHeightPx <= 0) return emptyList()

        // 1) 计算屏幕四角在 GL 空间坐标
        val pLT = mapMatrix.convertScreenToGL(0f, 0f, viewportWidthPx, viewportHeightPx)
        val pRT = mapMatrix.convertScreenToGL(viewportWidthPx.toFloat(), 0f, viewportWidthPx, viewportHeightPx)
        val pLB = mapMatrix.convertScreenToGL(0f, viewportHeightPx.toFloat(), viewportWidthPx, viewportHeightPx)
        val pRB = mapMatrix.convertScreenToGL(viewportWidthPx.toFloat(), viewportHeightPx.toFloat(), viewportWidthPx, viewportHeightPx)

        // 2) GL → Map 像素坐标（用 MapMatrix.glToWorld + world→map 像素换算）
        val lt = mapMatrix.glToMapPx(pLT.x, pLT.y)
        val rt = mapMatrix.glToMapPx(pRT.x, pRT.y)
        val lb = mapMatrix.glToMapPx(pLB.x, pLB.y)
        val rb = mapMatrix.glToMapPx(pRB.x, pRB.y)

        val minX = floor(min(min(lt.x, rt.x), min(lb.x, rb.x)).toDouble()).toInt()
        val maxX = ceil(max(max(lt.x, rt.x), max(lb.x, rb.x)).toDouble()).toInt()
        val minY = floor(min(min(lt.y, rt.y), min(lb.y, rb.y)).toDouble()).toInt()
        val maxY = ceil(max(max(lt.y, rt.y), max(lb.y, rb.y)).toDouble()).toInt()

        // 3) 裁剪到地图范围
        val clippedMinX = max(0, minX)
        val clippedMaxX = min(mapWidthPx, maxX)
        val clippedMinY = max(0, minY)
        val clippedMaxY = min(mapHeightPx, maxY)
        if (clippedMinX >= clippedMaxX || clippedMinY >= clippedMaxY) return emptyList()

        // 4) 选择 LOD 层级（占位），并将像素 → 瓦片索引范围（当前仍按 base 层计算）
        val level = selectLodLevel(mapMatrix)
        // TODO: 当启用多层级后，需要将 clippedMin/Max 按 level 的像素分辨率换算后再求瓦片网格范围
        val tileMinX = floor(clippedMinX / tileSizePx.toDouble()).toInt()
        val tileMaxX = floor((clippedMaxX - 1) / tileSizePx.toDouble()).toInt()
        val tileMinY = floor(clippedMinY / tileSizePx.toDouble()).toInt()
        val tileMaxY = floor((clippedMaxY - 1) / tileSizePx.toDouble()).toInt()

        val result = ArrayList<Tile>()
        for (ty in tileMinY..tileMaxY) {
            for (tx in tileMinX..tileMaxX) {
                val coord = TileCoord(level = level, x = tx, y = ty)
                val cached = cache.get(coord)
                if (cached != null) {
                    result.add(cached)
                } else {
                    // 占位 Tile：未就绪时返回 isReady=false，纹理 id=0；供渲染层跳过绘制
                    val originX = tx * tileSizePx
                    val originY = ty * tileSizePx
                    val width = min(tileSizePx, mapWidthPx - originX)
                    val height = min(tileSizePx, mapHeightPx - originY)
                    val placeholder = Tile(
                        coord = coord,
                        textureId = 0,
                        widthPx = width,
                        heightPx = height,
                        originXInMapPx = originX,
                        originYInMapPx = originY,
                        isReady = false
                    )
                    cache.put(coord, placeholder)
                    result.add(placeholder)
                    pendingLoads.add(coord)
                }
            }
        }
        return result
    }

    /**
     * 在 GL 线程按帧限速加载挂起瓦片（解码/拷贝数据 → glTex(Sub)Image2D 上传 → 更新缓存）。
     * 返回本帧加载的瓦片个数。
     */
    fun loadPendingOnGlThread(maxCount: Int = 2): Int {
        if (pendingLoads.isEmpty()) return 0
        var loaded = 0
        val iterator = pendingLoads.iterator()
        while (iterator.hasNext() && loaded < maxCount) {
            val coord = iterator.next()
            iterator.remove()
            val placeholder = cache.get(coord) ?: continue
            val region = regionProvider?.obtainRegion(
                placeholder.originXInMapPx,
                placeholder.originYInMapPx,
                placeholder.widthPx,
                placeholder.heightPx
            ) ?: continue

            val texId = texturePool.acquire()
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            // allocate texture storage
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                region.glFormat,
                region.width,
                region.height,
                0,
                region.glFormat,
                GLES20.GL_UNSIGNED_BYTE,
                null
            )
            // upload subimage
            GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                0,
                0,
                region.width,
                region.height,
                region.glFormat,
                GLES20.GL_UNSIGNED_BYTE,
                region.pixelBuffer
            )
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            val ready = Tile(
                coord = coord,
                textureId = texId,
                widthPx = placeholder.widthPx,
                heightPx = placeholder.heightPx,
                originXInMapPx = placeholder.originXInMapPx,
                originYInMapPx = placeholder.originYInMapPx,
                isReady = true
            )
            cache.put(coord, ready)
            loaded++
        }
        return loaded
    }

    // precise mapping now provided by MapMatrix.glToMapPx
}


