package dev.clxud.pawtap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class PawtapService : AccessibilityService() {
    companion object {
        @Volatile var instance: PawtapService? = null
        /** When true (EditMappingActivity is listening for a button), don't swallow keys. */
        @Volatile var passthrough = false
    }

    private var mappings = emptyMap<Int, Mapping>()
    private var enabled = true
    private val held = HashMap<Int, StrokeDescription>()

    override fun onServiceConnected() {
        instance = this
        reload()
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun reload() {
        mappings = Store.load(this).associateBy { it.keyCode }
        enabled = Store.enabled(this)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!enabled || passthrough) return false
        val m = mappings[event.keyCode] ?: return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) press(m)
            KeyEvent.ACTION_UP -> release(m)
        }
        return true
    }

    private fun press(m: Mapping) {
        val path = Path().apply { moveTo(m.x, m.y) }
        // Long stroke marked willContinue so the finger stays down until the button is released.
        val stroke = StrokeDescription(path, 0, 60_000, true)
        held[m.keyCode] = stroke
        dispatch(stroke, m.displayId)
    }

    private fun release(m: Mapping) {
        val prev = held.remove(m.keyCode) ?: return
        val path = Path().apply { moveTo(m.x, m.y) }
        dispatch(prev.continueStroke(path, 0, 1, false), m.displayId)
    }

    private fun dispatch(stroke: StrokeDescription, displayId: Int) {
        val g = GestureDescription.Builder().addStroke(stroke).setDisplayId(displayId).build()
        dispatchGesture(g, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
