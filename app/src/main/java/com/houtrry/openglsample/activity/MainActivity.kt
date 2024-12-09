package com.houtrry.openglsample.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.houtrry.common_map.utils.toGrayFile
import com.houtrry.lopengl.OpenglNativeTestActivity
import com.houtrry.lopengles20.activity.JavaOpenglES20Activity
import com.houtrry.openglsample.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


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
        startActivity(Intent(this, JavaOpenglES20Activity::class.java))
        binding.nativeOpengl.setOnClickListener {
            startActivity(Intent(this, OpenglNativeTestActivity::class.java))
        }
        binding.javaOpengl.setOnClickListener {
            startActivity(Intent(this, JavaOpenglES20Activity::class.java))
        }
        binding.grayToRgb.setOnClickListener {
            startActivity(Intent(this, GrayToRgbActivity::class.java))
        }
        binding.generateGrayImageFile.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    generateGrayImageFile()
                } else {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + this.packageName)
                    startActivityForResult(intent, REQUEST_CODE_R)
                }
            }
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
            val bitmap = BitmapFactory.decodeResource(
                this@MainActivity.resources,
                com.houtrry.openglsample.R.drawable.optemap_22k,
                BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.RGB_565
                })
            Log.d(TAG, "toGrayFile width: ${bitmap.width}, height: ${bitmap.height}")
            bitmap.toGrayFile(File(filesDir, "optemap_area_2k"))

            assets?.open("optemap_area_2k")?.let {
                val bytes = it.readBytes()
                Log.d(TAG, "size: ${bytes.size}")
//                bytes.forEachIndexed { index, byte ->
//                    Log.d(TAG, "byte[$index]: $byte")
//                }
                for (index in 0 .. 100) {
                    Log.d(TAG, "byte[$index]: ${bytes[index]}")
                }
                it.close()
            }
        }
    }

}