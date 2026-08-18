package dev.clxud.pawtap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/** Full-screen translucent overlay drawn by the accessibility service on any display. */
object PickerOverlay {
    private var current: Pair<WindowManager, View>? = null

    fun show(service: PawtapService, displayId: Int, onPick: (Float, Float) -> Unit): Boolean {
        dismiss()
        val display = service.getSystemService(DisplayManager::class.java).getDisplay(displayId) ?: return false
        return try {
            val ctx = service.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, null)
            val wm = ctx.getSystemService(WindowManager::class.java)
            val view = PickerView(ctx) { x, y -> dismiss(); onPick(x, y) }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            lp.fitInsetsTypes = 0
            wm.addView(view, lp)
            current = wm to view
            true
        } catch (e: Exception) {
            Log.e("Pawtap", "picker failed", e)
            false
        }
    }

    fun dismiss() {
        current?.let { (wm, v) -> try { wm.removeView(v) } catch (_: Exception) {} }
        current = null
    }

    private class PickerView(ctx: Context, val onPick: (Float, Float) -> Unit) : View(ctx) {
        val bg = Paint().apply { color = Color.argb(110, 255, 143, 171) }
        val txt = Paint().apply { color = Color.WHITE; textSize = 44f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        override fun onDraw(c: Canvas) {
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
            c.drawText("tap where the button should press", width / 2f, height / 2f, txt)
        }
        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_UP) onPick(e.rawX, e.rawY)
            return true
        }
    }
}
