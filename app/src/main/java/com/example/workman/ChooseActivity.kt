package com.example.workman

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide


class ChooseActivity : AppCompatActivity() {
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose)

        // Initialize SharedPreferencesHelper
        sharedPreferencesHelper = SharedPreferencesHelper(this)

        val imageView = findViewById<ImageView>(R.id.hire_gif)
        Glide.with(this)
            .load("https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExNWs5MXJ1bXQ5N2lzdHVxY2t5NjQ5eWluYWZjOTF6c2hvd2YzYjJmZSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/l4q8hrNIxa0WHBw9a/giphy.gif")
            .into(imageView)

        val imageView1 = findViewById<ImageView>(R.id.hire_gif2)
        Glide.with(this)
            .load("https://media.lordicon.com/icons/wired/lineal/408-worker-helmet.gif")
            .into(imageView1)


        findViewById<ConstraintLayout>(R.id.hireconst).setOnClickListener {
            sharedPreferencesHelper.saveUserChoice("Hiring")
            val intent = Intent(this, SignUp::class.java)
            intent.putExtra("USER_ROLE", "Hiring")
            startActivity(intent)
            finish()  // Finish AFTER starting SignUp
        }

        findViewById<ConstraintLayout>(R.id.Lookingconst).setOnClickListener {
            sharedPreferencesHelper.saveUserChoice("Worker")
            val intent = Intent(this, SignUp::class.java)
            intent.putExtra("USER_ROLE", "Worker")
            startActivity(intent)
            finish()  // Finish AFTER starting SignUp
        }


//        findViewById<TextView>(R.id.tvHiring).setOnClickListener {
//            sharedPreferencesHelper.saveUserChoice("Hiring")
//            intent.putExtra("USER_ROLE", "Hiring")
//            startActivity(Intent(this, SignUp::class.java))
//        }
//        finish()
//
//
//        findViewById<TextView>(R.id.tvLooking).setOnClickListener {
//            sharedPreferencesHelper.saveUserChoice("Worker")
//            intent.putExtra("USER_ROLE", "Worker")
//            startActivity(Intent(this, SignUp::class.java))
//        }
//        finish()


//        findViewById<TextView>(R.id.tvHiring).setOnClickListener {
//            val intent = Intent(this, SignUp::class.java)
//            intent.putExtra("USER_ROLE", "Hiring")
//            startActivity(intent)
//        }
//        finish()
//
//        findViewById<TextView>(R.id.tvLooking).setOnClickListener {
//            val intent = Intent(this, SignUp::class.java)
//            intent.putExtra("USER_ROLE", "Worker")
//            startActivity(intent)
//        }
//        finish()

    }

    private fun navigateToNext() {
        if (!sharedPreferencesHelper.isLoggedIn()) {
            val signUpIntent = Intent(this, SignUp::class.java)
            startActivity(signUpIntent)
        } else {
            val homeIntent = Intent(this, HomeWorkerDashboardActivity::class.java)
            startActivity(homeIntent)
        }
        finish()
    }


}