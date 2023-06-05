package com.houtrry.openglsample.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Surface
import com.houtrry.openglsample.R

class NativeOpenglActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_opengl)
    }

    /**
     * A native method that is implemented by the 'openglsample' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    external fun initEgl(surface: Surface): Int

    companion object {
        // Used to load the 'openglsample' library on application startup.
        init {
            System.loadLibrary("openglsample")
        }
    }
}