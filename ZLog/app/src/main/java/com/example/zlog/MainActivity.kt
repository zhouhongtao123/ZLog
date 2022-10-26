package com.example.zlog

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.jasonzhou.zlog.handler.CrashHandler

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CrashHandler.INSTANCE.init(this)
    }
}