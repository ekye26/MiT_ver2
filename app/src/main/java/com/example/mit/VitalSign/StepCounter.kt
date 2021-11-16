package com.example.mit.VitalSign

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mit.BuildConfig
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
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.snackbar.Snackbar
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.sql.DriverManager
import java.sql.SQLException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit


val TAG = "StepCounter"
enum class FitActionRequestCode { SUBSCRIBE, READ_DATA }

class StepCounter: AppCompatActivity() {

    private val fitnessOptions = FitnessOptions.builder()
        .accessActivitySessions(FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
        .build()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_counter)

        fitSignIn(FitActionRequestCode.READ_DATA)
        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)

    }


    private fun checkPermissionsAndRun(fitActionRequestCode: FitActionRequestCode) {
        if (permissionApproved()) {
            fitSignIn(fitActionRequestCode)
        } else {
            requestRuntimePermissions(fitActionRequestCode)
        }
    }


    private fun fitSignIn(requestCode: FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode)
        } else {
            requestCode.let {
                GoogleSignIn.requestPermissions(
                    this,
                    requestCode.ordinal,
                    getGoogleAccount(), fitnessOptions
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                val postSignInAction = FitActionRequestCode.values()[requestCode]
                performActionForRequestCode(postSignInAction)
            }
            else -> oAuthErrorMsg(requestCode, resultCode)
        }
    }

    private fun performActionForRequestCode(requestCode: FitActionRequestCode) =
        when (requestCode) {
            FitActionRequestCode.READ_DATA -> readData()
            //FitActionRequestCode.READ_DATA -> historyAPI()
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

    private fun oAuthPermissionsApproved() = GoogleSignIn.hasPermissions(
        getGoogleAccount(),
        fitnessOptions
    )


    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    private fun subscribe() {

        Fitness.getRecordingClient(this, getGoogleAccount())
            .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Successfully subscribed!")
                } else {
                    Log.w(TAG, "There was a problem subscribing.", task.exception)
                }
            }
    }


    // 걸음 수 데이터 시간 형식 지정
    private fun dumpDataSet(dataSet: DataSet) {
        Log.i(TAG, "Data returned for Data type: ${dataSet.dataType.name}")
        for (dp in dataSet.dataPoints) {
            Log.i(TAG,"Data point:")
            Log.i(TAG,"\tType: ${dp.dataType.name}")
            Log.i(TAG,"\tStart: ${dp.getStartTimeString()}")
            Log.i(TAG,"\tEnd: ${dp.getEndTimeString()}")
            for (field in dp.dataType.fields) {
                Log.i(TAG,"\tField: ${field.name} Value: ${dp.getValue(field)}")
            }
        }
    }

    private fun DataPoint.getStartTimeString() = Instant.ofEpochSecond(this.getStartTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()

    private fun DataPoint.getEndTimeString() = Instant.ofEpochSecond(this.getEndTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()


    fun readData() {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = "jdbc:postgresql://203.255.56.50:5432/thingsboard"
        val username = "postgres"
        val password = "7452"

        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password)
            val entity_ID = intent.getStringExtra("entity_ID")
            val sql =
                "SELECT MAX(ts) FROM ts_kv WHERE key = 37 AND entity_ID = '$entity_ID'"
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

                    val data_list = mutableListOf<Long>()
                    val step_list = mutableListOf<String>()
                    val sdf = SimpleDateFormat("yyyy-MM-dd-kk-mm")

                    val dateFormat: DateFormat = DateFormat.getDateInstance()
                    Log.d(TAG, "---------------------------------")
                    Log.d(TAG, "Range Start: " + dateFormat.format(startTime))
                    Log.d(TAG, "Range End: " + dateFormat.format(endTime))
                    Log.d(TAG, "---------------------------------")

                    val readRequest = DataReadRequest.Builder()
                        .read(DataType.TYPE_STEP_COUNT_DELTA)
                        .enableServerQueries()
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build()
                    Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                        .readData(readRequest)
                        .addOnSuccessListener { response ->
                            val dataSet = response.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)
                            for (dp in dataSet.dataPoints) {
                                Log.i(TAG, "Data point:")
                                Log.i(TAG, "\tType: " + dp.dataType.name)
                                Log.i(TAG, "\tStart: " + dateFormat.format(startTime))
                                Log.i(TAG, "\tEnd: " + dateFormat.format(endTime))
                                for (field in dp.dataType.fields) {
                                    Log.i(TAG, "\tField: "
                                            + field.name.toString() + " Value: " + dp.getValue(field))
                                    step_list.add(dp.getValue(field).toString())
                                    data_list.add(dp.getEndTime(TimeUnit.MILLISECONDS))
                                }
                            }
                            println(step_list)
                            println(data_list)

                            connect(this, step_list, data_list)
                        }
                }
            }
            readData_dispaly()
        }catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            e.printStackTrace()
        }
    }
    /** 여기가 데이터 값 불러오는 함수 */
    fun readData_dispaly() {
        val data_list = mutableListOf<Int>()

        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val startTime = endTime.minusWeeks(1) // 기간 : 현재 1주일로 선택
        Log.i(TAG, "Range Start: $startTime")
        Log.i(TAG, "Range End: $endTime")

        // 오늘 걸음수 데이터
        Fitness.getHistoryClient(this, getGoogleAccount())
            //.readData(DataReadRequest.Builder()
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dataSet ->
                var total = when { //total 읽어옴
                    dataSet.isEmpty -> 0
                    else -> dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                }

                var text7 : TextView = findViewById(R.id.textView7)
                //data_list.add(total)
                text7.text = "총 걸음 수 : $total" // total 출력
                Log.i(TAG, "Total steps: $total")

                val readRequest = DataReadRequest.Builder()
                    .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
                    .bucketByTime(1, TimeUnit.DAYS)
                    .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()

                // 과거 데이터 출력
                Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                    .readData(readRequest)
                    .addOnSuccessListener { response ->
                        for (dataSet in response.buckets.flatMap { it.dataSets }) {
                            dumpDataSet(dataSet)

                            val hit_total = when { // hit_total : 과거 데이터 값
                                dataSet.isEmpty -> 0
                                else -> dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                            }
                            data_list.add(hit_total) //mutableList에 나오는 for문 동안 값을 넣는다
                        }
                        data_list.add(total) // 과거 걸음에 오늘 걸음 수를 넣는다
                        graph(data_list, total)

                    }
                    .addOnFailureListener { e -> Log.w(TAG,"There was an error reading data from Google Fit", e) }
            }
            .addOnFailureListener { e -> Log.w(TAG, "There was a problem getting the step count.", e) }
    }

    private fun graph(data_list : MutableList<Int>, total : Int) {

        // 7일간 걸음 수 데이터 화면에 출력
        var text6 : TextView = findViewById(R.id.textView6)
        var totala = data_list[0]+data_list[1]+data_list[2]+data_list[3]+data_list[4]+data_list[5]+data_list[6]
        text6.text = ("◼ 일주일간 총 걸음수 : "+"$totala"+"\n"+"\n" + "✔ 7일전 : "
                + data_list[0]+"  ✔ 6일전 : " + data_list[1]+"\n"+"✔ 5일전 : " + data_list[2]+"  ✔ 4일전 : "
                + data_list[3]+"\n"+"✔ 3일전 : " + data_list[4]+"  ✔ 2일전 : "
                + data_list[5]+"\n"+"✔ 1일전 : " + data_list[6])

        val lineChart : LineChart = findViewById(R.id.Chart)
        val visitors = ArrayList<Entry>() // 함수 List 인거 같아요

        //데이터 값들의 형식을 변형하여 리스트에 추가

        visitors.add(Entry(1.0f, data_list[0].toFloat()))
        visitors.add(Entry(2.0f, data_list[1].toFloat())) // (8, 400)
        visitors.add(Entry(3.0f, data_list[2].toFloat()))
        visitors.add(Entry(4.0f, data_list[3].toFloat()))
        visitors.add(Entry(5.0f, data_list[4].toFloat()))
        visitors.add(Entry(6.0f, data_list[5].toFloat()))
        visitors.add(Entry(7.0f, data_list[6].toFloat()))
        visitors.add(Entry(8.0f, total.toFloat()))


        // 걸음수 데이터 그래프에 표현

        val lineDataSet = LineDataSet(visitors, "7일 전부터 오늘의 걸음 수 그래프")

        lineDataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        lineDataSet.valueTextColor = Color.BLACK
        lineDataSet.valueTextSize = 16f

        val lineData = LineData(lineDataSet)

        lineChart.data = lineData
        lineChart.invalidate()
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineChart.animateY(2000)
    }

    //MQTT 연결
    private fun connect(context: Context, total : MutableList<String>, times : MutableList<Long>) {

        val topic = ""
        val mqttAndroidClient = MqttAndroidClient(context, "tcp://" + "" + ":1883", MqttClient.generateClientId())

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

                            val msg = "{\"ts\": $a,\"values\":{\"steps\":$b}}"
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


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the main; this adds items to the action bar if it is present.
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
        if (id == R.id.action_settings){
            val settings = Intent(this, Health_setting::class.java)
            startActivity(settings)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun permissionApproved(): Boolean {
        return if (runningQOrLater) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                TODO("VERSION.SDK_INT < Q")
            }
        } else {
            true
        }
    }

    private fun requestRuntimePermissions(requestCode: FitActionRequestCode) {
        val shouldProvideRationale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            } else {
                TODO("VERSION.SDK_INT < Q")
            }
        requestCode.let {
            if (shouldProvideRationale) {
                Log.i(TAG, "Displaying permission rationale to provide additional context.")
                Snackbar.make(
                    findViewById(R.id.main_activity_view),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.ok) {
                        // Request permission
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                            requestCode.ordinal
                        )
                    }
                    .show()
            } else {
                Log.i(TAG, "Requesting permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    requestCode.ordinal
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when {
            grantResults.isEmpty() -> {
                Log.i(TAG, "User interaction was cancelled.")
            }
            grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                val fitActionRequestCode = FitActionRequestCode.values()[requestCode]
                fitActionRequestCode.let {
                    fitSignIn(fitActionRequestCode)
                }
            }
            else -> {
                Snackbar.make(
                    findViewById(R.id.main_activity_view),
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    .show()
            }
        }
    }
}
