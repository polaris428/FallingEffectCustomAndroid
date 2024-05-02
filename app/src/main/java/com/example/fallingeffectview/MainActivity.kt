package com.example.fallingeffectview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view =findViewById<FallingEffectView>(R.id.fallingEffectView)
        view.snowflakeSpeedMax =2
        view.snowflakeSpeedMin =1
        view.creationResourcesNum = 10

    }
}