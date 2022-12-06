package com.softparadox.caribbeanwar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MatchActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var myRef3: DatabaseReference
    private lateinit var listener: ValueEventListener
    private lateinit var user: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        auth = Firebase.auth
        user = auth.currentUser!!
        var userRank = ""

        val database = Firebase.database
        var myRef = database.getReference("users/${user.uid}")
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userRank = snapshot.child("rank").getValue(String::class.java).toString()
                val myRef2 = database.getReference("matching/${user.uid}")
                myRef2.setValue(user.uid.let {
                    Match(it, userRank)
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })



        myRef3 = database.getReference("matching")
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var score = 0
                for (data in snapshot.children) {
                    val rank = data.child("rank").getValue(String::class.java)
                    val uid2 = data.child("uid").getValue(String::class.java)
                    val invitation = data.child("invitation").getValue(String::class.java)

                    if (rank == userRank &&
                        user.uid != uid2 &&
                        invitation == null
                    ) {
                        if (uid2 != null) {
                            Toast.makeText(baseContext, uid2, Toast.LENGTH_SHORT).show()
                            inviteToPlay(user.uid, uid2)
                            myRef3.removeEventListener(listener)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }


//        myRef3.addListenerForSingleValueEvent(listener)
        myRef3.addValueEventListener(listener)

//        myRef3.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                var score = 0
//                for (data in snapshot.children) {
//                    val rank = data.child("rank").getValue(String::class.java)
//                    val uid2 = data.child("uid").getValue(String::class.java)
//                    val invitation = data.child("invitation").getValue(String::class.java)
//
//                    if (rank == userRank &&
//                        uid != uid2 &&
//                        invitation == null
//                    ) {
//                        Toast.makeText(baseContext, uid2, Toast.LENGTH_SHORT).show()
//                        break
//                    }
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {}
//        })

//        if (auth.currentUser != null) {
//            val database = Firebase.database
//            val myRef = database.getReference("matching/${auth.currentUser!!.uid}")
////            myRef.setValue(auth.currentUser!!.uid)
//            myRef.setValue(User(auth.currentUser!!.uid, "qwe", "axx", "Bronze", 1))
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        myRef3.removeEventListener(listener)
        removeFromMatch(user.uid)
    }

    fun inviteToPlay(fromUid: String, toUid: String) {
        val database = Firebase.database
        val myRefTo = database.getReference("matching/$toUid/invitation")
        val myRefFrom = database.getReference("matching/$fromUid/invitation")
        myRefTo.setValue(fromUid)
        myRefFrom.setValue(toUid)
    }

    fun removeFromMatch(uid: String) {
        val database = Firebase.database
        val myRef = database.getReference("matching/$uid")
        myRef.removeValue()
    }
}