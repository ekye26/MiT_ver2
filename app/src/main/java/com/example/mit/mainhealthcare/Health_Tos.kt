package com.example.mit.mainhealthcare

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.mit.R

class Health_Tos : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_tos)

        //전체동의
        //필수 서비스이용약관
        val checkBox2 = findViewById<CheckBox>(R.id.checkbox2)
        //필수 개인정보
        val checkBox3 = findViewById<CheckBox>(R.id.checkbox3)
        //선택 위치정보

        val button: Button = findViewById(R.id.next)
        button.isEnabled = false

        checkBox2.setOnClickListener {
            if (checkBox2.isChecked && checkBox3.isChecked) {
                button.isEnabled = true
                button.setOnClickListener{
                    val intent = Intent(this, Health_signup::class.java)
                    startActivity(intent)
                }

            } else {
                button.isEnabled = false
            }
        }

        checkBox3.setOnClickListener {
            if (checkBox2.isChecked && checkBox3.isChecked) {
                button.isEnabled = true
                button.setOnClickListener{
                    val intent = Intent(this, Health_signup::class.java)
                    startActivity(intent)
                }
            } else {
                button.isEnabled = false
            }
        }


        // 상세 약관 보여주기
        //이용약관 버튼 - 서비스 이용약관
        val btn_agr: Button = findViewById(R.id.btn_agr)
        btn_agr.setText(R.string.underlined_text)
        btn_agr.setOnClickListener {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.layout_tos, null)
            val textView: TextView = view.findViewById(R.id.textView)
            textView.setText(R.string.app_arg1)
            val textView1: TextView = view.findViewById(R.id.textView)
            textView1.setText(R.string.app_arg1_1)

            val alertDialog = AlertDialog.Builder(this)
                .setTitle("서비스 이용약관")
                .setPositiveButton("확인", null)
                .setNeutralButton("취소", null)
                .create()

            alertDialog.setView(view)
            alertDialog.show()
        }

        //이용약관 버튼2 - 위치 정보 이용 약관
        val btn_agr2: Button = findViewById(R.id.btn_agr2)
        btn_agr2.setText(R.string.underlined_text)
        btn_agr2.setOnClickListener {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = inflater.inflate(R.layout.layout_tos, null)
            val textView: TextView = view.findViewById(R.id.textView)
            textView.setText(R.string.app_arg2)
            val textView1: TextView = view.findViewById(R.id.textView)
            textView1.setText(R.string.app_arg2_1)


            val alertDialog = AlertDialog.Builder(this)
                .setTitle("개인 정보 이용 약관")
                .setPositiveButton("확인", null)
                .setNeutralButton("취소", null)
                .create()

            alertDialog.setView(view)
            alertDialog.show()
        }

    }
}