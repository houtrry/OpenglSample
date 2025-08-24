package com.houtrry.lopengles20.tile

/**
 * 区域数据提供者接口：负责按需提供指定矩形区域的像素数据
 * 
 * 实现者需要：
 * - 线程安全：可能在GL线程或后台线程中被调用
 * - 内存管理：返回的ByteBuffer应为direct buffer，便于GL上传
 * - 坐标系：使用地图像素坐标，左上角为原点
 * - 错误处理：无效区域或IO错误时返回null
 * 
 * 常见实现：
 * - JpegPngRegionProvider：从JPEG/PNG文件按需解码区域
 * - AlgorithmGrayscaleRegionProvider：从算法输出的灰度数据获取区域
 * - GrayscaleRegionProviderFromBitmap：从Android Bitmap获取区域
 */
interface RegionProvider {
    /**
     * 获取指定矩形区域的像素数据
     * 
     * @param x      区域左上角X坐标（地图像素坐标系）
     * @param y      区域左上角Y坐标（地图像素坐标系）
     * @param width  区域宽度（像素）
     * @param height 区域高度（像素）
     * @return 区域像素数据缓冲区，失败时返回null
     *         返回的buffer position应为0，可直接用于glTexImage2D/glTexSubImage2D
     */
    fun obtainRegion(x: Int, y: Int, width: Int, height: Int): RegionBuffer?
    
    /**
     * 释放Provider相关资源
     * 
     * 包括但不限于：
     * - 关闭文件流和缓存
     * - 返回对象池资源  
     * - 清理内存缓冲区
     * - 取消后台任务
     * 
     * 调用此方法后，Provider应停止工作，obtainRegion()可能返回null
     * 
     * 实现注意事项：
     * - 应当是幂等的（多次调用无副作用）
     * - 应当是线程安全的
     * - 对于无需释放资源的实现，可提供空实现
     */
    fun close() {
        // 默认空实现，保证向后兼容
        // 子类可以覆盖此方法提供具体的资源释放逻辑
    }
}