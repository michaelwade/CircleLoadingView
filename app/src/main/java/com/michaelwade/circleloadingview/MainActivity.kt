package com.michaelwade.circleloadingview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    lateinit var circleLoadingView: CircleLoadingView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        ProgressBar
        circleLoadingView = findViewById<CircleLoadingView>(R.id.circle_loading)
//        circleLoadingView.setProgressPercent(1.95f)
    }

    override fun onDestroy() {
        super.onDestroy()
//        circleLoadingView.cancelAnimator()
    }
}