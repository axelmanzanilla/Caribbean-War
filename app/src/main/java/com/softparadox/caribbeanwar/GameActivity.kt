package com.softparadox.caribbeanwar

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.allViews
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class GameActivity : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var refOpponentBoard: DatabaseReference
    private lateinit var refUserBoard: DatabaseReference
    private lateinit var refTurn: DatabaseReference
    private lateinit var listenerOpponentBoard: ValueEventListener
    private lateinit var listenerUserBoard: ValueEventListener
    private lateinit var listenerTurn: ValueEventListener
    private lateinit var textIndicator: TextView
    private var gameUid = ""
    private var userUid = ""
    private var opponentUid = ""
    private var yourTurn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        textIndicator = findViewById(R.id.status_indicator)

        database = Firebase.database
        gameUid = intent.getStringExtra("gameUid")!!
        userUid = intent.getStringExtra("userUid")!!
        opponentUid = intent.getStringExtra("opponentUid")!!

        refTurn = database.getReference("games/$gameUid/turn")
        listenerTurn = (object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val turn = snapshot.getValue(String::class.java)
                yourTurn = turn == userUid
                textIndicator.visibility = if (yourTurn) View.VISIBLE else View.INVISIBLE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        refTurn.addValueEventListener(listenerTurn)

        for (imageButton in findViewById<LinearLayout>(R.id.game_main).allViews) {
            imageButton.setOnClickListener { view ->
                sendPlay(view.tag.toString())
            }
        }

        listenOpponentsBoard()
        listenUsersBoard()
    }

    override fun onDestroy() {
        super.onDestroy()
        refOpponentBoard.removeEventListener(listenerOpponentBoard)
        refUserBoard.removeEventListener(listenerUserBoard)
        refTurn.removeEventListener(listenerTurn)
        removeInDB("games/$gameUid")
    }

    private fun sendPlay(square: String) {
        if (yourTurn) {
            launchMissileTo(square)
        } else {
            Toast.makeText(this, "Wait for your turn", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenOpponentsBoard() {
        refOpponentBoard = database.getReference("games/$gameUid/$opponentUid/board")
        listenerOpponentBoard = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (children in snapshot.children) {
                    findViewById<LinearLayout>(R.id.game_main).findViewWithTag<ImageButton>(children.key).background =
                        when (children.child("status").getValue(String::class.java)) {
                            "ship_top_hit",
                            "ship_middle_hit",
                            "ship_bottom_hit" -> getDrawable((R.drawable.ship_hit))
                            "water_hit" -> getDrawable((R.drawable.water_hit))
                            "destroyed" -> getDrawable((R.drawable.ship_sunken))
                            else -> getDrawable((R.drawable.water))
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        refOpponentBoard.addValueEventListener(listenerOpponentBoard)
    }

    private fun listenUsersBoard() {
        refUserBoard = database.getReference("games/$gameUid/$userUid/board")
        listenerUserBoard = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (children in snapshot.children) {
                    val imageButton =
                        findViewById<LinearLayout>(R.id.board_user).findViewWithTag<ImageButton>("user_${children.key}")
                    imageButton.background =
                        when (children.child("status").getValue(String::class.java)) {
                            "ship_top" -> getDrawable((R.drawable.ship_top))
                            "ship_middle" -> getDrawable((R.drawable.ship_middle))
                            "ship_bottom" -> getDrawable((R.drawable.ship_bottom))
                            "ship_top_hit" -> getDrawable((R.drawable.ship_top_burning))
                            "ship_middle_hit" -> getDrawable((R.drawable.ship_middle_burning))
                            "ship_bottom_hit" -> getDrawable((R.drawable.ship_bottom_burning))
                            "water_hit" -> getDrawable((R.drawable.water_hit))
                            else -> getDrawable((R.drawable.water))
                        }
                    imageButton.rotation =
                        children.child("rotation").getValue(Int::class.java)!!.toFloat()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        refUserBoard.addValueEventListener(listenerUserBoard)
    }

    private fun endTurn() {
        saveInDB("games/$gameUid/turn", opponentUid)
    }

    private fun launchMissileTo(key: String) {
        database.getReference("games/$gameUid/$opponentUid/board/$key")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val hasHit = snapshot.child("hit").getValue(Boolean::class.java)
                        if (hasHit == false) {
                            val id = snapshot.child("id").getValue(String::class.java)
                            val status = snapshot.child("status").getValue(String::class.java)!!
                            saveInDB(
                                "games/$gameUid/$opponentUid/board/$key/status",
                                "${status}_hit"
                            )
                            saveInDB("games/$gameUid/$opponentUid/board/$key/hit", true)
                            if (id == "water") endTurn()
                        } else {
                            Toast.makeText(
                                this@GameActivity,
                                "You already hit that square",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
            )
    }

    private fun saveInDB(path: String, value: Any) {
        database.getReference(path).setValue(value)
    }

    private fun removeInDB(path: String) {
        database.getReference(path).removeValue()
    }
}