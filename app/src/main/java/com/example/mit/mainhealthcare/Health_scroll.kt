package com.example.mit.mainhealthcare


import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mit.R
import com.example.mit.VitalSign.Health_sleep
import com.example.mit.VitalSign.Health_survey
import com.example.mit.VitalSign.HeartRate
import com.example.mit.VitalSign.StepCounter
import java.sql.DriverManager
import java.sql.SQLException

/** 로그인, 회원가입 > DashBoard
 * 상세정보, 걸음 수, 심박 수, 수면, 설문, 설정 창으로 이동
 */
class Health_scroll : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_scroll)

        val button_data: ImageButton = findViewById(R.id.btn_data)
        val button_step: ImageButton = findViewById(R.id.btn_step)
        val button_heart: ImageButton = findViewById(R.id.btn_heart)
        val button_survey: ImageButton = findViewById(R.id.btn_survay)
        val button_sleep: ImageButton = findViewById(R.id.btn_sleep)
        val button_settings: ImageButton = findViewById(R.id.btn_setting)

        val intro : TextView = findViewById(R.id.intro)
        val ID = intent.getStringExtra("ID")
        intro.text = "<  $ID 님의 MiT  >" // 화면에 아이디를 출력

        button_data.setOnClickListener {
            //아이디를 다음 activity에 보내준다.
            login("$ID")
        }

        button_step.setOnClickListener {
            //아이디를 다음 activity에 보내준다.
            val ID = intent.getStringExtra("ID")
            val TOKEN = intent.getStringExtra("TOKEN")
            val entity_ID = intent.getStringExtra("entity_ID")

            println("----------$ID, $TOKEN-----------")
            step("$ID", "$TOKEN","$entity_ID")
        }

        button_heart.setOnClickListener {
            val ID = intent.getStringExtra("ID")
            val TOKEN = intent.getStringExtra("TOKEN")
            val entity_ID = intent.getStringExtra("entity_ID")

            println("----------$ID, $TOKEN-----------")
            heart("$ID", "$TOKEN","$entity_ID")
        }

        button_survey.setOnClickListener {
            val ID = intent.getStringExtra("ID")
            val TOKEN = intent.getStringExtra("TOKEN")
            val entity_ID = intent.getStringExtra("entity_ID")
            println("----------$ID, $TOKEN-----------")
            survey("$ID", "$TOKEN")
        }

        button_sleep.setOnClickListener {
            //아이디를 다음 activity에 보내준다.
            val ID = intent.getStringExtra("ID")
            val TOKEN = intent.getStringExtra("TOKEN")

            println("----------$ID, $TOKEN-----------")
            sleep("$ID", "$TOKEN")
        }

        button_settings.setOnClickListener {
            val intent = Intent(this, Health_setting::class.java)
            startActivity(intent)
        }
    }

    private fun step(ID: String, TOKEN:String,entity_ID:String) {
        //아이디를 다음 activity에 보내준다.
        val intent = Intent(this, StepCounter::class.java)
        intent.putExtra("ID", ID)
        intent.putExtra("TOKEN", TOKEN)
        intent.putExtra("entity_ID",entity_ID)
        startActivity(intent)
    }

    private fun heart(ID: String, TOKEN:String,entity_ID:String) {
        //아이디를 다음 activity에 보내준다.
        val intent = Intent(this, HeartRate::class.java)
        intent.putExtra("ID", ID)
        intent.putExtra("TOKEN", TOKEN)
        intent.putExtra("entity_ID",entity_ID)
        startActivity(intent)
    }


    private fun sleep(ID: String, TOKEN:String){
        //아이디를 다음 activity에 보내준다.
        val intent = Intent(this, Health_sleep::class.java)
        intent.putExtra("ID", ID)
        intent.putExtra("TOKEN", TOKEN)

        startActivity(intent)
    }

    private fun survey(ID: String, TOKEN:String) {
        //아이디를 다음 activity에 보내준다.
        val intent = Intent(this, Health_survey::class.java)
        intent.putExtra("ID", ID)
        intent.putExtra("TOKEN", TOKEN)


        startActivity(intent)
    }


    /// 메뉴바
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
    // 로그아웃, 설정으로 이동 기능이 있음
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_logout) {
            val logout_intent = Intent(this, Health_main::class.java)
            Toast.makeText(this, "로그아웃을 누르셨습니다.", Toast.LENGTH_SHORT)
            startActivity(logout_intent)
        }

        if (id == R.id.action_settings) {
            val logout_intent = Intent(this, Health_setting::class.java)
            Toast.makeText(this, "설정을 누르셨습니다.", Toast.LENGTH_SHORT)
            startActivity(logout_intent)
        }

        return super.onOptionsItemSelected(item)
    }

    /** 상세 정보 페이지로 이동 시 실행되는 함수
     * 상세 정보에 아이디, 이름, 성별 등이 화면에 출력이 될 수 있게
     * DashBoard 화면에서 데이터베이스에서 아이디를 활용해 상세 정보 데이터를 가지고 온 후
     * 상세 정보 화면에 출력을 시켜준다.
     */
    private fun login(ID: String) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = "jdbc:postgresql://203.255.56.50:5432/postgres"
        val username = "postgres"
        val password = "7452"


        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password)
            println("Connected to PostgreSQL server")
            // account 테이블 에 아이디가 있는지 확인, 이름을 가져온다.
            val sql = "SELECT 이름 FROM account WHERE 아이디='$ID'"
            val statement = connection.createStatement()
            val result = statement.executeQuery(sql)

            while (result.next()) {
                if (ID != null) {
                    // 아이디 값을 활용해 생년월일, 성별 등 값을 가져온다.
                    val sql1 = "SELECT 생년월일 FROM account  WHERE 아이디 = '$ID'"
                    val statement1 = connection.createStatement()
                    val result1 = statement1.executeQuery(sql1)

                    val sql2 = "SELECT 성별 FROM account  WHERE 아이디= '$ID'"
                    val statement2 = connection.createStatement()
                    val result2 = statement2.executeQuery(sql2)


                    while (result1.next() and result2.next()) {
                        /** 첫 번째 결과 값은 생년월일, 두번째 성별, 세번째 이름 으로 설정
                         * 다음 화면으로 넘겨준다.
                         */
                        val birth = result1.getString("생년월일")
                        val gender = result2.getString("성별")
                        val name = result.getString("이름")

                        if (birth != null) {

                            val intent = Intent(this, Health_information_signup::class.java)
                            // 다음 화면으로 넘겨주기 위한 설정
                            intent.putExtra("GENDER", "성별 : $gender")
                            intent.putExtra("ID", "아이디 : $ID")
                            intent.putExtra("BIRTH", "생년월일 : $birth")
                            intent.putExtra("NAME", "이름 : $name")

                            startActivity(intent)
                        }
                    }
                } else {
                    Toast.makeText(this, "Fail.", Toast.LENGTH_SHORT).show()
                }
            }
            connection.close()
        } catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}