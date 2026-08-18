package dev.clxud.pawtap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Checks GitHub Releases for a newer APK, downloads it, and hands it to the package installer. */
object Updater {
    private const val REPO = "cloudwithax/pawtap"
    private const val TAG = "PawtapUpdater"

    data class Release(val version: String, val apkUrl: String, val notes: String)

    /** Blocking; call off the main thread. Returns null if up to date or on any error. */
    fun check(): Release? = try {
        val c = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.connectTimeout = 8000; c.readTimeout = 8000
        val json = JSONObject(c.inputStream.bufferedReader().readText())
        val tag = json.getString("tag_name").removePrefix("v")
        val assets = json.getJSONArray("assets")
        var url: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.getString("name").endsWith(".apk", true)) { url = a.getString("browser_download_url"); break }
        }
        if (url != null && isNewer(tag, BuildConfig.VERSION_NAME)) Release(tag, url, json.optString("body"))
        else null
    } catch (e: Exception) { Log.w(TAG, "check failed: $e"); null }

    fun isNewer(latest: String, current: String): Boolean {
        val a = latest.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    /** Blocking download; returns the APK file or null. */
    fun download(ctx: Context, url: String, onProgress: (Int) -> Unit): File? = try {
        val dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "pawtap-update.apk")
        val c = URL(url).openConnection() as HttpURLConnection
        c.instanceFollowRedirects = true
        val total = c.contentLengthLong
        c.inputStream.use { inp -> file.outputStream().use { out ->
            val buf = ByteArray(64 * 1024); var n: Int; var read = 0L
            while (inp.read(buf).also { n = it } != -1) {
                out.write(buf, 0, n); read += n
                if (total > 0) onProgress((read * 100 / total).toInt())
            }
        } }
        file
    } catch (e: Exception) { Log.w(TAG, "download failed: $e"); null }

    fun canInstall(ctx: Context) = ctx.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(ctx: Context) =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))

    fun install(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        })
    }

    /** Relaunch after the package installer replaces us. */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                context.startActivity(Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }
    }
}
