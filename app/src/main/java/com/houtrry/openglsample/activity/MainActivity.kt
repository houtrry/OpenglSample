package com.houtrry.openglsample.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.houtrry.openglsample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.nativeOpengl.setOnClickListener {
            startActivity(Intent(this, NativeOpenglActivity::class.java))
        }
        binding.javaOpengl.setOnClickListener {
            startActivity(Intent(this, JavaOpenglESActivity::class.java))
        }
        binding.grayToRgb.setOnClickListener {
            startActivity(Intent(this, GrayToRgbActivity::class.java))
        }
    }

}