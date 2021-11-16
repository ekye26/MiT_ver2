package com.example.mit.mainhealthcare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.core.view.isVisible
import com.example.mit.R
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

class Health_signup : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_signup)

        val button1: Button = findViewById(R.id.signup_ok) // 회원가입 완료 버튼
        val button2: Button = findViewById(R.id.id_check_btn) //아이디 중복확인 버튼

        val sign_id: EditText = findViewById(R.id.sign_up_id) //아이디
        val sign_pw: EditText = findViewById(R.id.sign_up_pw) //비밀번호
        val sign_name: EditText = findViewById(R.id.name) //이름
        val sign_birt: EditText = findViewById(R.id.birth) //생일

        //val gender : RadioGroup = findViewById(R.id.gender)

        val sign_up_pw_ch : TextView = findViewById(R.id.sign_up_pw_ch)
        val sign_pw_re : TextView = findViewById(R.id.sign_pw_re)

        val alchol  = findViewById<CheckBox>(R.id.checkBox42)
        val alchol_S = findViewById<TableRow>(R.id.alchol_s)
        val alchol_1  = findViewById<RadioButton>(R.id.checkBox44)
        val alchol_2  = findViewById<RadioButton>(R.id.checkBox46)
        val alchol_3  = findViewById<RadioButton>(R.id.checkBox45)

        val ciga = findViewById<CheckBox>(R.id.checkBox43)
        val ciga_S = findViewById<TableRow>(R.id.ciga_s)
        val ciga_1 = findViewById<RadioButton>(R.id.checkBox48)
        val ciga_2 = findViewById<RadioButton>(R.id.checkBox47)
        val ciga_3 = findViewById<RadioButton>(R.id.checkBox49)
        val ciga_4 = findViewById<RadioButton>(R.id.checkBox50)

        val y = findViewById<RadioButton>(R.id.yes)
        val n = findViewById<RadioButton>(R.id.no)
        val lo = findViewById<TableLayout>(R.id.lo)

        val radioch : RadioGroup.OnCheckedChangeListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            if (group.id == R.id.yn) {

                if (checkedId == R.id.yes) {

                    if (y.isChecked) {
                        lo.isVisible = true
                        n.isChecked = false
                    }
                }

                else {
                    lo.isVisible = false
                }

            }
        }

        alchol.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                alchol_1.isVisible = true
                alchol_2.isVisible = true
                alchol_3.isVisible = true

            }
            else {
                alchol_1.isVisible = false
                alchol_2.isVisible = false
                alchol_3.isVisible = false
            }
        }

        ciga.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ciga_1.isVisible = true
                ciga_2.isVisible = true
                ciga_3.isVisible = true
                ciga_4.isVisible = true
            }
            else {
                ciga_1.isVisible = false
                ciga_2.isVisible = false
                ciga_3.isVisible = false
                ciga_4.isVisible = false
            }

        }

        sign_up_pw_ch.addTextChangedListener(object : TextWatcher {
            //입력이 끝났을 때
            //4. 비밀번호 일치하는지 확인
            override fun afterTextChanged(p0: Editable?) {
                if(sign_pw.text.toString() == sign_up_pw_ch.text.toString()){
                    sign_pw_re.text = "비밀번호가 일치합니다."
                    // 가입하기 버튼 활성화
                    button1.isEnabled=true
                }
                else{
                    sign_pw_re.text = "비밀번호가 일치하지 않습니다."
                    // 가입하기 버튼 비활성화
                    button1.isEnabled=false
                }
            }
            //입력하기 전
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
            //텍스트 변화가 있을 시
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(sign_pw.text.toString() == sign_up_pw_ch.text.toString()){
                    sign_pw_re.text = "비밀번호가 일치합니다."
                    // 가입하기 버튼 활성화
                    button1.isEnabled=true
                }
                else{
                    sign_pw_re.setText("비밀번호가 일치하지 않습니다.")
                    // 가입하기 버튼 비활성화
                    button1.isEnabled=false
                }
            }
        })

        // 아이디 중복 체크 구현
        button2.setOnClickListener {
            val ID: String = sign_id.text.toString()
            id_check("$ID")
        }



        //postgreSQL로 입력값 전달
        button1.setOnClickListener {

            val gender : RadioGroup = findViewById(R.id.gender)
            val GENDER = when (gender.checkedRadioButtonId) {
                R.id.male -> "남성"
                else -> "여성"
            }

            val ID: String = sign_id.text.toString() //아이디
            //val PW: String = sign_pw.text.toString() //패스워드
            //val PW_CHECK = sign_pw_check.text.toString()
            val NAME: String = sign_name.text.toString() //이름
            val BIRTH: String = sign_birt.text.toString() //생일

            //입력받은 값들을 데이터 베이스에 전송

            val list_a = mutableListOf<String>()

            if (alchol_1.isChecked){list_a.add("주1회")}
            if (alchol_2.isChecked){list_a.add("주3회")}
            if (alchol_3.isChecked){list_a.add("주5회이상")}

            val list_c = mutableListOf<String>()

            if (ciga_1.isChecked){list_c.add("5개비이하")}
            if (ciga_2.isChecked){list_c.add("반갑")}
            if (ciga_3.isChecked){list_c.add("한갑")}
            if (ciga_4.isChecked){list_c.add("한갑 이상")}

            println("++++++++++++++++++")
            println(list_a)
            println(list_c)


            val list = mutableListOf<String>()

            val checkBox23 : CheckBox = findViewById(R.id.checkBox23)
            val checkBox27 : CheckBox = findViewById(R.id.checkBox27)
            val checkBox28 : CheckBox = findViewById(R.id.checkBox28)
            val checkBox29 : CheckBox = findViewById(R.id.checkBox29)
            val checkBox33 : CheckBox = findViewById(R.id.checkBox33)
            val checkBox34 : CheckBox = findViewById(R.id.checkBox34)
            val checkBox36 : CheckBox = findViewById(R.id.checkBox36)
            val checkBox37 : CheckBox = findViewById(R.id.checkBox37)
            val checkBox38 : CheckBox = findViewById(R.id.checkBox38)
            val checkBox39 : CheckBox = findViewById(R.id.checkBox39)
            val checkBox40 : CheckBox = findViewById(R.id.checkBox40)
            val checkBox41 : CheckBox = findViewById(R.id.checkBox41)
            val editTextTextPersonName5 : EditText = findViewById(R.id.editTextTextPersonName5)


            if (checkBox23.isChecked) { list.add("고혈압") }
            if (checkBox27.isChecked) { list.add("골다공증") }
            if (checkBox28.isChecked) { list.add("저혈압") }
            if (checkBox29.isChecked) { list.add("심근경색") }
            if (checkBox33.isChecked) { list.add("이상지질혈증") }
            if (checkBox34.isChecked) { list.add("빈혈") }
            if (checkBox36.isChecked) { list.add("당뇨병") }
            if (checkBox37.isChecked) { list.add("뇌졸증") }
            if (checkBox38.isChecked) { list.add("협심증") }
            if (checkBox39.isChecked) { list.add("폐쇄성폐질환") }
            if (checkBox40.isChecked) { list.add("불면증") }
            if (checkBox41.isChecked) {
                val etc = editTextTextPersonName5.text.toString()
                if(etc == "" || etc == null) { list.add("null") }
                else { list.add("기타 : $etc") }
            }
            println("=================")
            println(list)
            println("=================")

            connect("$ID", "$NAME", "$BIRTH", "$GENDER","$list","$list_a","$list_c")
        }
    }

    /** postgreSQL 연결 및 botton1 클릭 시 값 전달 */
    private fun connect(ID: String, NAME: String, BIRTH: String, GENDER: String, list:String, list_a: String, list_c:String) {

        //이 부분 없으면 오류 이유 파익 x
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = "jdbc:postgresql://203.255.56.50:5432/postgres"
        val username = "postgres"
        val password = "7452"


        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password) //연결한다,
            println("Connected to PostgreSQL server")

            /** 입력 */
            // 쿼리에 입력한다.
            //var sql = "INSERT INTO account ( 아이디, 패스워드, 이름, 생년월일, 성별, timestamp, 기저질환, 음주, 흡연)" + " VALUES (?,?,?,?,?,?,?,?,?)"
            var sql = "INSERT INTO account ( 아이디, 이름, 생년월일, 성별, timestamp, 기저질환, 음주, 흡연)" +
                    " VALUES (?,?,?,?,?,?,?,?)"

            val statement: PreparedStatement = connection.prepareStatement(sql)

            // 이 값을 테이블에 넣음
            /**
            statement.setString(1, "$ID")
            statement.setString(2, "$PW")
            statement.setString(3, "$NAME")
            statement.setString(4, "$BIRTH")
            statement.setString(5, "$GENDER")
            val time = System.currentTimeMillis()
            statement.setString(6,"$time")
            statement.setString(7,"$list")
            statement.setString(8,"$list_a")
            statement.setString(9,"$list_c") */

            statement.setString(1, "$ID")
            statement.setString(2, "$NAME")
            statement.setString(3, "$BIRTH")
            statement.setString(4, "$GENDER")
            val time = System.currentTimeMillis()
            statement.setString(5,"$time")
            statement.setString(6,"$list")
            statement.setString(7,"$list_a")
            statement.setString(8,"$list_c")

            val rows = statement.executeUpdate()

            if (rows > 0) {
                println("A new contact has been inserted.")
                val intent = Intent(this, Health_information_signup::class.java)
                intent.putExtra("ID", ID) //입력받은 값 Health_data_signup으로 전달
                intent.putExtra("NAME",NAME)
                intent.putExtra("BIRTH", BIRTH)
                intent.putExtra("GENDER",GENDER)

                print("===== $ID,$NAME,$BIRTH,$GENDER =====")
                Toast.makeText(this, " 회원가입 완료입니다.", Toast.LENGTH_SHORT).show()
                startActivity(intent)
            }
            connection.close()

        } catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            e.printStackTrace()
            Toast.makeText(this, " 회원가입 실패입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    //아이디 중복확인
    private fun id_check(ID: String) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val jdbcURL = "jdbc:postgresql://203.255.56.50:5432/postgres"
        val username = "postgres"
        val password = "7452"


        try {
            val connection = DriverManager.getConnection(jdbcURL, username, password) //연결한다,
            println("Connected to PostgreSQL server")

            /** 입력 */
            // 쿼리에 입력한다.
            var sql = "SELECT EXISTS (SELECT * FROM account WHERE 아이디 = '$ID') AS success;"
            val statement = connection.createStatement()
            val result = statement.executeQuery(sql)

            while (result.next()) {
                //입력되 아이디값이 중복되는지 확인
                val output = result.getBoolean("success")

                if (output == true) {
                    //중복될 경우
                    Toast.makeText(this, "아이디가 중복됩니다.", Toast.LENGTH_SHORT).show()
                } else {
                    //중복아닌 경우
                    Toast.makeText(this, "아이디 사용이 가능합니다.", Toast.LENGTH_SHORT).show()
                }
            }
            connection.close()
        } catch (e: SQLException) {
            println("Error in connected to PostgreSQL server")
            e.printStackTrace()
        }

    }
}
