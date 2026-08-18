package dev.clxud.pawtap

import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.KeyEvent
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class EditMappingActivity : AppCompatActivity() {
    private var index = -1
    private var keyCode = -1
    private var listening = false
    private lateinit var keyLabel: TextView
    private lateinit var screen: Spinner
    private lateinit var xField: EditText
    private lateinit var yField: EditText
    private lateinit var displayIds: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        index = intent.getIntExtra("index", -1)
        keyLabel = findViewById(R.id.keyLabel)
        screen = findViewById(R.id.screen)
        xField = findViewById(R.id.x)
        yField = findViewById(R.id.y)

        val dm = getSystemService(DisplayManager::class.java)
        displayIds = dm.displays.map { it.displayId }.sorted()
        val names = displayIds.mapIndexed { i, id ->
            when (i) { 0 -> "Top screen"; 1 -> "Bottom screen"; else -> "Display $id" }
        }
        screen.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)

        val listen = findViewById<MaterialButton>(R.id.listen)
        listen.setOnClickListener {
            listening = true
            PawtapService.passthrough = true
            keyLabel.text = "Press a button now…"
        }
        findViewById<MaterialButton>(R.id.pick).setOnClickListener {
            val displayId = displayIds[screen.selectedItemPosition]
            val svc = PawtapService.instance
            if (svc == null) {
                Toast.makeText(this, "Wake up Pawtap in Accessibility settings first \uD83D\uDC3E", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val ok = PickerOverlay.show(svc, displayId) { x, y ->
                xField.setText(x.toInt().toString())
                yField.setText(y.toInt().toString())
            }
            if (!ok) Toast.makeText(this, "Couldn't open picker on that screen \u2014 type coordinates instead", Toast.LENGTH_LONG).show()
        }
        findViewById<MaterialButton>(R.id.save).setOnClickListener { save() }
        val delete = findViewById<MaterialButton>(R.id.delete)
        delete.setOnClickListener {
            val list = Store.load(this)
            if (index in list.indices) { list.removeAt(index); Store.save(this, list) }
            finish()
        }

        if (index >= 0) {
            val m = Store.load(this)[index]
            keyCode = m.keyCode
            keyLabel.text = m.keyName()
            screen.setSelection(displayIds.indexOf(m.displayId).coerceAtLeast(0))
            xField.setText(m.x.toInt().toString())
            yField.setText(m.y.toInt().toString())
        } else {
            delete.visibility = android.view.View.GONE
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (listening && event.action == KeyEvent.ACTION_DOWN) {
            keyCode = event.keyCode
            keyLabel.text = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            listening = false
            PawtapService.passthrough = false
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun save() {
        val x = xField.text.toString().toFloatOrNull()
        val y = yField.text.toString().toFloatOrNull()
        if (keyCode < 0) { Toast.makeText(this, "Pick a button first 🐾", Toast.LENGTH_SHORT).show(); return }
        if (x == null || y == null) { Toast.makeText(this, "Pick a spot on the screen 🐾", Toast.LENGTH_SHORT).show(); return }
        val m = Mapping(keyCode, displayIds[screen.selectedItemPosition], x, y)
        val list = Store.load(this)
        list.removeAll { it.keyCode == keyCode && (index < 0 || it != list.getOrNull(index)) }
        if (index in list.indices) list[index] = m else list.add(m)
        Store.save(this, list)
        finish()
    }

    override fun onPause() {
        super.onPause()
        listening = false
        PawtapService.passthrough = false
    }

    override fun onDestroy() {
        super.onDestroy()
        PickerOverlay.dismiss()
    }
}
