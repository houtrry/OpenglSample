package com.houtrry.lopengles20.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.houtrry.lopengles20.databinding.ActivityJavaOpenglEs20Binding
import com.houtrry.lopengles20.weight.MapView

class JavaOpenglES20Activity : AppCompatActivity() {

    companion object {
        private const val TAG = "JavaOpenglES20Activity"
    }
    private lateinit var binding: ActivityJavaOpenglEs20Binding
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJavaOpenglEs20Binding.inflate(layoutInflater)
        setContentView(binding.root)


        mapView = MapView(this)
        binding.mapViewRoot.addView(mapView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
//        GlobalScope.launch(Dispatchers.IO) {
//            val costTime = measureTimeMillis {
//                val bitmap = BitmapFactory.decodeResource(this@JavaOpenglESActivity.resources, R.drawable.optemap_217k)
//                mapView.addShaper(MapShaper(bitmap, readRawText(R.raw.map_vertex), readRawText(R.raw.map_fragment)))
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