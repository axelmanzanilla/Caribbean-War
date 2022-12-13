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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var logoImage: ImageView
    private lateinit var playButton: Button
    private lateinit var rankingButton: Button
    private lateinit var accountButton: Button
    private lateinit var logoutButton: Button
    private lateinit var signText: TextView
    private lateinit var loginButton: Button
    private lateinit var usernameView: TextView
    private lateinit var usernameEdit: EditText
    private lateinit var usernameText: TextView
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logoImage = findViewById(R.id.logo_image)
        playButton = findViewById(R.id.play_button)
        rankingButton = findViewById(R.id.ranking_button)
        accountButton = findViewById(R.id.account_button)
        logoutButton = findViewById(R.id.logout_button)
        usernameText = findViewById(R.id.username_text)

        auth = Firebase.auth
        updateUI(auth.currentUser)

        Glide.with(this).load(R.drawable.logo).into(logoImage)

        playButton.setOnClickListener {
            if (Firebase.auth.currentUser != null) {
                startActivity(Intent(this, MatchActivity::class.java))
            } else {
                Toast.makeText(this, "Login first", Toast.LENGTH_SHORT).show()
            }
        }

        rankingButton.setOnClickListener {
            startActivity(Intent(this, RankingActivity::class.java))
        }

        logoutButton.setOnClickListener {
            Firebase.auth.signOut()
            updateUI(null)
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
                val username = usernameEdit.text.trim().toString()
                val email = emailEdit.text.trim().toString()
                val password = emailEdit.text.toString()

                if (email.isBlank() || password.isBlank() || isCreatingAccount && username.isBlank()) {
                    Toast.makeText(this, "There are empty fields", Toast.LENGTH_SHORT).show()
                } else {
                    if (isCreatingAccount) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val currentUser = auth.currentUser
                                    currentUser?.let { it ->
                                        User(
                                            it.uid,
                                            email,
                                            username,
                                            "Unranked",
                                            0
                                        )
                                    }?.create()
                                    updateUI(currentUser)
                                    dialog.dismiss()
                                } else {
                                    updateUI(null)
                                }
                            }
                    } else {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    updateUI(auth.currentUser)
                                    dialog.dismiss()
                                } else {
                                    updateUI(null)
                                }
                            }
                    }
                }
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            accountButton.visibility = View.GONE
            logoutButton.visibility = View.VISIBLE
            usernameText.visibility = View.VISIBLE

            val database = Firebase.database
            val myRef = database.getReference("users/${user.uid}")
            myRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java)
                    usernameText.text = username
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } else {
            accountButton.visibility = View.VISIBLE
            logoutButton.visibility = View.GONE
            usernameText.visibility = View.GONE
        }
    }
}