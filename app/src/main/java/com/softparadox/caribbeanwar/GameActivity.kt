package com.softparadox.caribbeanwar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.allViews

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        for (imageButton in findViewById<LinearLayout>(R.id.game_main).allViews) {
            imageButton.setOnClickListener { view ->
                sendPlay(view.tag.toString())
            }
        }
    }

    private fun sendPlay(square: String) {
        Toast.makeText(this, square, Toast.LENGTH_SHORT).show()
    }
}