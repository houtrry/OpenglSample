package com.houtrry.lopengles20.layer

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.view.Gravity
import com.houtrry.common_map.data.Vector3
import com.houtrry.common_map.utils.*
import com.houtrry.lopengles20.R
import com.houtrry.lopengles20.data.BubbleText
import com.houtrry.lopengles20.data.BubbleTextDrawable
import com.houtrry.lopengles20.data.BubbleTextLayoutParam
import com.houtrry.lopengles20.shaper.BubbleTextShape
import java.nio.ShortBuffer

class TextLayer : BaseLayer() {

    companion object {
        private const val TAG = "TextLayer"
    }

    private val bubbleTextShape = BubbleTextShape()


    init {
        Log.d(TAG, "init start")
    }

    private lateinit var textList: MutableList<BubbleText>

    override fun onCreate() {
        textList = mutableListOf<BubbleText>(
        BubbleText(
            "点位1",
            Vector3(0.0, 0.5, 0.0),
            BubbleTextLayoutParam(borderStokeWidth = 1.dp),
            context.getVectorDrawable(R.drawable.ic_1)?.let { BubbleTextDrawable(it) }
        ),
        BubbleText(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            Vector3(0.0, -0.5, 0.0),
            BubbleTextLayoutParam(borderStokeWidth = 1f),
            context.getVectorDrawable(R.drawable.ic_2)?.let { BubbleTextDrawable(it, gravity = Gravity.START) }
        ),
        BubbleText(
            "点位2",
            Vector3(0.5, 0.0, 0.0),
            BubbleTextLayoutParam(),
            context.getVectorDrawable(R.drawable.ic_1)?.let { BubbleTextDrawable(it, gravity = Gravity.TOP) }
        ),
        BubbleText(
            "点位22",
            Vector3(-0.5, 0.0, 0.0),
            BubbleTextLayoutParam(),
            context.getVectorDrawable(R.drawable.ic_2)?.let { BubbleTextDrawable(it, gravity = Gravity.END) }
        ),
//        BubbleText(
//            "点位3",
//            Vector3(-0.5, -0.5, 0.0),
//            BubbleTextLayoutParam()
//        ),
//        BubbleText(
//            "点位4",
//            Vector3(-0.5, 0.5, 0.0),
//            BubbleTextLayoutParam(),
//        ),
//        BubbleText(
//            "点位5",
//            Vector3(0.5, -0.5, 0.0),
//            BubbleTextLayoutParam(),
//        ),
//        BubbleText(
//            "点位6",
//            Vector3(0.5, 0.5, 0.0),
//            BubbleTextLayoutParam()
//        ),
//        BubbleText(
//            "点位7",
//            Vector3(0.0, 0.0, 0.0),
//            BubbleTextLayoutParam(),
//        ),
//        BubbleText(
//            "点位8",
//            Vector3(0.0, 0.0, 0.0),
//            BubbleTextLayoutParam(),
//        ),
//        BubbleText(
//            "长河落日圆",
//            Vector3(0.0, 0.0, 0.0),
//            BubbleTextLayoutParam(),
//        ),
//        BubbleText(
//            "点位10",
//            Vector3(0.0, 0.0, 0.0),
//            BubbleTextLayoutParam(),
//        ),
        )
    }


    override fun onDraw() {
        bubbleTextShape.setSize(viewWidth, viewHeight)
        bubbleTextShape.load(textList)
        bubbleTextShape.draw(program, mapMatrix)
    }
}