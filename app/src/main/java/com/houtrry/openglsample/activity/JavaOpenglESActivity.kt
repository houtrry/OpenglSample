package com.houtrry.openglsample.activity

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.houtrry.openglsample.R
import com.houtrry.openglsample.databinding.ActivityJavaOpenglEsactivityBinding
import com.houtrry.openglsample.shaper.MapShaper
import com.houtrry.openglsample.weight.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class JavaOpenglESActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "JavaOpenglESActivity"
    }
    private lateinit var binding: ActivityJavaOpenglEsactivityBinding
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJavaOpenglEsactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)


        mapView = MapView(this)
        binding.mapViewRoot.addView(mapView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
//        GlobalScope.launch(Dispatchers.IO) {
//            val costTime = measureTimeMillis {
//                val bitmap = BitmapFactory.decodeResource(this@JavaOpenglESActivity.resources, R.drawable.optemap_217k)
//                mapView.addShaper(MapShaper(bitmap))
//            }
//            Log.d(TAG, "costTime: $costTime")
//        }
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

}