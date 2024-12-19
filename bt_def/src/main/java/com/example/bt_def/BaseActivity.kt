package com.example.bt_def

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView



class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        // initRcView()
        supportFragmentManager.beginTransaction()
            .replace(R.id.placeHolder, DeviceListFragment()).commit()
    }
}