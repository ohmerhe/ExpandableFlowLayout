package com.ohmerhe.flowlayout.demo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (0 .. 16).forEach {
            val tagView = LayoutInflater.from(this@MainActivity).inflate(R.layout.view_tag, null, false) as
                    TextView
            tagView.text = "标签00$it"
            flowLayout.addView(tagView)
        }

    }
}
