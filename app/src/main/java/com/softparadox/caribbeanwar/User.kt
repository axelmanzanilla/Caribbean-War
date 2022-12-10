package com.softparadox.caribbeanwar

import android.util.Log
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime

data class User(
    val uid: String,
    val email: String,
    val username: String,
    val rank: String,
    val points: Int
) {
    fun create() {
        val database = Firebase.database
        val myRef = database.getReference("users/$uid")
        myRef.setValue(this)
    }
}
