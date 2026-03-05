package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class Education_therapyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_education_therapy)

        val btnrecommend: Button = findViewById(R.id.btn_recommend)
        val btnLevel1: Button = findViewById(R.id.btn_age1)
        val btnLevel2: Button = findViewById(R.id.btn_age2)
        val btnLevel3: Button = findViewById(R.id.btn_age3)
        val btnLevel4: Button = findViewById(R.id.btn_age4)
        val btnLevel5: Button = findViewById(R.id.btn_age5)
        val backButton: ImageView = findViewById(R.id.back)


        btnLevel1.setOnClickListener {
            val intent = Intent(this, Education_subjects_Activity::class.java)
            intent.putExtra("AGE_GROUP", "6")
            startActivity(intent)
        }

        btnLevel2.setOnClickListener {
            val intent = Intent(this, Education_subjects_Activity::class.java)
            intent.putExtra("AGE_GROUP", "7")
            startActivity(intent)
        }

        btnLevel3.setOnClickListener {
            val intent = Intent(this, Education_subjects_Activity::class.java)
            intent.putExtra("AGE_GROUP", "8")
            startActivity(intent)
        }

        btnLevel4.setOnClickListener {
            val intent = Intent(this, Education_subjects_Activity::class.java)
            intent.putExtra("AGE_GROUP", "9")
            startActivity(intent)
        }

        btnLevel5.setOnClickListener {
            val intent = Intent(this, Education_subjects_Activity::class.java)
            intent.putExtra("AGE_GROUP", "10")
            startActivity(intent)
        }


//        btnLevel1.setOnClickListener {
//            startActivity(Intent(this, Education_age4_6_Activity::class.java))
//        }
//        btnLevel2.setOnClickListener {
//            startActivity(Intent(this, Education_age6_10_Activity::class.java))
//        }
//        btnLevel3.setOnClickListener {
//            startActivity(Intent(this, Education_age6_10_Activity::class.java))
//        }
//        btnLevel4.setOnClickListener {
//            startActivity(Intent(this, Education_age6_10_Activity::class.java))
//        }

        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}
