package com.houtrry.lopengles20.tile

import android.opengl.GLES20
import com.houtrry.lopengles20.utils.PerfMetrics
import com.houtrry.lopengles20.data.MapMatrix
import java.nio.ByteBuffer
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

    // 可选的 1px 扩展边，用于抑制采样缝隙（默认关闭）
    private var enableBorder: Boolean = false
    private var borderSizePx: Int = 1

    fun setRegionProvider(provider: RegionProvider?) {
        regionProvider = provider
    }

    /**
     * 启用/关闭 1px 扩展边（用于抑制接缝）。默认关闭；启用时默认 border=1。
     */
    fun setBorderEnabled(enable: Boolean, borderSizePx: Int = 1) {
        this.enableBorder = enable
        this.borderSizePx = borderSizePx.coerceAtLeast(0)
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
        if (PerfMetrics.enabled) {
            PerfMetrics.setVisibleTiles(result.size)
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
            val border = if (enableBorder) borderSizePx else 0
            val texWidth = region.width + border * 2
            val texHeight = region.height + border * 2
            // 分配包含边框的纹理存储
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                region.glFormat,
                texWidth,
                texHeight,
                0,
                region.glFormat,
                GLES20.GL_UNSIGNED_BYTE,
                null
            )
            // 在 (border, border) 偏移处上传内容
            GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                border,
                border,
                region.width,
                region.height,
                region.glFormat,
                GLES20.GL_UNSIGNED_BYTE,
                region.pixelBuffer
            )
            if (enableBorder && border > 0) {
                uploadFullBorders(region, border)
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            if (PerfMetrics.enabled) {
                val bpp = when (region.glFormat) {
                    GLES20.GL_LUMINANCE, GLES20.GL_ALPHA, 0x8229 /*GL_RED*/ -> 1
                    else -> 4
                }
                val bytes = region.width.toLong() * region.height.toLong() * bpp
                PerfMetrics.addUploadBytes(bytes)
                PerfMetrics.addUpdatedTiles(1)
            }

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

    /**
     * 示例：将“任意方向扩展的动态有效区域 AABB（像素坐标）”分解为若干 Tile 的局部上传请求。
     * 注意：此方法仅构建请求，不实际上传。可用于离线/在线增量补丁场景。
     */
    fun buildSubUploadRequestsForDynamicAabb(
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int
    ): List<TileGridUtils.TileSubRequest> {
        val aabb = TileGridUtils.PixelAabb(minX, minY, maxX, maxY)
        return TileGridUtils.buildSubRequestsForAabb(aabb, tileSizePx)
    }

	/**
	 * 在 GL 线程直接应用动态 AABB 的增量数据：
	 * - 为涉及到的每个 Tile 分配/复用完整的 tileSizePx×tileSizePx 纹理；
	 * - 对交叠的子矩形执行 glTexSubImage2D 局部上传；
	 * - 在缓存中创建/更新 Tile，允许负索引；
	 * 返回被更新（发生上传）的 Tile 数量。
	 */
	fun applyDynamicAabbOnGlThread(
		minX: Int,
		minY: Int,
		maxX: Int,
		maxY: Int
	): Int {
		val provider = regionProvider ?: return 0
		val requests = buildSubUploadRequestsForDynamicAabb(minX, minY, maxX, maxY)
		if (requests.isEmpty()) return 0
		var updatedTiles = 0
		// 将请求按 tile 分组，便于一次性完成同一 tile 的多段上传
		val grouped = requests.groupBy { it.tileIndex }
		for ((tileIndex, subReqs) in grouped) {
			val coord = TileCoord(level = 0, x = tileIndex.x, y = tileIndex.y)
			var tile = cache.get(coord)
			var texId = tile?.textureId ?: 0
			if (texId == 0) {
				texId = texturePool.acquire()
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
				// 以第一个子请求的格式作为 tile 的格式（假设同一 provider 的格式一致）
				val firstReq = subReqs.first()
				val firstRegion = provider.obtainRegion(firstReq.subMinX, firstReq.subMinY, firstReq.subWidth, firstReq.subHeight)
				if (firstRegion == null) {
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
					continue
				}
				val border = if (enableBorder) borderSizePx else 0
				GLES20.glTexImage2D(
					GLES20.GL_TEXTURE_2D,
					0,
					firstRegion.glFormat,
					tileSizePx + border * 2,
					tileSizePx + border * 2,
					0,
					firstRegion.glFormat,
					GLES20.GL_UNSIGNED_BYTE,
					null
				)
				// 回填/创建 tile
				val originX = tileIndex.x * tileSizePx
				val originY = tileIndex.y * tileSizePx
				tile = Tile(
					coord = coord,
					textureId = texId,
					widthPx = tileSizePx,
					heightPx = tileSizePx,
					originXInMapPx = originX,
					originYInMapPx = originY,
					isReady = true
				)
				cache.put(coord, tile)
			} else {
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
			}
			// 逐段上传
			for (req in subReqs) {
				val region = provider.obtainRegion(req.subMinX, req.subMinY, req.subWidth, req.subHeight) ?: continue
				val border = if (enableBorder) borderSizePx else 0
				GLES20.glTexSubImage2D(
					GLES20.GL_TEXTURE_2D,
					0,
					req.xOffsetInTile + border,
					req.yOffsetInTile + border,
					req.subWidth,
					req.subHeight,
					region.glFormat,
					GLES20.GL_UNSIGNED_BYTE,
					region.pixelBuffer
				)
				if (enableBorder && border > 0) {
					uploadPartialBorders(region, req, border)
				}
			}
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
			updatedTiles++
		}
		return updatedTiles
	}

	/** 计算只采样“内容区域”的 UV（考虑可选边框）。 */
	fun getTileContentUv(tile: Tile): TileGridUtils.UvRect {
		val border = if (enableBorder) borderSizePx else 0
		val texW = tile.widthPx + border * 2
		val texH = tile.heightPx + border * 2
		if (texW <= 0 || texH <= 0) return TileGridUtils.UvRect(0f, 0f, 1f, 1f)
		val u0 = border.toFloat() / texW.toFloat()
		val v0 = border.toFloat() / texH.toFloat()
		val u1 = (border + tile.widthPx).toFloat() / texW.toFloat()
		val v1 = (border + tile.heightPx).toFloat() / texH.toFloat()
		return TileGridUtils.UvRect(u0, v0, u1, v1)
	}

	/**
	 * 计算当前视口可见范围覆盖的“已缓存 Tile（允许负索引）”。
	 * 与 queryVisibleTiles 不同点：不对像素范围裁剪到 map 尺寸，
	 * 仅在缓存中存在且已就绪时返回。
	 */
	fun queryVisibleCachedTilesAnyGrid(mapMatrix: MapMatrix): List<Tile> {
		if (viewportWidthPx <= 0 || viewportHeightPx <= 0) return emptyList()
		val pLT = mapMatrix.convertScreenToGL(0f, 0f, viewportWidthPx, viewportHeightPx)
		val pRT = mapMatrix.convertScreenToGL(viewportWidthPx.toFloat(), 0f, viewportWidthPx, viewportHeightPx)
		val pLB = mapMatrix.convertScreenToGL(0f, viewportHeightPx.toFloat(), viewportWidthPx, viewportHeightPx)
		val pRB = mapMatrix.convertScreenToGL(viewportWidthPx.toFloat(), viewportHeightPx.toFloat(), viewportWidthPx, viewportHeightPx)
		val lt = mapMatrix.glToMapPx(pLT.x, pLT.y)
		val rt = mapMatrix.glToMapPx(pRT.x, pRT.y)
		val lb = mapMatrix.glToMapPx(pLB.x, pLB.y)
		val rb = mapMatrix.glToMapPx(pRB.x, pRB.y)
		val minX = floor(min(min(lt.x, rt.x), min(lb.x, rb.x)).toDouble()).toInt()
		val maxX = ceil(max(max(lt.x, rt.x), max(lb.x, rb.x)).toDouble()).toInt()
		val minY = floor(min(min(lt.y, rt.y), min(lb.y, rb.y)).toDouble()).toInt()
		val maxY = ceil(max(max(lt.y, rt.y), max(lb.y, rb.y)).toDouble()).toInt()
		val tileMinX = floor(minX / tileSizePx.toDouble()).toInt()
		val tileMaxX = floor((maxX - 1) / tileSizePx.toDouble()).toInt()
		val tileMinY = floor(minY / tileSizePx.toDouble()).toInt()
		val tileMaxY = floor((maxY - 1) / tileSizePx.toDouble()).toInt()
		val result = ArrayList<Tile>()
		for (ty in tileMinY..tileMaxY) {
			for (tx in tileMinX..tileMaxX) {
				val coord = TileCoord(level = 0, x = tx, y = ty)
				val tile = cache.get(coord) ?: continue
				if (tile.isReady && tile.textureId != 0) result.add(tile)
			}
		}
		return result
	}

    // ================== 边框复制辅助 ==================
    private fun bytesPerPixel(glFormat: Int): Int {
        return when (glFormat) {
            GLES20.GL_LUMINANCE, GLES20.GL_ALPHA, 0x8229 /*GL_RED*/ -> 1
            else -> 4
        }
    }

    private fun uploadFullBorders(region: RegionBuffer, border: Int) {
        val bpp = bytesPerPixel(region.glFormat)
        val rowStride = region.width * bpp
        val src = region.pixelBuffer.duplicate().apply { position(0); limit(region.width * region.height * bpp) }

        // 顶部与底部行
        val topRow = ByteArray(rowStride)
        src.position(0); src.get(topRow)
        val bottomRow = ByteArray(rowStride)
        src.position((region.height - 1) * rowStride); src.get(bottomRow)
        val topBuf = ByteBuffer.allocateDirect(rowStride).put(topRow).apply { position(0) }
        val bottomBuf = ByteBuffer.allocateDirect(rowStride).put(bottomRow).apply { position(0) }
        // 顶部边
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, border, 0, region.width, border, region.glFormat, GLES20.GL_UNSIGNED_BYTE, topBuf)
        // 底部边
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, border, border + region.height, region.width, border, region.glFormat, GLES20.GL_UNSIGNED_BYTE, bottomBuf)

        // 左右列
        val leftCol = ByteArray(region.height * bpp)
        val rightCol = ByteArray(region.height * bpp)
        for (row in 0 until region.height) {
            src.position(row * rowStride)
            src.get(leftCol, row * bpp, bpp)
            src.position(row * rowStride + (region.width - 1) * bpp)
            src.get(rightCol, row * bpp, bpp)
        }
        val leftBuf = ByteBuffer.allocateDirect(region.height * bpp).put(leftCol).apply { position(0) }
        val rightBuf = ByteBuffer.allocateDirect(region.height * bpp).put(rightCol).apply { position(0) }
        // 左边
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, border, border, region.height, region.glFormat, GLES20.GL_UNSIGNED_BYTE, leftBuf)
        // 右边
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, border + region.width, border, border, region.height, region.glFormat, GLES20.GL_UNSIGNED_BYTE, rightBuf)
    }

    private fun uploadPartialBorders(region: RegionBuffer, req: TileGridUtils.TileSubRequest, border: Int) {
        val bpp = bytesPerPixel(region.glFormat)
        val rowStride = region.width * bpp
        val src = region.pixelBuffer.duplicate().apply { position(0); limit(region.width * region.height * bpp) }

        // 顶部
        if (req.yOffsetInTile == 0) {
            val topRow = ByteArray(rowStride)
            src.position(0); src.get(topRow)
            val buf = ByteBuffer.allocateDirect(rowStride).put(topRow).apply { position(0) }
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, req.xOffsetInTile + border, 0, req.subWidth, border, region.glFormat, GLES20.GL_UNSIGNED_BYTE, buf)
        }
        // 底部
        if (req.yOffsetInTile + req.subHeight == tileSizePx) {
            val bottomRow = ByteArray(rowStride)
            src.position((region.height - 1) * rowStride); src.get(bottomRow)
            val buf = ByteBuffer.allocateDirect(rowStride).put(bottomRow).apply { position(0) }
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, req.xOffsetInTile + border, border + req.yOffsetInTile + req.subHeight, req.subWidth, border, region.glFormat, GLES20.GL_UNSIGNED_BYTE, buf)
        }
        // 左侧
        if (req.xOffsetInTile == 0) {
            val leftCol = ByteArray(req.subHeight * bpp)
            for (i in 0 until req.subHeight) {
                val srcPos = i * rowStride
                src.position(srcPos)
                src.get(leftCol, i * bpp, bpp)
            }
            val buf = ByteBuffer.allocateDirect(req.subHeight * bpp).put(leftCol).apply { position(0) }
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, req.yOffsetInTile + border, border, req.subHeight, region.glFormat, GLES20.GL_UNSIGNED_BYTE, buf)
        }
        // 右侧
        if (req.xOffsetInTile + req.subWidth == tileSizePx) {
            val rightCol = ByteArray(req.subHeight * bpp)
            for (i in 0 until req.subHeight) {
                val srcPos = i * rowStride + (region.width - 1) * bpp
                src.position(srcPos)
                src.get(rightCol, i * bpp, bpp)
            }
            val buf = ByteBuffer.allocateDirect(req.subHeight * bpp).put(rightCol).apply { position(0) }
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, border + req.xOffsetInTile + req.subWidth, req.yOffsetInTile + border, border, req.subHeight, region.glFormat, GLES20.GL_UNSIGNED_BYTE, buf)
        }
    }
}


