package com.example.mit.VitalSign

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.mit.R
import com.example.mit.databinding.ActivityHealthSurveyBinding
import github.hongbeomi.dividerseekbar.DividerSeekBar
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class Health_survey : AppCompatActivity() {

    private lateinit var binding: ActivityHealthSurveyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHealthSurveyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val topic = "v1"
        val mqttAndroidClient = MqttAndroidClient(
            this,
            "tcp://" + "" + ":1883",
            MqttClient.generateClientId()
        )

        try {
            val options = MqttConnectOptions()
            val TOKEN = intent.getStringExtra("TOKEN")
            options.userName = "$TOKEN"
            mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(com.example.mit.VitalSign.TAG, "Connection success")
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {   //연결에 실패한경우
                    Log.e("connect_fail", "Failure $exception")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            Log.e("connect_fail", "Failure $e")

        }

        mqttAndroidClient.setCallback(object : MqttCallback {
            //클라이언트의 콜백을 처리하는부분
            override fun connectionLost(cause: Throwable) {}

            @Throws(Exception::class)
            override fun messageArrived(
                topic: String,
                message: MqttMessage
            ) {    //모든 메시지가 올때 Callback method
                if (topic == "$topic") {     //topic 별로 분기처리하여 작업을 수행할수도있음
                    val msg = String(message.payload)
                    Log.e("arrive message : ", msg)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}

        })

        val a : DividerSeekBar =  findViewById(R.id.divider)

        a.apply {
            min = 1
            max = 5
            setOffActivatedEvent()
            setOnActivatedEvent()
            setTextLocationMode(DividerSeekBar.TEXT_LOCATION_BOTTOM)
            setTextInterval(10)
            setTextColor(R.color.white)
            setTextSize(R.dimen.sp_12)
            setSeaLineColor(R.color.light_blue_600)
            setSeaLineStrokeWidth(R.dimen.dp_1)
            setDividerInterval(10)
            setDividerColor(R.color.light_blue_600)
            setDividerStrokeWidth(R.dimen.dp_1)
            setActiveMode(DividerSeekBar.ACTIVE_MODE_MINIMUM)
            setActivateTargetValue(50)
            setOnDividerSeekBarChangeStateListener(
                object : DividerSeekBar.OnDividerSeekBarChangeStateListener {
                    override fun onProgressEnabled(dividerSeekBar: DividerSeekBar, progress: Int) {
                        binding.textViewTest.apply {
                            text = "$progress"
                            println("11=================")
                            println(progress)
                            println("=================")
                        }
                    }

                    override fun onProgressDisabled(dividerSeekBar: DividerSeekBar, progress: Int) {
                        binding.textViewTest.apply {
                            text = "$progress"
                            println("22=================")
                            println(progress)
                            println("=================")
                        }
                    }
                })

        }


        binding.btnSur.setOnClickListener {

            /** 설문 값들을 저장할 수 있는 변수 선언  */
            val one = mutableListOf<String>() //1번
            val two = mutableListOf<String>() //2번

            if (binding.checkBox1.isChecked) {
                one.add("하")
            }
            //"평소와 유사"}

            if (binding.checkBox4.isChecked) {
                one.add("최하")
            }
            // "피곤/스트레스" }

            if (binding.checkBox2.isChecked) {
                one.add("보통")
            }
            // "질병초기" }

            if (binding.checkBox5.isChecked) {
                one.add("보통이상")
            }
            // "질병" }

            if (binding.checkBox3.isChecked) {
                one.add("상")
            }
            // "질병회복" }

            if (binding.checkBox6.isChecked) {
                one.add("최상")
            }

            ///////////////////////////////////////////"1-1기타" }

            if (binding.checkBox26.isChecked) {
                two.add("인후통")
            }
            // "두통" }

            if (binding.checkBox25.isChecked) {
                two.add("후각상실")
            }
            // "복통" }

            if (binding.checkBox31.isChecked) {
                two.add("복통")
            }
            //"기침" }

            if (binding.checkBox21.isChecked) {
                two.add("오한")
            }
            // "피로" }

            if (binding.checkBox22.isChecked) {
                two.add("설사")
            }
            //"오한" }

            if (binding.checkBox24.isChecked) {
                two.add("시각상실")
            }
            // "설사" }

            if (binding.checkBox30.isChecked) {
                two.add("두통")
            }
            // "인후통" }

            if (binding.checkBox32.isChecked) {
                two.add("기침")
            }
            //"호흡곤란" }

            if (binding.checkBox.isChecked) {
                two.add("피로")
            }
            // "미각상실" }

            if (binding.checkBox35.isChecked) {
                two.add("기타")
            }
            // "안구통증" }


            // 설문 내용이 포함된 리스트 출력

            println("========================")
            println("$one")
            println("$two")
            println("========================")

            val stress_list : MutableList<String> = mutableListOf()

            if (binding.textViewTest.text.toString() == "") {
                stress_list.add("1")
                println("11>>>>>>>>>>>>>>>>>>>$stress_list")
            } else {
                stress_list.add(binding.textViewTest.text.toString())
                println("22>>>>>>>>>>>>>>>>>>>$stress_list")
            }

            println("33>>>>>>>>>>>>>>>>>>>$stress_list")

            try {

                val msg = "[{\"survey1\": $one},{\"survey2\": $stress_list},{\"survey3\": $two}]"

                val message = MqttMessage()
                message.payload = msg.toByteArray()

                mqttAndroidClient.publish("$topic", message.payload, 0, false)
                Log.d(com.example.mit.VitalSign.TAG, "보낸 값 : $msg")
                //Log.d(com.example.mit.GoogleFit.TAG, "보낸 값 : $msg2")
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }
}

