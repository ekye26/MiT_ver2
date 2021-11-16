package com.example.mit.mainhealthcare

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mit.R
import android.content.Intent
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

/** DashBoard 창 > 상세 정보
 * 키, 몸무게 등 수정이 가능
 */
class Health_information : AppCompatActivity() {
    val TAG = "DATA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_information)

        val button: Button = findViewById(R.id.button)
        val T_birth : TextView = findViewById(R.id.BIRTH_data)
        val T_id : TextView = findViewById(R.id.ID_data)
        val T_name : TextView = findViewById(R.id.NAME_data)
        val T_gender : TextView = findViewById(R.id.GENDER_data)

        // DashBoard(Health_scroll.kt)에서 넘겨준 아이디, 이름, 생년월일, 성별의 값이 있는지 확인
        if(intent.hasExtra("ID")) {
            T_id.text = intent.getStringExtra("ID")
        }
        if(intent.hasExtra("NAME")) {
            T_name.text = intent.getStringExtra("NAME")
        }
        if(intent.hasExtra("BIRTH")) {
            T_birth.text = intent.getStringExtra("BIRTH")
        }

        if (intent.hasExtra("GENDER")) {
            T_gender.text = intent.getStringExtra("GENDER")
        }

        // 버튼 클릭 시 입력된 키, 몸무게의 값으로 변경된다.
        button.setOnClickListener {

            val ID = intent.getStringExtra("ID")
            Log.e(TAG, "ID : $ID")

            val height: EditText = findViewById(R.id.height)
            val weight: EditText = findViewById(R.id.weight)
            //키, 몸무게의 값을 String으로 변환시켜준다.
            val HEIGHT = height.text.toString()
            val WEIGHT = weight.text.toString()
            //connect 함수에 아이디, 키, 몸무게의 값을 넘겨준다.
            connect("$ID", "$HEIGHT", "$WEIGHT")

        }
    }

    //메뉴바
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            val logout_intent = Intent(this, Health_main::class.java)
            Toast.makeText(this, "로그아웃을 누르셨습니다.", Toast.LENGTH_SHORT)
            startActivity(logout_intent)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun connect(ID: String, HEIGHT: String, WEIGHT: String) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = 
        val username = 
        val password = 

        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password) //연결한다,
            println("Connected to PostgreSQL server")

            // 데이터베이스에 키, 몸무게의 값을 업데이트(수정)하는 코드
            var sql = "UPDATE information SET 신장 = '$HEIGHT', 몸무게 = '$WEIGHT' WHERE 아이디 = '$ID'"

            val statement: PreparedStatement = connection.prepareStatement(sql)
            statement.executeUpdate()

            //키의 값이 null 이 아니면 성공이 된다.
            if (HEIGHT != null) {
                println("A new contact has been inserted.")
                Toast.makeText(this, "저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }
            //연결을 끊어준다.
            connection.close()

        } catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            e.printStackTrace()
            Toast.makeText(this, "저장을 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
