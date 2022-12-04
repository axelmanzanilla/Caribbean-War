package com.softparadox.caribbeanwar

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GameActivity : AppCompatActivity() {
    private lateinit var drag: ImageView
    private lateinit var uno: LinearLayout
    private lateinit var main: LinearLayout
    private lateinit var prueba: ImageView
    private lateinit var hello: TextView
    private lateinit var button: Button
    private var putX = 0f
    private var putY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        drag = findViewById(R.id.drag)
        uno = findViewById(R.id.uno)
        main = findViewById(R.id.main)
        prueba = findViewById(R.id.prueba)
        hello = findViewById(R.id.hello)
        button = findViewById(R.id.button)

        drag.setOnLongClickListener {
            val clipText = "hi"
            val item = ClipData.Item(clipText)
            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
            val data = ClipData(clipText, mimeTypes, item)
//            hello.text = "(${drag.rotation})"
//            val dragShadowBuilder = View.DragShadowBuilder(it -> {
//
//            })

            var rotationRad = Math.toRadians(it.rotation.toDouble())
            val dragShadowBuilder: View.DragShadowBuilder = object : View.DragShadowBuilder(it) {
                var s = abs(sin(rotationRad))
                var c = abs(cos(rotationRad))
                val width = (it.width * c + it.height * s).toFloat()
                val height = (it.width * s + it.height * c).toFloat()

                override fun onDrawShadow(canvas: Canvas) {
                    canvas.scale(it.scaleX, it.scaleY, width / 2, height / 2)
                    canvas.rotate(it.rotation, width / 2, height / 2)
                    canvas.translate((width - it.width) / 2, (height - it.height) / 2)
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

            it.startDragAndDrop(data, dragShadowBuilder, it, 0)
            it.visibility = View.INVISIBLE
            true
        }

        drag.setOnClickListener { v ->
//            val aux = v.layoutParams.width
//            v.layoutParams.width = v.layoutParams.height
//            v.layoutParams.height = aux
            v.rotation += 90f

        }

        uno.setOnDragListener(dragListener)
        main.setOnDragListener(dragListener)

        prueba.setImageResource(R.drawable.ship)

        button.setOnClickListener {
            drag.visibility = View.VISIBLE
//            prueba.x = 0f

            drag.x = 40f
            drag.y = 40f

            Glide.with(this).load(R.drawable.exp).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    (resource as GifDrawable).setLoopCount(1)
                    resource.registerAnimationCallback(object :
                        Animatable2Compat.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable?) {
                            prueba.setImageResource(R.drawable.ship)
                        }
                    })
                    return false
                }
            }).into(prueba)
        }
    }

    val dragListener = View.OnDragListener { view, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
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
                true
            }
            DragEvent.ACTION_DROP -> {
                val destination = view as LinearLayout
                if (destination.id == R.id.uno) {
                    val item = event.clipData.getItemAt(0)
                    val dragData = item.text
//                    Toast.makeText(this, dragData, Toast.LENGTH_LONG).show()
//                    hello.text = "(${event.x}, ${event.y})"
                    putX = event.x
                    putY = event.y

                    val v = event.localState as View
                    val owner = v.parent as ViewGroup
                    owner.removeView(v)
                    destination.addView(v)
//                    v.visibility = View.VISIBLE
                    true
                } else {

                    view.invalidate()
                    false
                }
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val v = event.localState as View
                v.visibility = View.VISIBLE
                v.x = putX - 40
                v.y = putY - 40
                view.invalidate()
                true
            }
            else -> {
                view.invalidate()
                false
            }
        }
    }
}