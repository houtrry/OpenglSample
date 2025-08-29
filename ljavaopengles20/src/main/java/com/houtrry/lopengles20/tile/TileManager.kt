package com.houtrry.lopengles20.tile

import android.opengl.GLES20
import android.util.Log
import com.houtrry.lopengles20.utils.PerfMetrics
import com.houtrry.lopengles20.data.MapMatrix
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * 瓦片管理器：负责大地图的瓦片化渲染和资源管理
 * 
 * 核心功能：
 * - 可见瓦片计算：根据视口和变换矩阵确定需要渲染的瓦片
 * - 瓦片缓存管理：LRU策略管理瓦片生命周期，控制内存使用
 * - 纹理池管理：复用GL纹理对象，减少频繁创建/销毁开销
 * - 动态扩展支持：支持地图尺寸动态增长和负索引瓦片
 * - 分帧加载：按帧限速加载瓦片，避免阻塞渲染线程
 * - 边框扩展：可选的1px边框扩展，消除瓦片间采样缝隙
 * 
 * 设计原则：
 * - 线程安全：所有GL操作必须在GL线程中执行
 * - 内存可控：通过LRU和池化策略控制资源使用
 * - 性能优先：分帧加载和视口裁剪减少不必要的工作
 * - 扩展友好：预留LOD层级接口，支持未来多分辨率需求
 * 
 * @param tileSizePx            单个瓦片的像素尺寸（宽高相等），推荐256/512
 * @param lruCapacity           LRU缓存容量，控制最大瓦片数量
 * @param texturePoolCapacity   纹理池容量，控制可复用的GL纹理数量
 */
