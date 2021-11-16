package com.example.mit.mainhealthcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mit.databinding.ActivityHealthSettingBinding
import com.example.mit.VitalSign.Alarm.Time_Settings


class Health_setting : AppCompatActivity() {

    private lateinit var binding: ActivityHealthSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_settings)

        binding = ActivityHealthSettingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        binding.layoutLogout.setOnClickListener {
            intent = Intent(this, Health_main::class.java)
            startActivity(intent)
        }

        binding.layoutSurvay.setOnClickListener {
            intent = Intent(this, Time_Settings::class.java)
            startActivity(intent)
        }

        // 문의 메일 버튼 클릭시 자동으로 보내는 주소에 입력되어있는 이메일 주소로 입력된다.
        // 또한 아래의 내용이 자동 입력되어 문의를 보내는 데 편리함을 준다.
        binding.layoutContact.setOnClickListener {
            val email = Intent(Intent.ACTION_SEND)
            email.type = "plain/text"
            val address = arrayOf("mitteam21@gmail.com")
            email.putExtra(Intent.EXTRA_EMAIL, address)
            email.putExtra(Intent.EXTRA_SUBJECT, "MiT 애플리케이션 문의 메일")
            email.putExtra(Intent.EXTRA_TEXT, "[애플리케이션 문의]\n \n 기기명 (Device):\n안드로이드 OS (Android OS):\n내용 (Content):\n")
            startActivity(email)
        }

    }
}