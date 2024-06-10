package com.houtrry.openglsample.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.houtrry.openglsample.R
import com.houtrry.openglsample.databinding.ActivityGrayToRgbBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class GrayToRgbActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "GrayToRgbActivity"
    }

    private lateinit var binding: ActivityGrayToRgbBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGrayToRgbBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.srcImage.setImageResource(R.drawable.optemap_22k)
//        binding.srcImage.setImageResource(R.mipmap.ic_launcher)
        binding.grayToRgb.setOnClickListener {
            grayToRgb()
        }
    }

    private fun grayToRgb() {
        GlobalScope.launch(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeResource(
                this@GrayToRgbActivity.resources,
                R.drawable.optemap_217k,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                })
//            val byteCount = bitmap.byteCount
//            val buffer = ByteBuffer.allocate(byteCount)
//            bitmap.copyPixelsToBuffer(buffer)
//            val array = buffer.array()
//            val count = array.size / 4
//            Log.d(TAG, "size->${array.size}, count->$count, byteCount->$byteCount")
//            for (index in 0 until count) {
//                Log.d(
//                    TAG,
//                    "(${array[index * 4].toHexString()}, ${array[index * 4 + 1].toHexString()}, ${array[index * 4 + 2].toHexString()}, ${array[index * 4 + 3].toHexString()}) -> $index/$count"
//                )
//            }
            Log.d(TAG, "grayToRgb start")
            var line = 0
//            for (x in 0 until bitmap.width) {
//                val pixel = bitmap.getPixel(x, line)
//                Log.d(TAG, "${Integer.toHexString(pixel)}, $pixel -> ($x, $line) -> (${Color.red(pixel)/255.0}，${Color.green(pixel)/255.0}，${Color.blue(pixel)/255.0}，${Color.alpha(pixel)/255.0})")
//            }
            Log.e(TAG, "-----------------------------")
            line = bitmap.height/2
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, line)
                Log.d(TAG, "${Integer.toHexString(pixel)}, $pixel -> ($x, $line) -> (${Color.red(pixel)/255.0}，${Color.green(pixel)/255.0}，${Color.blue(pixel)/255.0}，${Color.alpha(pixel)/255.0}) -> (${Color.red(pixel)}，${Color.green(pixel)}，${Color.blue(pixel)}，${Color.alpha(pixel)})")
            }
        }
    }

    private fun Byte.toHexString():String = Integer.toHexString(this.toInt())
}