package com.example.mit.mainhealthcare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mit.R
import java.sql.DriverManager
import java.sql.SQLException

class Health_login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_login)

        val button1: Button = findViewById(R.id.login_btn)
        val button2: Button = findViewById(R.id.sign_up_btn)
        val login_id: EditText = findViewById(R.id.login_id)
        // val login_pw: EditText = findViewById(R.id.login_pw)

        button2.setOnClickListener {
            val nextsignup = Intent(this, Health_Tos::class.java)
            startActivity(nextsignup)
        }

        button1.setOnClickListener {

            val ID = login_id.text.toString()
            //val PW = login_pw.text.toString()

            //login("$ID", "$PW")
            login("$ID")

        }
    }

    private fun login(ID : String) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = "jdbc:postgresql://203.255.56.50:5432/postgres"
        val username = "postgres"
        val password = "7452"

        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password)
            println("Connected to PostgreSQL server")
            val sql = "SELECT EXISTS (SELECT * FROM account WHERE 아이디 = '$ID') AS success;"
            val statement = connection.createStatement()
            val result = statement.executeQuery(sql)

            println("로그인 : 1차")

            while (result.next()) {
                //입력되 아이디값이 중복되는지 확인
                val output = result.getBoolean("success")

                if (output) {

                    val sql1 = "SELECT token_key FROM account  WHERE 아이디 = '$ID'"
                    val statement1 = connection.createStatement()
                    val result1 = statement1.executeQuery(sql1)

                    while (result1.next()) {
                        val token = result1.getString("token_key")

                        if (token != null) {
                            val sql2 = "SELECT entity_ID FROM account  WHERE 아이디 = '$ID'"
                            val statement2 = connection.createStatement()
                            val result2 = statement2.executeQuery(sql2)

                            while (result2.next()) {

                                val entity_ID = result1.getString("entity_ID")

                                val intent = Intent(this, Health_scroll::class.java)
                                intent.putExtra("ID", ID)
                                intent.putExtra("TOKEN", token)
                                intent.putExtra("entity_ID",entity_ID)
                                Toast.makeText(this, "로그인이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                startActivity(intent)
                            }
                        }

                        val intent = Intent(this, Health_scroll::class.java)
                        intent.putExtra("ID", ID)
                        intent.putExtra("TOKEN", token)
                        Toast.makeText(this, "로그인이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        startActivity(intent)
                    }



                } else {
                    //중복아닌 경우
                    Toast.makeText(this, "아이디가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            connection.close()

        } catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            Toast.makeText(this, " 로그인 실패했습니다.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            println("@@@@@@@@@@@@@")
        }
    }

}
