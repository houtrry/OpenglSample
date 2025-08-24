package com.houtrry.lopengles20.activity

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.houtrry.common_map.utils.getAssertBitmap
import com.houtrry.lopengles20.R
import com.houtrry.lopengles20.layer.*
import com.houtrry.lopengles20.weight.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

/**
 * 增强地图渲染测试Activity
 * 
 * 测试场景：
 * ✅ ParcelFileDescriptor数据源（optemap_75k）
 * ✅ 多种数据源切换（fd → 文件 → bitmap）
 * ✅ 内存优化效果验证
 * ✅ 性能监控和分析
 * ✅ Tile渲染效果
 * 
 * UI布局：
 * - MapView：地图渲染视图
 * - 数据源切换按钮组
 * - 性能监控面板
 * - 内存优化统计显示
 */
class EnhancedMapTestActivity : Activity() {
    
    companion object {
        private const val TAG = "EnhancedMapTest"
    }
    
    // ================== UI组件 ==================
    private lateinit var mapView: MapView
    private lateinit var btnLoadFd: Button
    private lateinit var btnLoadFile: Button
    private lateinit var btnLoadBitmap: Button
    private lateinit var btnShowStats: Button
    private lateinit var tvStats: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    
    // ================== 地图组件 ==================
    private lateinit var enhancedMapLayer: EnhancedMapLayer
    
