package com.softparadox.caribbeanwar

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

data class Match(
    val uid: String,
    val username: String,
    val rank: String,
    val points: Int,
    val date: Long = System.currentTimeMillis(),
    var available: Boolean = false
) {
    fun create() {
        val database = Firebase.database
        val myRef = database.getReference("matching/$uid")
        myRef.setValue(this)
    }
}