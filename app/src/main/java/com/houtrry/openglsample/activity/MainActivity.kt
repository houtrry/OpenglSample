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
import com.houtrry.openglsample.R
import com.houtrry.openglsample.data.BubbleText
import com.houtrry.openglsample.data.BubbleTextDrawable
import com.houtrry.openglsample.data.BubbleTextLayoutParam
import com.houtrry.openglsample.data.Vector3
import com.houtrry.openglsample.databinding.ActivityMainBinding
import com.houtrry.openglsample.shaper.BubbleTextLayer
import com.houtrry.openglsample.utils.dp
import com.houtrry.openglsample.utils.getVectorDrawable
import com.houtrry.openglsample.utils.toGrayFile
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
//        startActivity(Intent(this, JavaOpenglESActivity::class.java))
        binding.nativeOpengl.setOnClickListener {
//            startActivity(Intent(this, OpenglNativeTestActivity::class.java))
            val bubbleTextLayer = BubbleTextLayer()
            val bubbleTextList = mutableListOf<BubbleText>(
                BubbleText(
                    "点位1",
                    Vector3(0.0, 0.0, 0.0),
                    BubbleTextLayoutParam(borderStokeWidth = 3.dp),
                    getVectorDrawable(R.drawable.ic_1)?.let { BubbleTextDrawable(it) }
                ),
                BubbleText(
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                    Vector3(0.0, 0.0, 0.0),
                    BubbleTextLayoutParam(borderStokeWidth = 3.dp),
                    getVectorDrawable(R.drawable.ic_2)?.let { BubbleTextDrawable(it, gravity = Gravity.START) }
                ),
                BubbleText(
                    "点位2",
                    Vector3(0.0, 0.0, 0.0),
                    BubbleTextLayoutParam(),
                    getVectorDrawable(R.drawable.ic_1)?.let { BubbleTextDrawable(it, gravity = Gravity.TOP) }
                ),
//                BubbleText(
//                    "点位2",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam(),
//                    getVectorDrawable(R.drawable.ic_2)?.let { BubbleTextDrawable(it, gravity = Gravity.END) }
//                ),
//                BubbleText(
//                    "点位3",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam()
//                ),
//                BubbleText(
//                    "点位4",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam(),
//                    getVectorDrawable(R.drawable.ic_1)?.let { BubbleTextDrawable(it, gravity = Gravity.BOTTOM) }
//                ),
//                BubbleText(
//                    "点位5",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam(),
//                    getVectorDrawable(R.drawable.ic_2)?.let { BubbleTextDrawable(it, gravity = Gravity.LEFT) }
//                ),
//                BubbleText(
//                    "点位6",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam()
//                ),
//                BubbleText(
//                    "点位7",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam(),
//                    getVectorDrawable(R.drawable.ic_1)?.let { BubbleTextDrawable(it, gravity = Gravity.RIGHT) }
//                ),
//                BubbleText(
//                    "点位8",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam(),
//                    getVectorDrawable(R.drawable.ic_2)?.let { BubbleTextDrawable(it, 40.dp, 40.dp, 12.dp, Gravity.START) }
//                ),
//                BubbleText(
//                    "长河落日圆",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam(),
//                    getVectorDrawable(R.drawable.ic_1)?.let { BubbleTextDrawable(it) }
//                ),
//                BubbleText(
//                    "点位10",
//                    Vector3(0.0, 0.0, 0.0),
//                    BubbleTextLayoutParam(),
//                    getVectorDrawable(R.drawable.ic_2)?.let { BubbleTextDrawable(it) }
//                ),


            )
            val bitmap = bubbleTextLayer.generateBubbleTextSpan(bubbleTextList)
            Log.d(TAG, "bitmap: $bitmap")
        }
        binding.javaOpengl.setOnClickListener {
            startActivity(Intent(this, JavaOpenglESActivity::class.java))
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