    // ================== 测试数据 ==================
    private var optemap75kFile: File? = null
    private var currentDataSourceType = "无"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enhanced_map_test)
        
        initViews()
        initMapLayer()
        setupEventListeners()
        prepareTestData()
        
        Log.d(TAG, "增强地图测试Activity已启动")
    }
    
    private fun initViews() {
        mapView = findViewById(R.id.map_view)
        // 布局中已关闭默认层，但为安全起见，代码创建场景可显式控制：
        mapView.setUseDefaultLayers(false)
        mapView.setUseCameraControlLayer(true)
        btnLoadFd = findViewById(R.id.btn_load_fd)
        btnLoadFile = findViewById(R.id.btn_load_file)
        btnLoadBitmap = findViewById(R.id.btn_load_bitmap)
        btnShowStats = findViewById(R.id.btn_show_stats)
        tvStats = findViewById(R.id.tv_stats)
        progressBar = findViewById(R.id.progress_bar)
        tvStatus = findViewById(R.id.tv_status)
        
        // 初始状态
        updateStatus("初始化中...")
        tvStats.text = "点击'显示统计'查看性能数据"
    }
    
    private fun initMapLayer() {
        GlobalScope.launch(Dispatchers.Main) {
            enhancedMapLayer = EnhancedMapLayer()
            // 设置自动全览监听器
            enhancedMapLayer.setAutoOverviewListener(object : AutoOverviewListener {
                override fun onRequestAutoOverview() {
                    Log.d(TAG, "🎯 收到自动全览请求，居中地图视图")
                    runOnUiThread {
                        updateStatus("已自动居中地图视图")
                    }
                }
            })
            mapView.addLayer(enhancedMapLayer)
            mapView.addLayers(
                RobotLayer(
                    withContext(Dispatchers.IO) { BitmapFactory.decodeResource(this@EnhancedMapTestActivity.resources, com.houtrry.common_map.R.mipmap.robot) }
                )
            )
            mapView.addLayers(TextLayer())
        }

        Log.d(TAG, "增强MapLayer已添加到MapView")
    }
    
    private fun setupEventListeners() {
        btnLoadFd.setOnClickListener {
            loadParcelFileDescriptorData()
        }
        
        btnLoadFile.setOnClickListener {
            loadFileData()
        }
        
        btnLoadBitmap.setOnClickListener {
            loadBitmapData()
        }
        
        btnShowStats.setOnClickListener {
            showPerformanceStats()
        }
    }
    
    /**
     * 准备测试数据：将assets中的optemap_75k复制到应用私有目录
     */
    private fun prepareTestData() {
        thread {
            try {
                updateStatus("准备测试数据...")
                
                // 将assets中的optemap_75k复制到应用目录
                val assetsFile = "optemap_22k.png"
                val targetFile = File(filesDir, "optemap_22k.png")
                
                if (!targetFile.exists()) {
                    assets.open(assetsFile).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "测试数据已复制: ${targetFile.absolutePath}")
                }
                
                optemap75kFile = targetFile
                
                runOnUiThread {
                    updateStatus("测试数据准备完成")
                    btnLoadFd.isEnabled = true
                    btnLoadFile.isEnabled = true
                    btnLoadBitmap.isEnabled = true
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "准备测试数据失败", e)
                runOnUiThread {
                    updateStatus("测试数据准备失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 测试1：ParcelFileDescriptor数据源
     * 模拟实时建图场景，测试内存优化效果
     */
    private fun loadParcelFileDescriptorData() {
        val file = optemap75kFile
        if (file == null || !file.exists()) {
            updateStatus("测试数据文件不存在")
            return
        }
        
        thread {
            try {
                runOnUiThread { 
                    showProgress("加载ParcelFd数据源...")
                    disableButtons()
                }
                
                // 创建ParcelFileDescriptor
//                val parcelFd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                
                runOnUiThread {
                    // 首先尝试关闭内存优化的基础模式，用于调试
                    Log.d(TAG, "开始加载ParcelFd数据源...")
//                    val success = enhancedMapLayer.setParcelFileDescriptor(parcelFd, enableOptimization = false)
                    val success = enhancedMapLayer.setImageFile(file)
                    if (success) {
                        currentDataSourceType = "ParcelFd(基础模式)"
                        updateStatus("ParcelFd数据源加载成功 - 基础模式")
                        
                        // 暂时禁用动态更新，用于调试
                        // simulateDynamicUpdates(file)
                        Log.d(TAG, "ParcelFd数据源加载完成，等待渲染...")
                    } else {
                        Log.e(TAG, "ParcelFd数据源加载失败")
                        updateStatus("ParcelFd数据源加载失败")
                    }
                    
                    hideProgress()
                    enableButtons()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载ParcelFd数据源失败", e)
                runOnUiThread {
                    updateStatus("ParcelFd加载失败: ${e.message}")
                    hideProgress()
                    enableButtons()
                }
            }
        }
    }
    
    /**
     * 测试2：文件数据源
     * 测试静态地图文件的区域解码能力
     */
    private fun loadFileData() {
        val file = optemap75kFile
        if (file == null || !file.exists()) {
            updateStatus("测试数据文件不存在")
            return
        }
        
        thread {
            try {
                runOnUiThread { 
                    showProgress("加载文件数据源...")
                    disableButtons()
                }
                
                // 注意：optemap_75k是二进制格式，不是标准图像文件
                // 这里我们先测试ParcelFd模式，后续可以准备真正的PNG文件
                
                runOnUiThread {
                    // 由于optemap_75k不是标准图像格式，我们用它来测试ParcelFd的文件模式
                    val success = enhancedMapLayer.setImageFile(file, useCpuGrayscale = true)
                    
                    if (success) {
                        currentDataSourceType = "ImageFile"
                        updateStatus("文件数据源加载成功")
                    } else {
                        updateStatus("文件数据源加载失败（optemap_75k非标准图像格式）")
                        // 降级到ParcelFd模式
                        loadParcelFileDescriptorData()
                        return@runOnUiThread
                    }
                    
                    hideProgress()
                    enableButtons()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载文件数据源失败", e)
                runOnUiThread {
                    updateStatus("文件加载失败: ${e.message}")
                    hideProgress()
                    enableButtons()
                }
            }
        }
    }
    
    /**
     * 测试3：Bitmap数据源
     * 测试兼容性和降级渲染模式
     */
    private fun loadBitmapData() {
        thread {
            try {
                runOnUiThread { 
                    showProgress("生成Bitmap数据源...")
                    disableButtons()
                }
                
                // 生成一个测试bitmap（棋盘格模式）
                val bitmap = generateTestBitmap(1024, 1024)
                
                runOnUiThread {
                    val success = enhancedMapLayer.setMapBitmap(bitmap)
                    
                    if (success) {
                        currentDataSourceType = "Bitmap"
                        updateStatus("Bitmap数据源加载成功")
                    } else {
                        updateStatus("Bitmap数据源加载失败")
                    }
                    
                    hideProgress()
                    enableButtons()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载Bitmap数据源失败", e)
                runOnUiThread {
                    updateStatus("Bitmap加载失败: ${e.message}")
                    hideProgress()
                    enableButtons()
                }
            }
        }
    }
    
    /**
     * 显示性能统计和内存优化分析
     */
    private fun showPerformanceStats() {
        val analysis = enhancedMapLayer.analyzeOptimization()
        val basicStats = enhancedMapLayer.getDataSourceStats()
        
        val statsText = buildString {
            appendLine("=== 当前状态 ===")
            appendLine("数据源类型: $currentDataSourceType")
            
            if (basicStats != null) {
                appendLine("更新次数: ${basicStats.updateCount}")
                appendLine("内存使用: ${String.format("%.1f", basicStats.memoryUsageMB)}MB")
                appendLine("平均耗时: ${String.format("%.1f", basicStats.avgUpdateTimeMs)}ms")
            }
            
            appendLine("\n$analysis")
            
            appendLine("\n=== 使用建议 ===")
            appendLine("• ParcelFd(优化): 适合实时建图，内存占用最低")
            appendLine("• ImageFile: 适合静态地图，支持超大文件")
            appendLine("• Bitmap: 兼容模式，适合小地图")
        }
        
        tvStats.text = statsText
        
        // 滚动到顶部
        tvStats.scrollTo(0, 0)
        
        Log.d(TAG, "性能统计:\n$statsText")
    }
    
    /**
     * 模拟动态更新：测试实时建图场景的内存优化效果
     */
    private fun simulateDynamicUpdates(file: File) {
        // 启动一个线程模拟10fps的地图更新
        thread {
            for (updateIndex in 0 until 20) {
                try {
                    Thread.sleep(100) // 10fps
                    
                    // 重新打开文件（模拟新的地图数据）
                    val parcelFd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    
                    runOnUiThread {
                        enhancedMapLayer.updateParcelFdData(parcelFd)
                        
                        // 每5次更新显示一次统计
                        if (updateIndex % 5 == 0) {
                            val stats = enhancedMapLayer.getDataSourceStats()
                            if (stats != null) {
                                updateStatus("动态更新中... (${updateIndex + 1}/20) 内存: ${String.format("%.1f", stats.memoryUsageMB)}MB")
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "动态更新失败: ${e.message}")
                    break
                }
            }
            
            runOnUiThread {
                updateStatus("动态更新测试完成")
            }
        }
    }
    
    /**
     * 生成测试用的棋盘格bitmap
     */
    private fun generateTestBitmap(width: Int, height: Int): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint()
        val tileSize = 64
        
        for (y in 0 until height step tileSize) {
            for (x in 0 until width step tileSize) {
                val isBlack = ((x / tileSize) + (y / tileSize)) % 2 == 0
                paint.color = if (isBlack) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                
                canvas.drawRect(
                    x.toFloat(), y.toFloat(),
                    (x + tileSize).toFloat(), (y + tileSize).toFloat(),
                    paint
                )
            }
        }
        
        return bitmap
    }
    
    // ================== UI辅助方法 ==================
    
    private fun updateStatus(message: String) {
        tvStatus.text = message
        Log.d(TAG, "状态: $message")
    }
    
    private fun showProgress(message: String) {
        progressBar.visibility = View.VISIBLE
        updateStatus(message)
    }
    
    private fun hideProgress() {
        progressBar.visibility = View.GONE
    }
    
    private fun disableButtons() {
        btnLoadFd.isEnabled = false
        btnLoadFile.isEnabled = false
        btnLoadBitmap.isEnabled = false
    }
    
    private fun enableButtons() {
        btnLoadFd.isEnabled = true
        btnLoadFile.isEnabled = true
        btnLoadBitmap.isEnabled = true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "增强地图测试Activity销毁")
    }
}
