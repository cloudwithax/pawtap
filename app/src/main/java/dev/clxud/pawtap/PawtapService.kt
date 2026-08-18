package dev.clxud.pawtap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.util.SparseArray
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class PawtapService : AccessibilityService() {
    companion object {
        @Volatile var instance: PawtapService? = null
        /** When true (EditMappingActivity is listening for a button), don't swallow keys. */
        @Volatile var passthrough = false
        private const val HOLD_MS = 60_000L
    }

    private class Held(val m: Mapping, var stroke: StrokeDescription)

    private var mappings = SparseArray<Mapping>()
    private var enabled = true
    private val held = LinkedHashMap<Int, Held>()   // keyCode -> finger currently down

    override fun onServiceConnected() { instance = this; reload() }
    override fun onDestroy() { instance = null; super.onDestroy() }

    fun reload() {
        val sa = SparseArray<Mapping>()
        Store.load(this).forEach { sa.put(it.keyCode, it) }
        mappings = sa
        enabled = Store.enabled(this)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!enabled || passthrough) return false
        val m = mappings[event.keyCode] ?: return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) press(event.keyCode, m)
            KeyEvent.ACTION_UP -> release(event.keyCode)
        }
        return true
    }

    private fun path(m: Mapping) = Path().apply { moveTo(m.x, m.y) }

    private fun press(key: Int, m: Mapping) {
        held.remove(key)
        val h = Held(m, StrokeDescription(path(m), 0, HOLD_MS, true))
        held[key] = h
        sync(m.displayId, fresh = h, ending = null)
    }

    private fun release(key: Int) {
        val h = held.remove(key) ?: return
        sync(h.m.displayId, fresh = null, ending = h)
    }

    /**
     * Android cancels every in-flight gesture whenever a new one is dispatched, so each
     * press/release re-issues ALL fingers on that display in a single gesture: held ones as
     * continued strokes, plus the new one and/or the one being lifted.
     */
    private fun sync(displayId: Int, fresh: Held?, ending: Held?) {
        val b = GestureDescription.Builder().setDisplayId(displayId)
        for (h in held.values) {
            if (h.m.displayId != displayId) continue
            if (h !== fresh) h.stroke = h.stroke.continueStroke(path(h.m), 0, HOLD_MS, true)
            b.addStroke(h.stroke)
        }
        if (ending != null) b.addStroke(ending.stroke.continueStroke(path(ending.m), 0, 1, false))
        dispatchGesture(b.build(), object : GestureResultCallback() {
            override fun onCancelled(g: GestureDescription?) {
                // Someone else's touch (or another display's gesture) killed our fingers; forget them
                // so the next press starts a clean stroke instead of continuing a dead one.
                held.values.removeAll { it.m.displayId == displayId }
            }
        }, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
