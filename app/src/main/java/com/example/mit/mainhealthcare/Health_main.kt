package com.example.mit.mainhealthcare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.mit.R

class Health_main : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_health_main)
        val button1: Button = findViewById<Button>(R.id.title_login)
        val button2: Button = findViewById(R.id.title_signup)

        button1.setOnClickListener {
            val nextlogin = Intent(this, Health_login::class.java)
            startActivity(nextlogin)
        }

        button2.setOnClickListener {
            val nextsignup = Intent(this, Health_Tos::class.java)
            startActivity(nextsignup)
        }

    }
}