package dev.clxud.pawtap

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity

/** Full-screen translucent overlay: tap anywhere to pick the coordinate on this display. */
class PickPointActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContentView(object : View(this) {
            val bg = Paint().apply { color = Color.argb(90, 255, 143, 171) }
            val txt = Paint().apply { color = Color.WHITE; textSize = 48f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            override fun onDraw(c: Canvas) {
                c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
                c.drawText("🐾 tap where the button should press", width / 2f, height / 2f, txt)
            }
            override fun onTouchEvent(e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_UP) {
                    val loc = IntArray(2); getLocationOnScreen(loc)
                    setResult(RESULT_OK, Intent().putExtra("x", e.x + loc[0]).putExtra("y", e.y + loc[1]))
                    finish()
                }
                return true
            }
        })
    }
}
