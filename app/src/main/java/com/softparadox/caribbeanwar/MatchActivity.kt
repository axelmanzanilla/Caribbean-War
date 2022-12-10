package com.softparadox.caribbeanwar

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.childEvents
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.lang.Math.abs
import java.time.LocalDateTime
import java.util.Date

class MatchActivity : AppCompatActivity() {
    private lateinit var refSearching: DatabaseReference
    private lateinit var refSelecting: DatabaseReference
    private lateinit var refMessage: DatabaseReference
    private lateinit var listenerSearching: ValueEventListener
    private lateinit var listenerSelecting: ValueEventListener
    private lateinit var authUser: FirebaseUser
    private lateinit var currentUser: Match
    private lateinit var currentOpponent: Match
    private lateinit var userFoundText: TextView
    private lateinit var cancelButton: Button
    private lateinit var acceptButton: Button
    private lateinit var searchingText: TextView
    private lateinit var mainHandler: Handler
    private lateinit var updateTextTask: Runnable
    private lateinit var database: FirebaseDatabase
    private lateinit var dialogFind: Dialog

    //    Status: [waiting, searching, paired, userReading, finished]
    private var status = "waiting"
    private var lastUidMatched = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        // INIT VARIABLES
        database = Firebase.database
        authUser = Firebase.auth.currentUser!!
        mainHandler = Handler(Looper.getMainLooper())
        lastUidMatched = authUser.uid
        setCurrentUser()
        // To show searching page
        searchingText = findViewById(R.id.searching_text)
        var dots = 0
        updateTextTask = object : Runnable {
            override fun run() {
                var text = "Searching"
                for (i in 1..dots) {
                    text += "."
                }
                dots = if (dots >= 3) 0 else dots + 1
                searchingText.text = text
                mainHandler.postDelayed(this, 400)
            }
        }

        onReceiveMessage("invited") {
            stopSearchingOpponent("paired")
            setCurrentOpponent(it!!) {
                showOpponent()
            }
        }

        onReceiveMessage("cancel") {
            stopSearchingOpponent("userReading")
            dialogFind.dismiss()
            // TODO(Add new dialog you have been cancelled)
            Toast.makeText(this, "El jugador canceló la partida", Toast.LENGTH_SHORT).show()
            startSearchingOpponent()
        }

        refSearching = database.getReference("matching")
        listenerSearching = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val opponentUid =
                    snapshot.children.elementAt(0).child("uid").getValue(String::class.java)!!

