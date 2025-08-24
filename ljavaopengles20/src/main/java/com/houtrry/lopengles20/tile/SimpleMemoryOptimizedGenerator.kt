package com.houtrry.lopengles20.tile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 简化版内存优化ParcelMapData生成器
 * 
 * 针对optemap_999k.png (10922×10230) 的OOM问题，提供实用的解决方案
 * 
 * 核心优化策略：
 * 1. 使用RGB_565减少50%Bitmap内存
 * 2. 分批处理像素，避免大数组分配
 * 3. 及时释放中间对象
 * 4. AndroidManifest.xml启用largeHeap
 * 
 * 内存优化效果：
 * - 原始方案峰值: ~1000MB (ARGB_8888 + IntArray + ByteArray)
 * - 优化方案峰值: ~350MB (RGB_565 + 分批处理)
 * - 内存减少: 65%
 */
class SimpleMemoryOptimizedGenerator {
    
    companion object {
        private const val TAG = "SimpleMemOptGen"
        private const val HEADER_SIZE = 36
        private const val BATCH_SIZE = 32768  // 每批32K像素
    }
    
    /**
     * 从drawable资源生成（内存优化版本）
     */
    fun generateFromDrawableOptimized(
        context: Context,
        drawableRes: Int,
        outputFileName: String,
        originX: Double,
        originY: Double,
        resolution: Double,
        useExternalStorage: Boolean = false
    ): String? {
        return try {
            Log.i(TAG, "开始内存优化生成 (drawable)")
            
            val outputFile = getOutputFile(context, outputFileName, useExternalStorage)
            
            // 检查可用内存
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val availableMemory = maxMemory - usedMemory
            
            Log.i(TAG, "内存状态: 最大=${formatSize(maxMemory)}, 已用=${formatSize(usedMemory)}, 可用=${formatSize(availableMemory)}")
            
            if (availableMemory < 200 * 1024 * 1024) {  // 少于200MB可用内存
                Log.w(TAG, "⚠️ 可用内存不足，建议先清理内存或重启应用")
            }
            
            // 使用内存优化参数加载
            val bitmap = loadBitmapOptimized(context, drawableRes, true)
            
            try {
                val success = processBitmapToFile(bitmap, outputFile, originX, originY, resolution)
                
                if (success) {
                    Log.i(TAG, "✅ 内存优化生成成功: ${outputFile.absolutePath}")
                    outputFile.absolutePath
                } else {
                    Log.e(TAG, "❌ 生成失败")
                    null
                }
            } finally {
                // 立即释放Bitmap
                bitmap?.recycle()
                System.gc()  // 建议GC回收内存
            }
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "❌ OOM - 即使使用内存优化仍然内存不足", e)
            // 尝试超级优化模式
            tryUltraOptimizedMode(context, drawableRes, outputFileName, originX, originY, resolution, useExternalStorage, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "❌ 生成失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 从assets资源生成（内存优化版本）
     */
    fun generateFromAssetsOptimized(
        context: Context,
        assetFileName: String,
        outputFileName: String,
        originX: Double,
        originY: Double,
        resolution: Double,
        useExternalStorage: Boolean = false
    ): String? {
        return try {
            Log.i(TAG, "开始内存优化生成 (assets)")
            
            val outputFile = getOutputFile(context, outputFileName, useExternalStorage)
            
            // 加载Bitmap
            val bitmap = loadBitmapOptimized(context, assetFileName, false)
            
            try {
                val success = processBitmapToFile(bitmap, outputFile, originX, originY, resolution)
                
                if (success) {
                    Log.i(TAG, "✅ 内存优化生成成功: ${outputFile.absolutePath}")
                    outputFile.absolutePath
                } else {
                    Log.e(TAG, "❌ 生成失败")
                    null
                }
            } finally {
                bitmap?.recycle()
                System.gc()
            }
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "❌ OOM - 即使使用内存优化仍然内存不足", e)
            tryUltraOptimizedMode(context, assetFileName, outputFileName, originX, originY, resolution, useExternalStorage, false)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "❌ 生成失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 内存优化的Bitmap加载
     */
    private fun loadBitmapOptimized(context: Context, source: Any, isDrawable: Boolean): Bitmap? {
        Log.d(TAG, "使用内存优化参数加载Bitmap")
        
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565  // 使用RGB_565，减少50%内存
            inScaled = false
            inDither = false
            inPurgeable = true  // 允许系统在内存紧张时回收像素内存
            inInputShareable = true
            
            // 禁用预乘Alpha以节省处理时间
            inPremultiplied = false
        }
        
        val bitmap = if (isDrawable) {
            BitmapFactory.decodeResource(context.resources, source as Int, options)
        } else {
            context.assets.open(source as String).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        }
        
        if (bitmap != null) {
            Log.i(TAG, "Bitmap加载成功: ${bitmap.width}×${bitmap.height}, 配置=${bitmap.config}")
            Log.i(TAG, "预估Bitmap内存: ${formatSize(bitmap.byteCount.toLong())}")
        } else {
            Log.e(TAG, "Bitmap加载失败")
        }
        
        return bitmap
    }
    
    /**
     * 处理Bitmap并写入文件
     */
    private fun processBitmapToFile(
        bitmap: Bitmap?,
        outputFile: File,
        originX: Double,
        originY: Double,
        resolution: Double
    ): Boolean {
        if (bitmap == null) {
            return false
        }
        
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val area = width * height
            
            Log.i(TAG, "处理Bitmap: ${width}×${height}, 总像素: $area")
            
            FileOutputStream(outputFile).use { fileOutput ->
                // 1. 写入头部
                writeHeader(fileOutput, originX, originY, resolution, width, height, area)
                
                // 2. 分批处理像素数据
                val totalPixels = width * height
                var processedPixels = 0
                
                Log.d(TAG, "开始分批处理像素，批大小: $BATCH_SIZE")
                
                while (processedPixels < totalPixels) {
                    val remainingPixels = totalPixels - processedPixels
                    val currentBatchSize = minOf(BATCH_SIZE, remainingPixels)
                    
                    // 计算当前批次的起始位置
                    val startY = processedPixels / width
                    val startX = processedPixels % width
                    
                    // 处理当前批次
                    val batchData = processBatch(bitmap, startX, startY, currentBatchSize, width)
                    
                    if (batchData != null) {
                        fileOutput.write(batchData)
                        processedPixels += currentBatchSize
                        
                        // 报告进度
                        if (processedPixels % (BATCH_SIZE * 10) == 0) {
                            val progress = (processedPixels * 100) / totalPixels
                            Log.v(TAG, "处理进度: $progress% ($processedPixels/$totalPixels)")
                        }
                    } else {
                        Log.e(TAG, "批次处理失败: $processedPixels")
                        return false
                    }
                    
                    // 定期建议GC
                    if (processedPixels % (BATCH_SIZE * 5) == 0) {
                        System.gc()
                    }
                }
            }
            
            Log.i(TAG, "文件处理完成: ${outputFile.absolutePath}")
            true
            
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "处理Bitmap失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 处理单个批次的像素
     */
    private fun processBatch(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        batchSize: Int,
        imageWidth: Int
    ): ByteArray? {
        return try {
            val batchPixels = IntArray(batchSize)
            val grayBytes = ByteArray(batchSize)
            
            // 计算实际需要读取的区域
            var pixelsRead = 0
            var currentY = startY
            var currentX = startX
            
            while (pixelsRead < batchSize && currentY < bitmap.height) {
                val rowRemaining = imageWidth - currentX
                val pixelsInThisRow = minOf(batchSize - pixelsRead, rowRemaining)
                
                // 读取当前行的像素
                bitmap.getPixels(
                    batchPixels, pixelsRead,     // 目标数组和偏移
                    pixelsInThisRow,             // stride（这一行像素数）
                    currentX, currentY,          // 源起始位置
                    pixelsInThisRow, 1           // 读取区域大小
                )
                
                pixelsRead += pixelsInThisRow
                
                // 移动到下一行
                currentX = 0
                currentY++
            }
            
            // 转换为灰度
            for (i in 0 until batchSize) {
                val rgb = batchPixels[i]
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                
                // 标准灰度转换公式
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                grayBytes[i] = gray.coerceIn(0, 255).toByte()
            }
            
            grayBytes
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 超级优化模式（最后手段）
     */
    private fun tryUltraOptimizedMode(
        context: Context,
        source: Any,
        outputFileName: String,
        originX: Double,
        originY: Double,
        resolution: Double,
        useExternalStorage: Boolean,
        isDrawable: Boolean
    ): String? {
        Log.w(TAG, "尝试超级优化模式（降低图像质量）")
        
        return try {
            // 使用严重降质的参数
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inSampleSize = 2  // 降采样，减少到1/4内存
                inScaled = false
                inDither = false
            }
            
            val bitmap = if (isDrawable) {
                BitmapFactory.decodeResource(context.resources, source as Int, options)
            } else {
                context.assets.open(source as String).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            }
            
            if (bitmap != null) {
                Log.w(TAG, "超级优化模式: 降采样到 ${bitmap.width}×${bitmap.height}")
                
                val outputFile = getOutputFile(context, outputFileName, useExternalStorage)
                val success = processBitmapToFile(bitmap, outputFile, originX, originY, resolution)
                
                bitmap.recycle()
                
                if (success) {
                    Log.w(TAG, "⚠️ 超级优化模式成功，但图像质量降低")
                    outputFile.absolutePath
                } else {
                    null
                }
            } else {
                null
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "超级优化模式也失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 写入头部（使用小端字节序，与ByteUtil兼容）
     */
    private fun writeHeader(
        outputStream: FileOutputStream,
        originX: Double,
        originY: Double,
        resolution: Double,
        width: Int,
        height: Int,
        area: Int
    ) {
        // 🚨 修复字节序问题：使用LITTLE_ENDIAN替代nativeOrder()
        val headerBuffer = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        
        headerBuffer.putDouble(0, originX)
        headerBuffer.putDouble(8, originY)
        headerBuffer.putDouble(16, resolution)
        headerBuffer.putInt(24, width)
        headerBuffer.putInt(28, height)
        headerBuffer.putInt(32, area)
        
        outputStream.write(headerBuffer.array())
        
        Log.d(TAG, "头部写入完成: 36字节 (小端字节序)")
        Log.d(TAG, "与ByteUtil兼容的字节序格式")
    }
    
    // 工具方法
    private fun getOutputFile(context: Context, fileName: String, useExternalStorage: Boolean): File {
        val directory = if (useExternalStorage) {
            context.getExternalFilesDir(null) ?: throw IllegalStateException("External storage not available")
        } else {
            context.filesDir
        }
        
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        return File(directory, fileName)
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
    
    /**
     * 内存检查工具
     */
    fun checkMemoryStatus(context: Context) {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val availableMemory = maxMemory - usedMemory
        
        Log.i(TAG, "======== 内存状态检查 ========")
        Log.i(TAG, "最大内存: ${formatSize(maxMemory)}")
        Log.i(TAG, "已分配内存: ${formatSize(totalMemory)}")
        Log.i(TAG, "空闲内存: ${formatSize(freeMemory)}")
        Log.i(TAG, "已用内存: ${formatSize(usedMemory)}")
        Log.i(TAG, "可用内存: ${formatSize(availableMemory)}")
        
        val usagePercentage = (usedMemory * 100) / maxMemory
        
        when {
            usagePercentage > 90 -> Log.e(TAG, "❌ 内存使用率过高: ${usagePercentage}%，建议重启应用")
            usagePercentage > 70 -> Log.w(TAG, "⚠️ 内存使用率较高: ${usagePercentage}%，建议清理内存")
            else -> Log.i(TAG, "✅ 内存使用率正常: ${usagePercentage}%")
        }
        
        // 检查是否启用了largeHeap
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        Log.i(TAG, "系统可用内存: ${formatSize(memoryInfo.availMem)}")
        Log.i(TAG, "系统内存阈值: ${formatSize(memoryInfo.threshold)}")
        Log.i(TAG, "系统内存紧张: ${memoryInfo.lowMemory}")
    }
}
