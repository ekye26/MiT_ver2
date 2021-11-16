package com.example.mit.VitalSign


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mit.R
import com.example.mit.mainhealthcare.Health_main
import com.example.mit.mainhealthcare.Health_setting
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.snackbar.Snackbar
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.sql.DriverManager
import java.sql.SQLException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

const val tag = "Heart Rate"

class HeartRate : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100 //권한 변수

    private val fitnessOptions = FitnessOptions.builder()
        .accessActivitySessions(FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_LOCATION_SAMPLE, FitnessOptions.ACCESS_READ)
        .build()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate)

        val button: Button = findViewById(R.id.button3)

        button.setOnClickListener {

            fitSignIn(FitActionRequestCode.READ_DATA)
            checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)

        }

        fitSignIn(FitActionRequestCode.READ_DATA)
        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)

    }

    private fun checkPermissionsAndRun(fitActionRequestCode: FitActionRequestCode) {
        if (permissionApproved()) { fitSignIn(fitActionRequestCode) }
        else { requestRuntimePermissions(fitActionRequestCode) }
    }

    /** 구글 피트니스 로그인 확인
     * 사용자가 로그인했는지 확인하고, 로그인된 경우 지정된 기능을 실행합니다.
     * 사용자가 로그인하지 않은 경우, 로그인 후 함수를 지정하여 로그인을 시작합니다.
     */
    private fun fitSignIn(requestCode: FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode)
        } else { requestCode.let { GoogleSignIn.requestPermissions(this, requestCode.ordinal, getGoogleAccount(), fitnessOptions) }
        }
    }

    /** 로그인이 되었을 경우
     *  로그인 콜백을 처리
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                val postSignInAction = FitActionRequestCode.values()[requestCode]
                performActionForRequestCode(postSignInAction)
            }else -> oAuthErrorMsg(requestCode, resultCode)
        }
    }

    /** 구글피트니스 로그인 전달되고 성공 콜백과 함께 반환됩니다.
     * 이를 통해 호출자는 로그인 방법을 지정할 수 있습니다.
     */
    private fun performActionForRequestCode(requestCode: FitActionRequestCode) =
        when (requestCode) {
            FitActionRequestCode.READ_DATA -> readData()
            FitActionRequestCode.SUBSCRIBE -> subscribe()
        }

    private fun oAuthErrorMsg(requestCode: Int, resultCode: Int) {
        val message = """
            There was an error signing into Fit. Check the troubleshooting section of the README
            for potential issues.
            Request code was: $requestCode
            Result code was: $resultCode
        """.trimIndent()
        Log.e(TAG, message)
    }

    private fun oAuthPermissionsApproved() = GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)

    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    /** 데이터 등록 요청 및 심박수 데이터 기록*/
    private fun subscribe() {
        Fitness.getRecordingClient(this, getGoogleAccount())
            .subscribe(DataType.TYPE_HEART_RATE_BPM)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { Log.i(tag, "Successfully subscribed!") }
                else { Log.w(tag, "There was a problem subscribing.", task.exception) }
            }
    }

    /** 심박 수  데이터 값 불러오는 함수
     * 현재 시간의 6시간 전부터 현재까지의 심박 수를 읽어옵니다
     * (시간은 설정에 따라 상이될 수 있음)
     */
    private fun readData() {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = "jdbc:postgresql://:5432/thingsboard"
        val username = ""
        val password = ""

        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password)
            val sql =
                "SELECT MAX(ts) FROM ts_kv WHERE key = 36 AND entity_ID = '32844200-ae2a-11eb-ac33-11278d1fcfcb'"
            val statement = connection.createStatement()
            val result = statement.executeQuery(sql)
            println("Connected to PostgreSQL server")

            while (result.next()) {

                val SEC = 60
                val curTime = System.currentTimeMillis()
                val regTime = result.getLong("max")
                var diffTime = (curTime - regTime) / 1000

                val minus = (diffTime / SEC).toInt()

                println("#########################################")
                println("$minus 분 전")

                if (minus > 0) {
                    val cal: Calendar = Calendar.getInstance()
                    val now = Date()
                    cal.time = now
                    val endTime: Long = cal.timeInMillis
                    cal.add(Calendar.MINUTE, -minus)
                    val startTime: Long = cal.timeInMillis

                    val data_list = mutableListOf<String>()
                    val heart_list = mutableListOf<Int>()
                    val sdf = SimpleDateFormat("yyyy-MM-dd-kk-mm")

                    val dateFormat: DateFormat = DateFormat.getDateInstance()
                    Log.d(tag, "---------------------------------")
                    Log.d(tag, "Range Start: " + dateFormat.format(startTime))
                    Log.d(tag, "Range End: " + dateFormat.format(endTime))
                    Log.d(tag, "---------------------------------")

                    val readRequest = DataReadRequest.Builder()
                        .read(DataType.TYPE_HEART_RATE_BPM)
                        .enableServerQueries()
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build()

                    val addOnSuccessListener = Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                        .readData(readRequest)
                        .addOnSuccessListener { dataReadResult ->
                            if (dataReadResult.dataSets.size > 0) {
                                for (dataSet in dataReadResult.dataSets) {
                                    // 심박 수를 읽어온 시간
                                    val dateFormat: DateFormat =
                                        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
                                    for (dataPoint in dataSet.dataPoints) {

                                        for (field in dataPoint.dataType.fields) {
                                            // 심박 수 값
                                            val mLastHeartBPM =
                                                dataPoint.getValue(field).asFloat().toInt()
                                            val times = dateFormat.format(
                                                dataPoint.getStartTime(
                                                    TimeUnit.MILLISECONDS
                                                )
                                            ).toString()

                                            // 심박 수 시간 : data_list, 심박 수 값 : heart_list
                                            data_list.add(sdf.parse(times).time.toString())
                                            heart_list.add(mLastHeartBPM)
                                        }
                                    }

                                    if (data_list.size > 0) {

                                        connect(this, heart_list, data_list)

                                        val avg: TextView = this.findViewById(R.id.textView16)
                                        val min_max: TextView = this.findViewById(R.id.textView20)
                                        println(heart_list)
                                        println(data_list)

                                        val heartdata = mutableListOf<Int>()

                                        /** 심박 수 화면 출력 방식
                                         * 1. heart_list 7개 이하 시 오류가 생겨 나눠주었습니다.
                                         * 2. list값을 평균, 최대, 최소 를 만들어 출력시켜줍니다.
                                         */
                                        if (heart_list.size > 7) {
                                            val reverse = heart_list.reversed()
                                            for (i in 0..6) {
                                                heartdata.add(reverse[i])
                                            }
                                            val list_sorted = heartdata.sorted()
                                            val list_size = heartdata.sorted().size
                                            val min = list_sorted[0]
                                            val max = list_sorted[list_size - 1]
                                            var sum = 0
                                            for (i in heartdata) sum += i
                                            val list_avg = sum / list_size
                                            avg.text = "평균 심박수 : $list_avg"
                                            min_max.text = "최소 심박수 : $min   |   최대 심박수 : $max"
                                        } else if (heart_list.size > 0) {
                                            val reverse = heart_list.reversed()
                                            for (i in heart_list.indices) {
                                                heartdata.add(reverse[i])
                                            }
                                            val list_sorted = heartdata.sorted()
                                            val list_size = heartdata.sorted().size
                                            val min = list_sorted[0]
                                            val max = list_sorted[list_size - 1]
                                            var sum = 0
                                            for (i in heartdata) sum += i
                                            val list_avg = sum / list_size
                                            avg.text = "평균 심박수 : $list_avg"
                                            min_max.text = "최소 심박수 : $min   |   최대 심박수 : $max"
                                        }
                                        graph(heartdata)
                                    } else if (data_list.size == 0) {

                                        val cal: Calendar = Calendar.getInstance()
                                        val now = Date()
                                        cal.time = now
                                        val endTime: Long = cal.timeInMillis
                                        cal.add(Calendar.HOUR, -2)
                                        val startTime: Long = cal.timeInMillis

                                        val data_list = mutableListOf<String>()
                                        val heart_list = mutableListOf<Int>()
                                        val sdf = SimpleDateFormat("yyyy-MM-dd-kk-mm")

                                        val dateFormat: DateFormat = DateFormat.getDateInstance()
                                        Log.d(tag, "---------------------------------")
                                        Log.d(tag, "Range Start: " + dateFormat.format(startTime))
                                        Log.d(tag, "Range End: " + dateFormat.format(endTime))
                                        Log.d(tag, "---------------------------------")

                                        val readRequest = DataReadRequest.Builder()
                                            .read(DataType.TYPE_HEART_RATE_BPM)
                                            .enableServerQueries()
                                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                                            .build()

                                        val addOnSuccessListener = Fitness.getHistoryClient(
                                            this,
                                            GoogleSignIn.getAccountForExtension(
                                                this,
                                                fitnessOptions
                                            )
                                        )
                                            .readData(readRequest)
                                            .addOnSuccessListener { dataReadResult ->
                                                if (dataReadResult.dataSets.size > 0) {
                                                    for (dataSet in dataReadResult.dataSets) {
                                                        // 심박 수를 읽어온 시간
                                                        val dateFormat: DateFormat =
                                                            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
                                                        for (dataPoint in dataSet.dataPoints) {

                                                            for (field in dataPoint.dataType.fields) {
                                                                // 심박 수 값
                                                                val mLastHeartBPM =
                                                                    dataPoint.getValue(field)
                                                                        .asFloat().toInt()
                                                                val times = dateFormat.format(
                                                                    dataPoint.getStartTime(
                                                                        TimeUnit.MILLISECONDS
                                                                    )
                                                                ).toString()

                                                                // 심박 수 시간 : data_list, 심박 수 값 : heart_list
                                                                data_list.add(sdf.parse(times).time.toString())
                                                                heart_list.add(mLastHeartBPM)
                                                            }
                                                        }

                                                        val avg: TextView =
                                                            this.findViewById(R.id.textView16)
                                                        val min_max: TextView =
                                                            this.findViewById(R.id.textView20)
                                                        println(heart_list)
                                                        println(data_list)

                                                        val heartdata = mutableListOf<Int>()

                                                        /** 심박 수 화면 출력 방식
                                                         * 1. heart_list 7개 이하 시 오류가 생겨 나눠주었습니다.
                                                         * 2. list값을 평균, 최대, 최소 를 만들어 출력시켜줍니다.
                                                         */
                                                        if (heart_list.size > 7) {
                                                            val reverse = heart_list.reversed()
                                                            for (i in 0..6) {
                                                                heartdata.add(reverse[i])
                                                            }
                                                            val list_sorted = heartdata.sorted()
                                                            val list_size = heartdata.sorted().size
                                                            val min = list_sorted[0]
                                                            val max = list_sorted[list_size - 1]
                                                            var sum = 0
                                                            for (i in heartdata) sum += i
                                                            val list_avg = sum / list_size
                                                            avg.text = "평균 심박수 : $list_avg"
                                                            min_max.text =
                                                                "최소 심박수 : $min   |   최대 심박수 : $max"
                                                        } else if (heart_list.size > 0) {
                                                            val reverse = heart_list.reversed()
                                                            for (i in heart_list.indices) {
                                                                heartdata.add(reverse[i])
                                                            }
                                                            val list_sorted = heartdata.sorted()
                                                            val list_size = heartdata.sorted().size
                                                            val min = list_sorted[0]
                                                            val max = list_sorted[list_size - 1]
                                                            var sum = 0
                                                            for (i in heartdata) sum += i
                                                            val list_avg = sum / list_size
                                                            avg.text = "평균 심박수 : $list_avg"
                                                            min_max.text =
                                                                "최소 심박수 : $min   |   최대 심박수 : $max"
                                                        }
                                                        graph(heartdata)
                                                    }
                                                }
                                            }
                                    }
                                }
                            } else {

                                val cal: Calendar = Calendar.getInstance()
                                val now = Date()
                                cal.time = now
                                val endTime: Long = cal.timeInMillis
                                cal.add(Calendar.HOUR, -2)
                                val startTime: Long = cal.timeInMillis

                                val data_list = mutableListOf<String>()
                                val heart_list = mutableListOf<Int>()
                                val sdf = SimpleDateFormat("yyyy-MM-dd-kk-mm")

                                val dateFormat: DateFormat = DateFormat.getDateInstance()
                                Log.d(tag, "---------------------------------")
                                Log.d(tag, "Range Start: " + dateFormat.format(startTime))
                                Log.d(tag, "Range End: " + dateFormat.format(endTime))
                                Log.d(tag, "---------------------------------")

                                val readRequest = DataReadRequest.Builder()
                                    .read(DataType.TYPE_HEART_RATE_BPM)
                                    .enableServerQueries()
                                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                                    .build()

                                val addOnSuccessListener = Fitness.getHistoryClient(
                                    this,
                                    GoogleSignIn.getAccountForExtension(this, fitnessOptions)
                                )
                                    .readData(readRequest)
                                    .addOnSuccessListener { dataReadResult ->
                                        if (dataReadResult.dataSets.size > 0) {
                                            for (dataSet in dataReadResult.dataSets) {
                                                // 심박 수를 읽어온 시간
                                                val dateFormat: DateFormat =
                                                    SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
                                                for (dataPoint in dataSet.dataPoints) {

                                                    for (field in dataPoint.dataType.fields) {
                                                        // 심박 수 값
                                                        val mLastHeartBPM =
                                                            dataPoint.getValue(field).asFloat()
                                                                .toInt()
                                                        val times = dateFormat.format(
                                                            dataPoint.getStartTime(
                                                                TimeUnit.MILLISECONDS
                                                            )
                                                        ).toString()

                                                        // 심박 수 시간 : data_list, 심박 수 값 : heart_list
                                                        data_list.add(sdf.parse(times).time.toString())
                                                        heart_list.add(mLastHeartBPM)
                                                    }
                                                }

                                                val avg: TextView =
                                                    this.findViewById(R.id.textView16)
                                                val min_max: TextView =
                                                    this.findViewById(R.id.textView20)
                                                println(heart_list)
                                                println(data_list)

                                                val heartdata = mutableListOf<Int>()

                                                /** 심박 수 화면 출력 방식
                                                 * 1. heart_list 7개 이하 시 오류가 생겨 나눠주었습니다.
                                                 * 2. list값을 평균, 최대, 최소 를 만들어 출력시켜줍니다.
                                                 */
                                                if (heart_list.size > 7) {
                                                    val reverse = heart_list.reversed()
                                                    for (i in 0..6) {
                                                        heartdata.add(reverse[i])
                                                    }
                                                    val list_sorted = heartdata.sorted()
                                                    val list_size = heartdata.sorted().size
                                                    val min = list_sorted[0]
                                                    val max = list_sorted[list_size - 1]
                                                    var sum = 0
                                                    for (i in heartdata) sum += i
                                                    val list_avg = sum / list_size
                                                    avg.text = "평균 심박수 : $list_avg"
                                                    min_max.text =
                                                        "최소 심박수 : $min   |   최대 심박수 : $max"
                                                } else if (heart_list.size > 0) {
                                                    val reverse = heart_list.reversed()
                                                    for (i in heart_list.indices) {
                                                        heartdata.add(reverse[i])
                                                    }
                                                    val list_sorted = heartdata.sorted()
                                                    val list_size = heartdata.sorted().size
                                                    val min = list_sorted[0]
                                                    val max = list_sorted[list_size - 1]
                                                    var sum = 0
                                                    for (i in heartdata) sum += i
                                                    val list_avg = sum / list_size
                                                    avg.text = "평균 심박수 : $list_avg"
                                                    min_max.text =
                                                        "최소 심박수 : $min   |   최대 심박수 : $max"
                                                }
                                                graph(heartdata)
                                            }
                                        }
                                    }
                            }
                        }
                }
            }
        }catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            e.printStackTrace()
        }
    }

    private fun graph(heartdata :MutableList<Int>) {


        val lineChart : LineChart = findViewById(R.id.Chart1)
        val heart = ArrayList<Entry>()

        /** 그래프
         * heartdata에는 7개의 값이 들어있습니다.
         * 역순으로 리스트를 바꿔 준 후 그래프로 출력 시켜줍니다.
         * (뒤에서부터 값을 reversed해주었기에 그래프를 그리기 위해선 다시 reversed 해주었니다.
         */
        for (i in heartdata.indices) {
            val reverse_heartdata = heartdata.reversed()
            heart.add(Entry(i.toFloat(), reverse_heartdata[i].toFloat()))
        }

        val lineDataSet = LineDataSet(heart, "오늘의 심박 수 그래프")

        lineDataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        lineDataSet.valueTextColor = Color.BLACK
        lineDataSet.valueTextSize = 10f

        val lineData = LineData(lineDataSet)

        lineChart.data = lineData
        lineChart.invalidate()
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineChart.animateY(200)

    }

    private fun connect(context: Context, total: MutableList<Int>, times : MutableList<String>) {

        val topic = "v1/devices/me/telemetry"
        val mqttAndroidClient = MqttAndroidClient(context, "tcp://" + "203.255.56.50" + ":1883", MqttClient.generateClientId())

        try {
            val options = MqttConnectOptions()
            val TOKEN = intent.getStringExtra("TOKEN")
            options.userName = "$TOKEN"
            mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    try {
                        // 타임스탬프를 보내기 위해 변환
                        for (i in total.indices) {
                            var a = times[i]
                            var b = total[i]

                            val msg = "{\"ts\": $a,\"values\":{\"heart_rate\":$b}}"
                            val message = MqttMessage()
                            message.payload = msg.toByteArray()
                            mqttAndroidClient.publish("$topic", message.payload, 0, false)
                            Log.d(TAG, "보낸 값 : $msg")
                        }

                    } catch (e: MqttException) {
                        e.printStackTrace()
                        Log.d(TAG, "Connection Fail : $e")
                    }
                }
                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {   //연결에 실패한경우
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            val logout_intent = Intent(this, Health_main::class.java)
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT)
            startActivity(logout_intent)
        }
//        if (id == R.id.action_google_logout) {
//            val logout_intent = Intent(this, Health_scroll::class.java)
//            Toast.makeText(this, "구글피트니스가 로그아웃 되었습니다.", Toast.LENGTH_SHORT)
//            startActivity(logout_intent)
//        }
        if (id == R.id.action_settings){
            val settings = Intent(this, Health_setting::class.java)
            startActivity(settings)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun permissionApproved(): Boolean {
        return if (runningQOrLater) { PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) }
        else { true }
    }


    /** 허용 권한 관련 함수
     * 이전에 요청을 거부했지만 "다시 묻지 않음" 확인란을 선택하지 않은 경우 이 문제가 발생합니다.
     */
    private fun requestRuntimePermissions(requestCode: FitActionRequestCode) {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BODY_SENSORS)
        requestCode.let {
            if (shouldProvideRationale) {
                Log.i(tag, "Displaying permission rationale to provide additional context.")
                Snackbar.make(
                    findViewById(R.id.main_activity_view),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.ok) { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), requestCode.ordinal) }
                    .show()
            } else {
                Log.i(tag, "Requesting permission")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), requestCode.ordinal)
            }
        }
    }
}
