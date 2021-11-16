package com.example.mit.VitalSign


import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import com.example.mit.R
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class Health_sleep : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_sleep)

        SleepTime()  //  ---1번

    }

    private fun SleepTime() {   //   ---2번
        val textView: TextView = findViewById(R.id.textView1)
        val timePicker: TimePicker = findViewById(R.id.time1)
        timePicker.setOnTimeChangedListener { _, hour, minute ->  // timePicker가 변화될 때
            var hour = hour

            if (textView != null) {
                val hour = if (hour < 10) "0$hour" else hour   // 0~9까지 : 00시~ 09시로 한다
                val min = if (minute < 10) "0$minute" else minute //위와 동일

                val msg = "잠든 시간 :  $hour : $min "
                textView.text = msg  // textView에 msg 값 들어간다
                textView.visibility = ViewGroup.VISIBLE  // 그 값이 보여진다

                WakeTime("$hour", "$min")  //  ---3번
                // WakeTime 함수에 위의 값을 넣는다 (이유) 총 수면 시간 계산을 위해
            }
        }
    }

    private fun WakeTime(sleep_hour: String, sleep_min: String) {  //  ---4번
        val textView: TextView = findViewById(R.id.textView2)
        val timePicker: TimePicker = findViewById(R.id.time2)
        val textView3: TextView = findViewById(R.id.textView3)

        // timePicker를 수정 시 textView에 시간이 변경 및 MQTT로 값 설정이 된다.
        timePicker.setOnTimeChangedListener { _, hour, minute ->
            var hour = hour

            if (textView != null) {
                val wake_hour = if (hour < 10) "0$hour" else hour
                val wake_min = if (minute < 10) "0$minute" else minute

                val msg = "일어난 시간 : $wake_hour : $wake_min "

                // 아래 : 총 수면 시간 계산
                if (wake_min.toString().toInt() >= sleep_min.toInt()) {
                    // 예  -  잠든 시각 : 5시, 일어난 시각 오후 3시 일 시 구분하여 계산
                    //        잠든 분 또한 마찬가지로 계산 하여 textView3로 출력 시킴
                    val cal_hour = if (sleep_hour.toInt() >= hour.toString().toInt() ){
                        24 + wake_hour.toString().toInt() - sleep_hour.toInt()
                    } else { wake_hour.toString().toInt() - sleep_hour.toInt()  }

                    val cal_min = wake_min.toString().toInt() - sleep_min.toInt()
                    val result = "수면시간 : $cal_hour 시간 $cal_min 분"
                    textView3.text = result
                    connect(cal_hour, cal_min)
                } else {
                    val cal_hour = if (sleep_hour.toInt() >= wake_hour.toString().toInt() ){
                        24 + wake_hour.toString().toInt() - sleep_hour.toInt() - 1
                    } else { wake_hour.toString().toInt() - sleep_hour.toInt() - 1 }
                    val cal_min = 60 + wake_min.toString().toInt() - sleep_min.toInt()
                    val result = "수면시간 : $cal_hour 시간 $cal_min 분"
                    textView3.text = result
                    connect(cal_hour, cal_min) // ---5번
                }

                textView.text = msg
                textView.visibility = ViewGroup.VISIBLE
                textView3.visibility = ViewGroup.VISIBLE
            }
        }
    }

    /** MQTT 전송 부분 */
    private fun connect(hour: Int, min: Int) {  // ----6번
        val radioGroup: RadioGroup = findViewById(R.id.sleep_q)
        val button: Button = findViewById(R.id.btn_sleept)

        val topic = ""  // 토픽 이름
        val mqttAndroidClient = MqttAndroidClient(this, "tcp://" + "" + ":1883", MqttClient.generateClientId())

        try {
            val options = MqttConnectOptions()
            val TOKEN = intent.getStringExtra("TOKEN")
            options.userName = "$TOKEN"
            mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    try {
                        mqttAndroidClient.subscribe("$topic", 0) //연결에 성공시 토픽으로 subscribe함
                    } catch (e: MqttException) { e.printStackTrace() }
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

        /** 수면 XML 아래 있는 버튼 클릭 시 수행하는 행동
         * 버튼 클릭시 아이디, 수면 질(상, 중, 하),수면 시간 전송된다.
         * 단, 순서대로(아이디 입력부터 ~) 했을 시 아이디가 보내진다*/
        button.setOnClickListener {
            val ID = intent.getStringExtra("ID")
            println("====아이디 : $ID")

            //수면의 질 선택 시 best, so-so, worst 가 전송된다
            val sleep_state = when (radioGroup.checkedRadioButtonId) {
                R.id.top -> "best"
                R.id.mid -> "so-so"
                R.id.low -> "worst"
                else -> "Not Yet"
            }
            // try~catch 구문 : mqtt로 try 구문의 값이 전송된다. 가는 값은 ByteArray() 고정

            try {
                val msg = "{\"sleep\":$hour : $min}"
                val message = MqttMessage()
                message.payload = msg.toByteArray()
                // MQTT -> ThingsBoard 형식 : {sleep:시간} 고정
                mqttAndroidClient.publish("$topic","{\"sleep\":$hour : $min}".toByteArray(), 0, false)
                mqttAndroidClient.publish("$topic", "{\"state\":$sleep_state}".toByteArray(), 0, false)
                Log.d(TAG, "보낸 값 : $msg")

            } catch (e: MqttException) { e.printStackTrace() }
        }
    }

    // 버퍼에 연결 되지 않았을 시
    private val disconnectedBufferOptions: DisconnectedBufferOptions
        private get() {
            val disconnectedBufferOptions = DisconnectedBufferOptions()
            disconnectedBufferOptions.isBufferEnabled = true
            disconnectedBufferOptions.bufferSize = 100
            disconnectedBufferOptions.isPersistBuffer = true
            disconnectedBufferOptions.isDeleteOldestMessages = false
            return disconnectedBufferOptions
        }
    private val mqttConnectionOption: MqttConnectOptions
        private get() {
            val mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isCleanSession = false
            mqttConnectOptions.isAutomaticReconnect = true
            mqttConnectOptions.setWill("offline", "offline".toByteArray(), 1, true)
            return mqttConnectOptions
        }
}
