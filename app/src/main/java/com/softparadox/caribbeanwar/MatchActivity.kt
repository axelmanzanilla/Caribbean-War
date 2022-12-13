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
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MatchActivity : AppCompatActivity() {
    private lateinit var refFetching: DatabaseReference
    private lateinit var refSearching: DatabaseReference
    private lateinit var refInvitation: DatabaseReference
    private lateinit var listenerFetching: ValueEventListener
    private lateinit var listenerSearching: ValueEventListener
    private lateinit var listenerInvitation: ValueEventListener
    private lateinit var authUser: FirebaseUser
    private lateinit var currentUser: Match
    private lateinit var currentOpponent: Match
    private lateinit var database: FirebaseDatabase
    private lateinit var dialogFind: Dialog
    private lateinit var dialogHandler: Handler
    private lateinit var dialogRunnable: Runnable
    private var fetching = true
    private var matching = false
    private var opponentAcceptMatch = false
    private var userAcceptMatch = false
    private var lastUidMatched = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

//      Inicia las variables globales
        database = Firebase.database
        authUser = Firebase.auth.currentUser!!
        lastUidMatched = authUser.uid
        setCurrentUser()
        setSearchingTex()
        dialogHandler = Handler(Looper.getMainLooper())
        findViewById<Button>(R.id.cancel_searching).setOnClickListener {
            finish()
        }

//      Comportamiento al recibir un mensaje de invitación, si es aceptado se va a la pantalla de
//      juego, si es cancelado regresa a buscar oponentes y muestra un mensaje, si es declinado
//      solo regresa a buscar oponentes
        onReceiveInvitation {
            when (it) {
                "accept" -> {
                    if (userAcceptMatch) goToGame()
                    else opponentAcceptMatch = true
                }
                "cancel" -> {
                    Toast.makeText(
                        this,
                        "The opponent canceled the match, you went back to search",
                        Toast.LENGTH_SHORT
                    ).show()
                    cancelGame()
                }
                "decline" -> {
                    cancelShowOpponent()
                }
                else -> {
                    if (currentUser.available) {
                        setAvailability(false)
                        showOpponent(it!!)
                    } else {
                        sendInvitation(it!!, "decline")
                    }
                }
            }
        }

//      Listener para invitar a todos los usuarios que vayan entrando
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

                    if (matchScore > 20) {
                        Invitation(currentUser.uid, matchScore).invite(opponentUid)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        startSearchingOpponent()

//      Espera n segundos a que lleguen las invitaciones de los que ya están en la lista
//      de match y selecciona el que tenga el puntaje más alto
        refFetching = database.getReference("matching/${authUser.uid}/invitations")
        listenerFetching = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChildren()) {
                    val opponentUid =
                        snapshot.children.elementAt(0).child("uid").getValue(String::class.java)!!
                    sendMessageOrDelete(opponentUid)
                } else {
                    fetching = false
                    refFetching.removeEventListener(listenerFetching)
                    setAvailability(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        Handler(Looper.getMainLooper()).postDelayed({
            refFetching.orderByChild("matchScore").limitToLast(1)
                .addValueEventListener(listenerFetching)
        }, 1000)
//         Posible bug: Si hay un usuario que mande su invitación después de 1 segundo va a volver
//         a hacer el proceso haciendo que se desplieguen dos dialogos
    }

    override fun onDestroy() {
        super.onDestroy()
        if (fetching) {
            refFetching.removeEventListener(listenerFetching)
        }
        if (matching) {
            sendInvitation(currentOpponent.uid, "decline")
        }
        stopSearchingOpponent()
        refInvitation.removeEventListener(listenerInvitation)
        removeInDB("matching/${authUser.uid}")
    }

    private fun setCurrentUser() {
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

    private fun setCurrentOpponent(uid: String) {
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
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendMessageOrDelete(uid: String) {
        ifUserIsAvailable(uid,
            {
                sendInvitation(uid, currentUser.uid)
                showOpponent(uid)
            },
            {
                removeInDB("matching/${authUser.uid}/invitations/$uid")
            })
    }

    private fun startSearchingOpponent() {
        refSearching.orderByChild("date").limitToLast(1)
            .addValueEventListener(listenerSearching)
    }

    private fun stopSearchingOpponent() {
        refSearching.removeEventListener(listenerSearching)
    }

    private fun addToMatch(match: Match) {
        match.create()
    }

    private fun showOpponent(uid: String) {
        matching = true
        setCurrentOpponent(uid)
        dialogRunnable = (Runnable { showDialog() })
        dialogHandler.postDelayed(dialogRunnable, 1000)
    }

    private fun cancelShowOpponent() {
        matching = false
        dialogHandler.removeCallbacks(dialogRunnable)
        setAvailability(true)
        if (fetching) {
            removeInDB("matching/${authUser.uid}/invitations/${currentOpponent.uid}")
        }
    }

    private fun showDialog() {
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

        val text = "username: ${currentOpponent.username}\nrank: ${currentOpponent.rank}"
        dialogFind.window?.findViewById<TextView>(R.id.user_found_text)!!.text = text

        dialogFind.window?.findViewById<Button>(R.id.cancel_game_button)!!.setOnClickListener {
            sendInvitation(currentOpponent.uid, "cancel")
            cancelGame()
        }

        dialogFind.window?.findViewById<Button>(R.id.accept_game_button)!!.setOnClickListener {
            userAcceptMatch = true
            sendInvitation(currentOpponent.uid, "accept")
            if (opponentAcceptMatch) goToGame()
        }

        dialogFind.show()
    }

    private fun onReceiveInvitation(funcion: (String?) -> Unit) {
        refInvitation = database.getReference("matching/${authUser.uid}/invitation")
        listenerInvitation = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val info = snapshot.getValue(String::class.java)
                if (info != null) {
                    removeInDB("matching/${authUser.uid}/invitation")
                    funcion(info)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        refInvitation.addValueEventListener(listenerInvitation)
    }

    private fun ifUserIsAvailable(
        uid: String,
        isAvailable: () -> Unit,
        isNotAvailable: () -> Unit
    ) {
        database.getReference("matching/$uid/available")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Boolean::class.java) == true) {
                        isAvailable()
                    } else {
                        isNotAvailable()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendInvitation(toUid: String, status: String) {
        saveInDB("matching/$toUid/invitation", status)
    }

    private fun saveInDB(path: String, value: Any) {
        database.getReference(path).setValue(value)
    }

    private fun removeInDB(path: String) {
        database.getReference(path).removeValue()
    }

    private fun setAvailability(availability: Boolean) {
        currentUser.available = availability
        saveInDB("matching/${currentUser.uid}/available", availability)
    }

    private fun cancelGame() {
        matching = false
        userAcceptMatch = false
        opponentAcceptMatch = false
        setAvailability(true)
        dialogFind.dismiss()
        if (fetching)
            removeInDB("matching/${authUser.uid}/invitations/${currentOpponent.uid}")
    }

    private fun setSearchingTex() {
        var dots = 0
        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                var text = "Searching"
                for (i in 1..dots) {
                    text += "."
                }
                dots = if (dots >= 3) 0 else dots + 1
                findViewById<TextView>(R.id.searching_text).text = text
                Handler(Looper.getMainLooper()).postDelayed(this, 400)
            }
        })
    }

    private fun goToGame() {
        val ids = listOf(currentOpponent.uid, currentUser.uid)
        val idsSorted = ids.sorted()
        val gameUid = idsSorted[0].substring(0, 13) + idsSorted[1].substring(0, 13)
        finish()
        val intent = Intent(this, SelectActivity::class.java)
        intent.putExtra("gameUid", gameUid)
        intent.putExtra("userUid", currentUser.uid)
        startActivity(intent)
    }
}