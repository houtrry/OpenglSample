package com.houtrry.openglsample.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.houtrry.lopengl.OpenglNativeTestActivity
import com.houtrry.lopengles20.activity.EnhancedMapTestActivity
import com.houtrry.lopengles20.tile.MapMetadata
import com.houtrry.lopengles20.tile.SimpleMemoryOptimizedGenerator
import com.houtrry.openglsample.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_R = 0x1001
        private const val REQUEST_CODE_M = 0x1002
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        startActivity(Intent(this, JavaOpenglES20Activity::class.java))
        binding.nativeOpengl.setOnClickListener {
            startActivity(Intent(this, OpenglNativeTestActivity::class.java))
        }
        binding.javaOpengl.setOnClickListener {
            startActivity(Intent(this, EnhancedMapTestActivity::class.java))
        }
        binding.grayToRgb.setOnClickListener {
            startActivity(Intent(this, GrayToRgbActivity::class.java))
        }
        binding.testOpengl.setOnClickListener {
            startActivity(Intent(this, TestActivity::class.java))
        }
        binding.textBenchmark.setOnClickListener {
            startActivity(Intent(this, TextBenchmarkActivity::class.java))
        }
        binding.generateGrayImageFile.setOnClickListener {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    if (Environment.isExternalStorageManager()) {
                        generateGrayImageFile()
                    } else {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:" + this.packageName)
                        startActivityForResult(intent, REQUEST_CODE_R)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        generateGrayImageFile()
                    } else {
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ), REQUEST_CODE_M
                        )
                    }
                }
                else -> {
                    generateGrayImageFile()
                }
            }
        }
//        startActivity(Intent(this, OpenglNativeTestActivity::class.java))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    generateGrayImageFile()
                } else {
                    Toast.makeText(this, "存储权限获取失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_R) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // 用户已授权，可以进行文件操作
                    generateGrayImageFile()
                } else {
                    // 用户未授权，显示提示信息
                    Toast.makeText(this, "存储权限获取失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateGrayImageFile() {
        GlobalScope.launch(Dispatchers.IO) {
//            val bitmap = BitmapFactory.decodeResource(
//                this@MainActivity.resources,
//                com.houtrry.openglsample.R.drawable.optemap_999k,
//                BitmapFactory.Options().apply {
//                    inScaled = false
//                    inPreferredConfig = Bitmap.Config.RGB_565
//                })
//            Log.d(TAG, "toGrayFile width: ${bitmap.width}, height: ${bitmap.height}")
//            bitmap.toGrayFile(File(filesDir, "optemap_area_75k"))
//
//            assets?.open("optemap_area_2k")?.let {
//                val bytes = it.readBytes()
//                Log.d(TAG, "size: ${bytes.size}")
////                bytes.forEachIndexed { index, byte ->
////                    Log.d(TAG, "byte[$index]: $byte")
////                }
//                for (index in 0 .. 100) {
//                    Log.d(TAG, "byte[$index]: ${bytes[index]}")
//                }
//                it.close()
//            }

//            val example = ParcelMapDataGeneratorExample()
//            val outputPath = example.generateOptemapParcelDataFromDrawable(
//                context = this@MainActivity,
//                drawableRes = R.drawable.optemap_999k,
//                useExternalStorage = true  // 使用内部存储，无需权限
//            )
//            val generator = SimpleMemoryOptimizedGenerator()
//            val outputPath = generator.generateFromAssetsOptimized(
//                context = this@MainActivity,
//                assetFileName = "optemap_999k.png",
//                outputFileName = "optemap_999k",
//                originX = -415.618653,
//                originY = -159.552259,
//                resolution = 0.05
//            )
            val generator = SimpleMemoryOptimizedGenerator()
            val outputPath = generator.generateFromAssetsOptimized(
                context = this@MainActivity,
                assetFileName = "optemap_999k.png",
                outputFileName = "optemap_999k",
                originX = -415.618653,
                originY = -159.552259,
                resolution = 0.05
            )
            Log.d(TAG, "outputPath: $outputPath")
            val targetFile = File(outputPath)
            val fd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val headerBytes = ByteArray(36)

            FileInputStream(fd.fileDescriptor).use { inputStream ->
                val headerRead = inputStream.read(headerBytes)
                if (headerRead != 36) {
                    throw IllegalArgumentException("Invalid header: expected 36 bytes, got $headerRead")
                }
            }

            val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
            val result = MapMetadata(
                originX = headerBuffer.getDouble(0),
                originY = headerBuffer.getDouble(8),
                resolution = headerBuffer.getDouble(16),
                width = headerBuffer.getInt(24),
                height = headerBuffer.getInt(28),
                area = headerBuffer.getInt(32)
            )
            Log.d(TAG, "result: $result")
        }
    }

}