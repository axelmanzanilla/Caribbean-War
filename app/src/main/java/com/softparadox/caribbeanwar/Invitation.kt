package com.softparadox.caribbeanwar

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

data class Invitation(
    val uid: String,
    val matchScore: Long
) {
    fun invite(toUid: String) {
        val database = Firebase.database
        val myRef = database.getReference("matching/$toUid/invitations/$uid")
        myRef.setValue(this)
    }
}