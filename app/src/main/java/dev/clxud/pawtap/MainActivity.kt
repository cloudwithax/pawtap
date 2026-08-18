package dev.clxud.pawtap

import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {
    private lateinit var list: RecyclerView
    private lateinit var status: TextView
    private lateinit var empty: TextView
    private var mappings = mutableListOf<Mapping>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        list = findViewById(R.id.list)
        status = findViewById(R.id.status)
        empty = findViewById(R.id.empty)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = Adapter()

        findViewById<MaterialButton>(R.id.enableService).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<MaterialButton>(R.id.add).setOnClickListener {
            startActivity(Intent(this, EditMappingActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.update).setOnClickListener { doUpdate() }
        checkForUpdate()
        val sw = findViewById<MaterialSwitch>(R.id.enabled)
        sw.isChecked = Store.enabled(this)
        sw.setOnCheckedChangeListener { _, on -> Store.setEnabled(this, on) }
    }

    override fun onResume() {
        super.onResume()
        mappings = Store.load(this)
        list.adapter?.notifyDataSetChanged()
        empty.visibility = if (mappings.isEmpty()) View.VISIBLE else View.GONE
        val running = PawtapService.instance != null
        status.text = if (running) "🐾 Pawtap is awake and listening" else "😴 Pawtap is asleep — enable it in Accessibility settings"
        findViewById<View>(R.id.enableService).visibility = if (running) View.GONE else View.VISIBLE
    }

    private var pendingRelease: Updater.Release? = null

    private fun checkForUpdate() {
        Thread {
            val r = Updater.check() ?: return@Thread
            runOnUiThread {
                pendingRelease = r
                findViewById<MaterialButton>(R.id.update).apply {
                    text = "\u2B06 Update to v${r.version}"
                    visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun doUpdate() {
        val r = pendingRelease ?: return
        if (!Updater.canInstall(this)) {
            Toast.makeText(this, "Allow Pawtap to install updates, then tap again", Toast.LENGTH_LONG).show()
            startActivity(Updater.installPermissionIntent(this))
            return
        }
        val btn = findViewById<MaterialButton>(R.id.update)
        btn.isEnabled = false
        Thread {
            val f = Updater.download(this, r.apkUrl) { p -> runOnUiThread { btn.text = "Downloading\u2026 $p%" } }
            runOnUiThread {
                btn.isEnabled = true
                if (f == null) { btn.text = "Download failed \u2014 tap to retry"; return@runOnUiThread }
                btn.text = "\u2B06 Update to v${r.version}"
                Updater.install(this, f)
            }
        }.start()
    }

    private fun screenLabel(displayId: Int): String {
        val dm = getSystemService(DisplayManager::class.java)
        val ids = dm.displays.map { it.displayId }.sorted()
        return when (ids.indexOf(displayId)) {
            0 -> "Top screen"
            1 -> "Bottom screen"
            else -> "Display $displayId"
        }
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.title)
            val sub: TextView = v.findViewById(R.id.sub)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_mapping, p, false))
        override fun getItemCount() = mappings.size
        override fun onBindViewHolder(h: VH, i: Int) {
            val m = mappings[i]
            h.title.text = "${m.keyName()}  →  ${screenLabel(m.displayId)}"
            h.sub.text = "tap at (${m.x.toInt()}, ${m.y.toInt()})"
            h.itemView.setOnClickListener {
                startActivity(Intent(this@MainActivity, EditMappingActivity::class.java).putExtra("index", i))
            }
        }
    }
}
