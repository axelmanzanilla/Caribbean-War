package com.softparadox.caribbeanwar

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.truncate

class SelectActivity : AppCompatActivity() {
    private lateinit var shipSmall1: ImageView
    private lateinit var shipSmall2: ImageView
    private lateinit var shipMedium1: ImageView
    private lateinit var shipMedium2: ImageView
    private lateinit var shipLarge: ImageView
    private lateinit var board: RelativeLayout
    private lateinit var main: LinearLayout
    private var square = 0
    private var putX = 0f
    private var putY = 0f
    private var gameUid = ""
    private var userUid = ""
    private var ships = mutableListOf<Ship>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        shipSmall1 = findViewById(R.id.ship_small_1)
        shipSmall2 = findViewById(R.id.ship_small_2)
        shipMedium1 = findViewById(R.id.ship_medium_1)
        shipMedium2 = findViewById(R.id.ship_medium_2)
        shipLarge = findViewById(R.id.ship_large)
        board = findViewById(R.id.board)
        main = findViewById(R.id.main)
        val screenWidth = windowManager.defaultDisplay.width
        square = ((128f / 135f) * screenWidth / 8f).toInt()

        setBoardSize(square)
        setShips()
        gameUid = intent.getStringExtra("gameUid")!!
        userUid = intent.getStringExtra("userUid")!!

        findViewById<Button>(R.id.asd_button).setOnClickListener {
            if (ships.filter { s -> s.x == 0 && s.y == 0 }.isNotEmpty()) {
                Toast.makeText(this, "You must put all your ships", Toast.LENGTH_SHORT).show()
            } else {
                finish()
                startActivity(Intent(this, GameActivity::class.java))
                saveShips()
                saveWater()
            }
        }

        val longClickListener = View.OnLongClickListener { v ->
            val clipText = v.tag as CharSequence
            val item = ClipData.Item(clipText)
            val data = ClipData(clipText, arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)

            val rotationRad = Math.toRadians(v.rotation.toDouble())
            val dragShadowBuilder: View.DragShadowBuilder = object : View.DragShadowBuilder(v) {
                var s = abs(sin(rotationRad))
                var c = abs(cos(rotationRad))
                val width = (v.width * c + v.height * s).toFloat()
                val height = (v.width * s + v.height * c).toFloat()

                override fun onDrawShadow(canvas: Canvas) {
                    canvas.scale(v.scaleX, v.scaleY, width / 2, height / 2)
                    canvas.rotate(v.rotation, width / 2, height / 2)
                    canvas.translate((width - v.width) / 2, (height - v.height) / 2)
                    super.onDrawShadow(canvas)
                }

                override fun onProvideShadowMetrics(
                    shadowSize: Point,
                    shadowTouchPoint: Point
                ) {
                    shadowSize.set(width.toInt(), height.toInt())
                    shadowTouchPoint.set(shadowSize.x / 2, shadowSize.y / 2)
                }
            }
            v.startDragAndDrop(data, dragShadowBuilder, v, 0)
            v.visibility = View.INVISIBLE
            true
        }

        shipSmall1.setOnLongClickListener(longClickListener)
        shipSmall2.setOnLongClickListener(longClickListener)
        shipMedium1.setOnLongClickListener(longClickListener)
        shipMedium2.setOnLongClickListener(longClickListener)
        shipLarge.setOnLongClickListener(longClickListener)