                if (lastUidMatched != opponentUid) {
                    lastUidMatched = opponentUid
                    val opponentPoints =
                        snapshot.children.elementAt(0).child("points").getValue(Int::class.java)!!
                    val opponentDate =
                        snapshot.children.elementAt(0).child("date").getValue(Long::class.java)!!
                    val pointScore =
                        100 / (kotlin.math.abs(opponentPoints - currentUser.points) + 1)
                    val dateScore = (opponentDate - currentUser.date) / 1000
                    val matchScore = pointScore + dateScore

                    if (matchScore > 100) {
                        inviteToPlay(opponentUid, matchScore)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

//        Ya te llegaron las invitaciones
        refSelecting = database.getReference("matching/${authUser.uid}/invitations")
        listenerSelecting = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChildren()) {
                    val opponentUid =
                        snapshot.children.elementAt(0).child("uid").getValue(String::class.java)!!
                    setCurrentOpponent(opponentUid) {
                        //TODO: Agregar que cancele la busqueda y que se espere a que lleguen todos y lo haga de golpe
                        sendMessage(opponentUid, "invited", currentUser.uid)
                        showOpponent()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        refSelecting.orderByChild("matchScore").limitToLast(1)
            .addValueEventListener(listenerSelecting)

        startSearchingOpponent()
    }

    fun setCurrentUser() {
        database.getReference("users/${authUser.uid}")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java)!!
                    val rank = snapshot.child("rank").getValue(String::class.java)!!
                    val points = snapshot.child("points").getValue(Int::class.java)!!
                    currentUser = Match(authUser.uid, username, rank, points)
                    addToMatch(currentUser)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun setCurrentOpponent(uid: String, funcion: () -> Unit) {
        database.getReference("matching/$uid")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val opponentUid = snapshot.child("uid").getValue(String::class.java)!!
                    val opponentUsername = snapshot.child("username").getValue(String::class.java)!!
                    val opponentRank = snapshot.child("rank").getValue(String::class.java)!!
                    val opponentPoints = snapshot.child("points").getValue(Int::class.java)!!
                    val opponentDate = snapshot.child("date").getValue(Long::class.java)!!

                    currentOpponent = Match(
                        opponentUid,
                        opponentUsername,
                        opponentRank,
                        opponentPoints,
                        opponentDate
                    )
                    funcion()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroy() {
        super.onDestroy()
//        TODO: Remove all listeners from myrefmessage
        removeFromMatch()
        stopSearchingOpponent("finished")
    }

    fun startSearchingOpponent() {
        status = "searching"
        mainHandler.post(updateTextTask)
        refSearching.orderByChild("date").limitToLast(1)
            .addValueEventListener(listenerSearching)
    }

    fun stopSearchingOpponent(withStatus: String) {
        status = withStatus
        mainHandler.removeCallbacks(updateTextTask)
        refSearching.removeEventListener(listenerSearching)
    }

    fun inviteToPlay(uid: String, matchScore: Long) {
        status = "paired"
        Invitation(currentUser.uid, matchScore).invite(uid)
        Toast.makeText(this, "Has invitado a ${uid} :)", Toast.LENGTH_SHORT)
            .show()
    }

    fun removeInvitations() {
        // Borrar?
        removeInDB("matching/${authUser.uid}/invitations")
        removeInDB("matching/${currentOpponent.uid}/invitations")
    }

    fun addToMatch(match: Match) {
        status = "searching"
        match.create()
    }

    fun removeFromMatch() {
        status = "finished"
        removeInDB("matching/${authUser.uid}")
    }

    fun showOpponent() {
        dialogFind = Dialog(this)
        dialogFind.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialogFind.setCancelable(false)
        dialogFind.setCanceledOnTouchOutside(false)
        dialogFind.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogFind.window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
        dialogFind.setContentView(R.layout.layout_found)

        cancelButton = dialogFind.window?.findViewById(R.id.cancel_game_button)!!
        acceptButton = dialogFind.window?.findViewById(R.id.accept_game_button)!!
        userFoundText = dialogFind.window?.findViewById(R.id.user_found_text)!!
        userFoundText.text = "username: ${currentOpponent.username}\nrank: ${currentOpponent.rank}"

        cancelButton.setOnClickListener {
            sendMessage(currentOpponent.uid, "cancel", currentOpponent.uid)
            removeInvitations()
            startSearchingOpponent()
            status = "searching"
            dialogFind.dismiss()
        }

        acceptButton.setOnClickListener {
            this.finish()
            startActivity(Intent(this, GameActivity::class.java))
        }

        dialogFind.show()
    }

    fun onReceiveMessage(key: String, funcion: (String?) -> Unit) {
        refMessage = database.getReference("matching/${authUser.uid}/$key")
        val listenerMessage = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val info = snapshot.getValue(String::class.java)
                if (info != null) {
                    removeInDB("matching/${authUser.uid}/$key")
                    funcion(info)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        refMessage.addValueEventListener(listenerMessage)
    }

    fun sendMessage(toUid: String, key: String, msg: String) {
        saveInDB("matching/$toUid/$key", msg)
    }

    private fun saveInDB(path: String, value: Any) {
        database.getReference(path).setValue(value)
    }

    private fun removeInDB(path: String) {
        database.getReference(path).removeValue()
    }
}