package com.softparadox.caribbeanwar

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    private lateinit var logoImage: ImageView
    private lateinit var playButton: Button
    private lateinit var rankingButton: Button
    private lateinit var optionsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logoImage = findViewById(R.id.logo_image)
        playButton = findViewById(R.id.play_button)
        rankingButton = findViewById(R.id.ranking_button)
        optionsButton = findViewById(R.id.options_button)
        Glide.with(this).load(R.drawable.logo).into(logoImage)


        playButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        rankingButton.setOnClickListener {
            startActivity(Intent(this, RankingActivity::class.java))
        }

        optionsButton.setOnClickListener {
            startActivity(Intent(this, OptionsActivity::class.java))

//            val dialog = Dialog(this)
//            dialog.window?.setLayout(
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.WRAP_CONTENT
//            )
//            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//            dialog.window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
//            dialog.setContentView(R.layout.activity_options)
//            dialog.show()
        }
    }
}