        val clickListener = View.OnClickListener { v ->
            var auxX = v.x
            var auxY = v.y
            var auxR = v.rotation

            if (auxR == 0f) {
                auxR = 90f
                auxX += 0.5f * square
                auxY += 0.5f * square
                if (auxY > 6.5f * square) v.x = 6.5f * square
            } else {
                auxR = 0f
                auxX -= 0.5f * square
                auxY -= 0.5f * square
                if (auxY < -1 * square) auxY = -1f * square
            }

            val placeHolder = Ship()
            placeHolder.uid = ships.find { s -> s.uid == v.tag }!!.uid
            placeHolder.size = when (v.tag.toString().split("_")[1]) {
                "small" -> 2
                "medium" -> 3
                else -> 4
            }
            placeHolder.angle = auxR.toInt()
            placeHolder.x = getCoordX(auxX, placeHolder)
            placeHolder.y = getCoordY(auxY, placeHolder)

            if (canPut(placeHolder)) {
                val currentShip = ships.find { ship -> ship.uid == v.tag }!!
                currentShip.angle = placeHolder.angle
                currentShip.x = placeHolder.x
                currentShip.y = placeHolder.y
                v.rotation = auxR
                v.x = auxX
                v.y = auxY
            } else {
                Toast.makeText(this, "Not able to do that move", Toast.LENGTH_SHORT).show()
            }
        }

        shipSmall1.setOnClickListener(clickListener)
        shipSmall2.setOnClickListener(clickListener)
        shipMedium1.setOnClickListener(clickListener)
        shipMedium2.setOnClickListener(clickListener)
        shipLarge.setOnClickListener(clickListener)

