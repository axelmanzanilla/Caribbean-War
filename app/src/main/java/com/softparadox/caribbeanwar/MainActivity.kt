package com.softparadox.caribbeanwar

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {
    private lateinit var logoImage: ImageView
    private lateinit var playButton: Button
    private lateinit var rankingButton: Button
    private lateinit var accountButton: Button
    private lateinit var signText: TextView
    private lateinit var loginButton: Button
    private lateinit var usernameView: TextView
    private lateinit var usernameEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logoImage = findViewById(R.id.logo_image)
        playButton = findViewById(R.id.play_button)
        rankingButton = findViewById(R.id.ranking_button)
        accountButton = findViewById(R.id.account_button)

        Glide.with(this).load(R.drawable.logo).into(logoImage)


        playButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        rankingButton.setOnClickListener {
            startActivity(Intent(this, RankingActivity::class.java))
        }

        accountButton.setOnClickListener {
            val dialog = Dialog(this)
            dialog.window?.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
            dialog.setContentView(R.layout.layout_account)
            dialog.show()

            signText = dialog.window?.findViewById(R.id.sign_text)!!
            loginButton = dialog.window?.findViewById(R.id.login_button)!!
            usernameView = dialog.window?.findViewById(R.id.username_view)!!
            usernameEdit = dialog.window?.findViewById(R.id.username_edit)!!
            emailEdit = dialog.window?.findViewById(R.id.email_edit)!!
            passwordEdit = dialog.window?.findViewById(R.id.password_edit)!!

            var isCreatingAccount = false

            signText.setOnClickListener {
                if (isCreatingAccount) {
                    signText.text = "You do not have an account?"
                    loginButton.text = "Login"
                    usernameEdit.visibility = View.GONE
                    usernameView.visibility = View.GONE
                    isCreatingAccount = false
                } else {
                    signText.text = "Do you already have an account?"
                    loginButton.text = "Create"
                    usernameEdit.visibility = View.VISIBLE
                    usernameView.visibility = View.VISIBLE
                    isCreatingAccount = true
                }
            }

            loginButton.setOnClickListener {
                if (
                    emailEdit.text.isBlank() ||
                    passwordEdit.text.isBlank() ||
                    (isCreatingAccount && usernameEdit.text.isBlank())
                ) {
                    Toast.makeText(this, "There are empty fields", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, emailEdit.text.trim(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}