package com.softparadox.caribbeanwar

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

data class Match(
    val uid: String,
    val rank: String
) {
    fun create() {
        val database = Firebase.database
        val myRef = database.getReference("matching/${uid}")
        myRef.setValue(this)
    }
}