        val dragListener = View.OnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    putX = event.x
                    putY = event.y
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    view.invalidate()
                    true
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    view.invalidate()
                    false
                }
                DragEvent.ACTION_DROP -> {
                    val destination = view as RelativeLayout
                    if (destination.id == R.id.board) {
                        putX = event.x
                        putY = event.y
                        val v = event.localState as View
                        val owner = v.parent as ViewGroup
                        owner.removeView(v)
                        destination.addView(v)
                        true
                    } else {
                        view.invalidate()
                        false
                    }
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    val v = event.localState as View
                    v.visibility = View.VISIBLE

                    val placeHolder = Ship()
                    placeHolder.uid = ships.find { s -> s.uid == v.tag }!!.uid
                    placeHolder.size = when (v.tag.toString().split("_")[1]) {
                        "small" -> 2
                        "medium" -> 3
                        else -> 4
                    }
                    placeHolder.angle = v.rotation.toInt()
                    val pixelX = getPixelsX(putX, placeHolder)
                    val pixelY = getPixelsY(putY, placeHolder)
                    placeHolder.x = getCoordX(pixelX, placeHolder)
                    placeHolder.y = getCoordY(pixelY, placeHolder)

                    if (canPut(placeHolder)) {
                        v.x = pixelX
                        v.y = pixelY
                        val index = ships.indexOfFirst { ship -> ship.uid == v.tag }
                        ships[index].x = placeHolder.x
                        ships[index].y = placeHolder.y
                        ships[index].angle = placeHolder.angle
                    }
                    view.invalidate()
                    true
                }
                else -> {
                    view.invalidate()
                    false
                }
            }
        }

        board.setOnDragListener(dragListener)
    }

    private fun setShips() {
        setShipsSize(square)
        ships.add(0, Ship("ship_small_1", 0, 0, 0, 2))
        ships.add(1, Ship("ship_small_2", 0, 0, 0, 2))
        ships.add(2, Ship("ship_medium_1", 0, 0, 0, 3))
        ships.add(3, Ship("ship_medium_2", 0, 0, 0, 3))
        ships.add(4, Ship("ship_large_1", 0, 0, 0, 4))
    }

    private fun getCoordX(x: Float, ship: Ship): Int {
        return when (ship.size) {
            2 -> if (ship.angle == 0) (x / square + 1).toInt() else (x / square + 1f).toInt()
            3 -> if (ship.angle == 0) (x / square + 1).toInt() else (x / square + 0.5f).toInt()
            4 -> if (ship.angle == 0) (x / square + 1).toInt() else (x / square).toInt()
            else -> 0
        }
    }

    private fun getCoordY(y: Float, ship: Ship): Int {
        return when (ship.size) {
            2 -> if (ship.angle == 0) (y / square + 2).toInt() else (y / square + 2.5f).toInt()
            3 -> if (ship.angle == 0) (y / square + 1.5).toInt() else (y / square + 2.5f).toInt()
            4 -> if (ship.angle == 0) (y / square + 1).toInt() else (y / square + 2.5f).toInt()
            else -> 0
        }
    }

    private fun getPixelsX(pxX: Float, ship: Ship): Float {
        return when (ship.size) {
            3 -> square * truncate(pxX / square)
            else -> {
                if (ship.angle == 0) square * truncate(pxX / square) else square * truncate((pxX - 64f) / square) + (0.5f * square)
            }
        }
    }

    private fun getPixelsY(pxY: Float, ship: Ship): Float {
        return when (ship.size) {
            3 -> if (ship.angle == 0) square * truncate((pxY) / square) - 1.5f * square else square * truncate(
                pxY / square
            ) - (1.5f * square)
            else -> {
                if (ship.angle == 0) square * truncate((pxY - 64f) / square) - square else square * truncate(
                    pxY / square
                ) - (1.5f * square)
            }
        }
    }

    private fun canPut(ship: Ship): Boolean {
        val isNotOverlapping = ships.filter { s -> s.uid != ship.uid }.filter { s ->
            (overlapping(
                s.x.toDouble(),
                s.x + (s.size - 1) * seno(s.angle),
                ship.x.toDouble(),
                ship.x + (ship.size - 1) * seno(ship.angle)
            )) && (overlapping(
                s.y.toDouble(),
                s.y + (s.size - 1) * coseno(s.angle),
                ship.y.toDouble(),
                ship.y + (ship.size - 1) * coseno(ship.angle)
            ))
        }.isEmpty()
        val isNotOutside = ship.x >= 1 &&
                ship.x + (ship.size - 1) * seno(ship.angle) <= 8 &&
                ship.y >= 1 &&
                ship.y + (ship.size - 1) * coseno(ship.angle) <= 8
        return isNotOverlapping && isNotOutside
    }

    private fun seno(angle: Int): Double {
        return sin(Math.toRadians(angle.toDouble()))
    }

    private fun coseno(angle: Int): Double {
        return cos(Math.toRadians(angle.toDouble()))
    }

    private fun overlapping(p1: Double, p2: Double, q1: Double, q2: Double): Boolean {
        return q1 in p1..p2 || q2 in p1..p2 || p1 in q1..q2 || p2 in q1..q2
    }

    private fun setBoardSize(size: Int) {
        val boardParams = board.layoutParams
        boardParams.width = 8 * size
        boardParams.height = 8 * size
        board.layoutParams = boardParams
    }

    private fun setShipsSize(size: Int) {
        val dragParams = shipSmall1.layoutParams
        dragParams.width = 1 * size
        dragParams.height = 2 * size
        shipSmall1.layoutParams = dragParams
        shipSmall2.layoutParams = dragParams
        dragParams.height = 3 * size
        shipMedium1.layoutParams = dragParams
        shipMedium2.layoutParams = dragParams
        dragParams.height = 4 * size
        shipLarge.layoutParams = dragParams
    }

    private fun saveShips() {
        for (ship in ships) {
            var i = 0
            while (i < ship.size) {
                var key = ""
                val row = ship.y + 64 + i * coseno(ship.angle)
                val column = ship.x + i * seno(ship.angle)
                val id = ship.uid
                val rotation = ship.angle.toString()
                val status = if (i == 0) {
                    if (rotation == "0") "ship_top" else "ship_bottom"
                } else if (i == ship.size - 1) {
                    if (rotation == "0") "ship_bottom" else "ship_top"
                } else {
                    "ship_middle"
                }
                key += row.toInt().toChar()
                key += column.toInt()
                i++
                Firebase.database.getReference("games/$gameUid/$userUid/$key")
                    .setValue(Frame(id, status, rotation))
            }
        }
    }

    private fun saveWater() {
        for (i in 65..72) {
            for (column in 1..8) {
                val key = "${i.toChar()}$column"
                val reference = Firebase.database.getReference("games/$gameUid/$userUid/$key")
                reference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            reference.setValue(Frame("water", "water", "0"))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }
}