class TileManager(
    private val tileSizePx: Int = 512,
    lruCapacity: Int = 256,
    texturePoolCapacity: Int = 256
) {

    companion object {
        private const val TAG = "TileManager"
    }

    // ================== 核心状态变量 ==================
    /** 地图总宽度（像素），用于边界裁剪和瓦片范围计算 */
    private var mapWidthPx: Int = 0
    /** 地图总高度（像素），用于边界裁剪和瓦片范围计算 */
    private var mapHeightPx: Int = 0
    /** 视口宽度（像素），用于可见区域计算 */
    private var viewportWidthPx: Int = 0
    /** 视口高度（像素），用于可见区域计算 */
    private var viewportHeightPx: Int = 0

    // ================== 缓存和资源管理 ==================
    /** LRU 缓存：管理 TileCoord → Tile 的映射，包括占位瓦片和已就绪瓦片 */
    private val cache = LruCache<TileCoord, Tile>(lruCapacity)
    /** 纹理池：预分配和复用 GL 纹理对象，减少 glGenTextures 调用频率 */
    private val texturePool = TexturePool(texturePoolCapacity)
    /** 待加载队列：存储需要异步加载的瓦片坐标，保持插入顺序 */
    private val pendingLoads = LinkedHashSet<TileCoord>()
    /** 区域数据提供者：负责获取指定区域的像素数据 */
    private var regionProvider: RegionProvider? = null

    // ================== 边框扩展配置 ==================
    /** 是否启用边框扩展：可选的1px扩展边，用于抑制瓦片间采样缝隙（默认关闭） */
    private var enableBorder: Boolean = false
    /** 边框扩展尺寸：扩展边的像素宽度，默认1像素 */
    private var borderSizePx: Int = 1

    /**
     * 设置区域数据提供者
     * 
     * @param provider 区域数据提供者实例，null表示清空
     *                 提供者负责在需要时获取指定区域的像素数据
     */
    fun setRegionProvider(provider: RegionProvider?) {
        regionProvider = provider
    }

    /**
     * 全局销毁：释放所有资源，包括瓦片纹理和纹理池
     * 
     * 执行操作：
     * 1. 遍历缓存中的所有瓦片，删除其GL纹理
     * 2. 清空LRU缓存
     * 3. 销毁纹理池中的所有纹理
     * 4. 清空待加载队列
     * 5. 重置区域提供者
     * 
     * 注意事项：
     * - 必须在拥有有效GL上下文的GL线程中调用
     * - 调用后TileManager不可再使用，除非重新初始化
     * - 建议在Activity/Fragment销毁时调用
     */
    fun destroy() {
        val keys = cache.keys()
        for (k in keys) {
            val t = cache.get(k) ?: continue
            val id = t.textureId
            if (id != 0) {
                val tmp = intArrayOf(id)
                GLES20.glDeleteTextures(1, tmp, 0)
            }
            cache.remove(k)
        }
        texturePool.destroy()
        pendingLoads.clear()
        regionProvider = null
    }

    /**
     * 启用/关闭边框扩展功能（用于抑制瓦片间采样缝隙）
     * 
     * 边框扩展原理：
     * - 在每个瓦片周围添加边框像素，复制边缘像素值
     * - 采样时UV坐标映射到内容区域，避免采样到相邻瓦片
     * - 有效消除线性插值导致的瓦片间缝隙
     * 
     * 性能影响：
     * - 启用后纹理尺寸增加：(size + border*2) × (size + border*2)
     * - 增加边框像素拷贝的CPU开销
     * - 建议在视觉质量要求高且缝隙明显时启用
     * 
     * @param enable        是否启用边框扩展，默认关闭
     * @param borderSizePx  边框像素宽度，默认1像素，最小值0
     */
    fun setBorderEnabled(enable: Boolean, borderSizePx: Int = 1) {
        this.enableBorder = enable
        this.borderSizePx = borderSizePx.coerceAtLeast(0)
    }

    /**
     * 设置地图总尺寸
     * 
     * 用途：
     * - 用于可见瓦片计算时的边界裁剪
     * - 确定边缘瓦片的实际尺寸
     * - 支持地图动态扩展，可随时更新
     * 
     * @param widthPx  地图总宽度（像素）
     * @param heightPx 地图总高度（像素）
     */
    fun setMapSize(widthPx: Int, heightPx: Int) {
        mapWidthPx = widthPx
        mapHeightPx = heightPx
        Log.d(TAG, "setMapSize end, mapWidthPx: $widthPx, mapHeightPx: $heightPx")
    }

    /**
     * 设置视口尺寸
     * 
     * 用途：
     * - 可见瓦片计算的基础参数
     * - 屏幕坐标到GL坐标转换的依据
     * - 通常对应渲染表面的像素尺寸
     * 
     * @param widthPx  视口宽度（像素）
     * @param heightPx 视口高度（像素）
     */
    fun setViewportSize(widthPx: Int, heightPx: Int) {
        viewportWidthPx = widthPx
        viewportHeightPx = heightPx
        Log.d(TAG, "setViewportSize end, viewportWidthPx: $widthPx, viewportHeightPx: $heightPx")
    }

    /**
     * LOD 层级选择（占位框架）：根据当前变换状态选择最适合的细节层级
     * 
     * 实现思路：
     * - 思路1：计算地图像素到屏幕像素的映射比例，选择接近1:1的层级
     * - 思路2：将屏幕1px反推到地图空间的长度，据此估算合适层级
     * - 思路3：基于缩放级别的阈值分段选择：zoom < 0.5用level1，zoom < 0.25用level2等
     * 
     * 当前状态：固定返回0（最高分辨率层）
     * 后续扩展：return estimateLevel(mapMatrix, viewportWidthPx, viewportHeightPx)
     * 
     * @param mapMatrix 当前地图变换矩阵，包含缩放、平移、旋转信息
     * @return LOD层级，0表示最高分辨率，正数表示逐级降采样
     */
    private fun selectLodLevel(mapMatrix: MapMatrix): Int {
        return 0
    }

    /**
     * 计算当前视口下的可见瓦片列表
     * 
     * 算法流程：
     * 1. 计算屏幕四角在GL坐标系中的位置
     * 2. 将GL坐标转换为地图像素坐标
     * 3. 计算包围盒（AABB）并裁剪到地图边界
     * 4. 将像素范围转换为瓦片索引范围
     * 5. 遍历范围内的瓦片坐标
     * 6. 从缓存获取已有瓦片，或创建占位瓦片
     * 7. 将新的占位瓦片加入待加载队列
     * 
     * 性能优化：
     * - 边界裁剪减少不必要的瓦片创建
     * - 占位瓦片机制避免重复计算
     * - 视口外瓦片通过LRU自动淘汰
     * 
     * @param mapMatrix 当前地图变换矩阵，用于坐标转换
     * @return 可见瓦片列表，包括已就绪和占位瓦片
     */
    fun queryVisibleTiles(mapMatrix: MapMatrix): List<Tile> {
        if (mapWidthPx <= 0 || mapHeightPx <= 0 || viewportWidthPx <= 0 || viewportHeightPx <= 0) {
            Log.d(TAG, "queryVisibleTiles 0, $mapWidthPx -> $mapHeightPx, $viewportWidthPx -> $viewportHeightPx")
            return emptyList()
        }

        // 1) 计算屏幕四角在 GL 空间坐标
        val pLT = mapMatrix.convertScreenToGL(0f, 0f, viewportWidthPx, viewportHeightPx)
        val pRT = mapMatrix.convertScreenToGL(viewportWidthPx.toFloat(), 0f, viewportWidthPx, viewportHeightPx)
        val pLB = mapMatrix.convertScreenToGL(0f, viewportHeightPx.toFloat(), viewportWidthPx, viewportHeightPx)
        val pRB = mapMatrix.convertScreenToGL(viewportWidthPx.toFloat(), viewportHeightPx.toFloat(), viewportWidthPx, viewportHeightPx)
        Log.d(TAG, "queryVisibleTiles 1, $pLT -> $pRT, $pLB -> $pRB")

        // 2) GL → Map 像素坐标（用 MapMatrix.glToWorld + world→map 像素换算）
        val lt = mapMatrix.glToMapPx(pLT.x, pLT.y)
        val rt = mapMatrix.glToMapPx(pRT.x, pRT.y)
        val lb = mapMatrix.glToMapPx(pLB.x, pLB.y)
        val rb = mapMatrix.glToMapPx(pRB.x, pRB.y)
        Log.d(TAG, "queryVisibleTiles 2, $lt -> $rt, $lb -> $rb")

        val minX = floor(min(min(lt.x, rt.x), min(lb.x, rb.x)).toDouble()).toInt()
        val maxX = ceil(max(max(lt.x, rt.x), max(lb.x, rb.x)).toDouble()).toInt()
        val minY = floor(min(min(lt.y, rt.y), min(lb.y, rb.y)).toDouble()).toInt()
        val maxY = ceil(max(max(lt.y, rt.y), max(lb.y, rb.y)).toDouble()).toInt()
        Log.d(TAG, "queryVisibleTiles 3, $minX -> $maxX, $minY -> $maxY")

        // 3) 裁剪到地图范围
        val clippedMinX = max(0, minX)
        val clippedMaxX = min(mapWidthPx, maxX)
        val clippedMinY = max(0, minY)
        val clippedMaxY = min(mapHeightPx, maxY)
        Log.d(TAG, "queryVisibleTiles 4, $clippedMinX -> $clippedMaxX, $clippedMinY -> $clippedMaxY")
        if (clippedMinX >= clippedMaxX || clippedMinY >= clippedMaxY) {
            Log.d(TAG, "queryVisibleTiles has scroll outer of map, show nothing")
            return emptyList()
        }

        // 4) 选择 LOD 层级（占位），并将像素 → 瓦片索引范围（当前仍按 base 层计算）
        val level = selectLodLevel(mapMatrix)
        // TODO: 当启用多层级后，需要将 clippedMin/Max 按 level 的像素分辨率换算后再求瓦片网格范围
        val tileMinX = floor(clippedMinX / tileSizePx.toDouble()).toInt()
        val tileMaxX = floor((clippedMaxX - 1) / tileSizePx.toDouble()).toInt()
        val tileMinY = floor(clippedMinY / tileSizePx.toDouble()).toInt()
        val tileMaxY = floor((clippedMaxY - 1) / tileSizePx.toDouble()).toInt()

        Log.d(TAG, "queryVisibleTiles 5, $tileMinX -> $tileMaxX, $tileMinY -> $tileMaxY, $tileSizePx")
        val result = ArrayList<Tile>()
        // 将内部行索引统一为“左上原点，y向下”
        for (tyUp in tileMinY..tileMaxY) {
            for (tx in tileMinX..tileMaxX) {
                val coord = TileCoord(level = level, x = tx, y = tyUp)
                val cached = cache.get(coord)
                if (cached != null) {
                    Log.d(TAG, "queryVisibleTiles use cached tile -> 2 -> $coord -> $cached")
                    result.add(cached)
                } else {
                    // 占位 Tile：未就绪时返回 isReady=false，纹理 id=0；供渲染层跳过绘制
                    val originX = tx * tileSizePx
                    val originYDown = tyUp * tileSizePx
                    val width = min(tileSizePx, mapWidthPx - originX)
                    val height = min(tileSizePx, mapHeightPx - originYDown)
                    val placeholder = Tile(
                        coord = coord,
                        textureId = 0,
                        widthPx = width,
                        heightPx = height,
                        originXInMapPx = originX,
                        originYInMapPx = originYDown,
                        isReady = false
                    )
                    Log.d(TAG, "queryVisibleTiles generate new tile -> 1 -> $coord -> $placeholder")
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
     * 在GL线程中分帧加载待处理的瓦片
     * 
     * 执行流程：
     * 1. 从待加载队列取出瓦片坐标（FIFO顺序）
     * 2. 通过RegionProvider获取像素数据
     * 3. 从纹理池获取可复用的GL纹理
     * 4. 创建纹理存储空间（考虑边框扩展）
     * 5. 上传像素数据到GPU（glTexSubImage2D）
     * 6. 处理边框扩展（如果启用）
     * 7. 更新缓存中的瓦片状态为就绪
     * 8. 记录性能指标
     * 
     * 分帧加载的优势：
     * - 避免单帧加载过多瓦片造成掉帧
     * - 保持渲染流畅性，分散计算负载
     * - 可根据设备性能调整每帧加载数量
     * 
     * 注意事项：
     * - 必须在GL线程中调用
     * - maxCount应根据目标帧率和设备性能调整
     * - 建议每帧调用，持续消化待加载队列
     * 
     * @param maxCount 本帧最大加载瓦片数量，默认2个，避免掉帧
     * @return 本帧实际加载的瓦片数量
     */
    fun loadPendingOnGlThread(maxCount: Int = 2): Int {
        Log.d(TAG, "loadPendingOnGlThread called, $maxCount/${pendingLoads.size}/${cache.size()}")
        if (pendingLoads.isEmpty()) return 0
        var loaded = 0
        val iterator = pendingLoads.iterator()
        while (iterator.hasNext() && loaded < maxCount) {
            val coord = iterator.next()
            iterator.remove()
            val placeholder = cache.get(coord) ?: continue
            // Provider 以左上角为原点(y向下)，内部tile原点为左下(y向上)，需要转换
            val srcX = placeholder.originXInMapPx
            val srcYTop = placeholder.originYInMapPx

            Log.d(TAG, "loadPendingOnGlThread $coord -> srcX: $srcX, srcYTop: $srcYTop = $mapHeightPx - ${placeholder.originYInMapPx} - ${placeholder.heightPx}")
            val startTimestamp = System.currentTimeMillis()
            val region = regionProvider?.obtainRegion(
                srcX,
                srcYTop,
                placeholder.widthPx,
                placeholder.heightPx
            ) ?: continue
            Log.d(TAG, "obtainRegion $coord cost time is ${System.currentTimeMillis() - startTimestamp}")

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
            Log.d(TAG, "generate tile -> 2 -> $coord -> $ready")
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
				// 内部已改为左上(y向下)，直接使用请求中的y
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
                Log.d(TAG, "generate tile -> 3 -> $coord -> $tile")
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


