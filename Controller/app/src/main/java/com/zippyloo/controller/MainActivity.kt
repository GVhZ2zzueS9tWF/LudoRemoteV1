package com.zippyloo.controller

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var ip: EditText
    private lateinit var pin: EditText
    private lateinit var x: EditText
    private lateinit var y: EditText
    private lateinit var status: TextView
    @Volatile private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(20, 24, 20, 24) }
        root.addView(TextView(this).apply { text = "LUDO CONTROLLER"; textSize = 25f; gravity = Gravity.CENTER })
        ip = field("Helper IP address (example: 192.168.1.10)")
        pin = field("6-digit Pair PIN")
        root.addView(ip); root.addView(pin)
        root.addView(Button(this).apply { text = "CONNECT / PAIR"; setOnClickListener { pair() } })
        status = TextView(this).apply { text = "Not connected"; gravity = Gravity.CENTER; textSize = 15f }
        root.addView(status)
        root.addView(TextView(this).apply { text = "Manual screen tap (0–1000 scale)"; textSize = 16f; gravity = Gravity.CENTER })
        val coords = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        x = field("X 0–1000"); y = field("Y 0–1000"); coords.addView(x, LinearLayout.LayoutParams(0, -2, 1f)); coords.addView(y, LinearLayout.LayoutParams(0, -2, 1f)); root.addView(coords)
        root.addView(Button(this).apply { text = "SEND TAP"; setOnClickListener { val xx = x.text.toString().toFloatOrNull(); val yy = y.text.toString().toFloatOrNull(); if (xx == null || yy == null || xx !in 0f..1000f || yy !in 0f..1000f) status.text = "Enter X/Y from 0 to 1000" else sendTap(xx / 1000f, yy / 1000f) } })
        root.addView(TextView(this).apply { text = "\nConnect both phones to the same Wi-Fi/Hotspot. Enter the Helper IP and PIN shown on the Helper phone. Coordinates must be calibrated manually."; textSize = 13f; gravity = Gravity.CENTER })
        setContentView(root)
    }

    private fun field(hintText: String) = EditText(this).apply { hint = hintText; singleLine = true; textSize = 15f }

    private fun pair() {
        val host = ip.text.toString().trim(); val code = pin.text.toString().trim()
        if (host.isBlank() || code.length != 6) { status.text = "Enter Helper IP and 6-digit PIN"; return }
        thread {
            try {
                val result = request(host, "/pair", JSONObject().put("pin", code), null)
                token = result.getString("token")
                runOnUiThread { status.text = "Connected and paired" }
            } catch (e: Exception) { runOnUiThread { status.text = "Connection failed: ${e.message}" } }
        }
    }

    private fun sendTap(nx: Float, ny: Float) {
        val auth = token
        if (auth == null) { status.text = "Pair first"; return }
        val host = ip.text.toString().trim()
        thread {
            try { request(host, "/tap", JSONObject().put("x", nx).put("y", ny), auth); runOnUiThread { status.text = "Tap command sent" } }
            catch (e: Exception) { runOnUiThread { status.text = "Tap failed: ${e.message}" } }
        }
    }

    private fun request(host: String, path: String, body: JSONObject, auth: String?): JSONObject {
        val conn = (URL("http://$host:18765$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 4000; readTimeout = 4000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (auth != null) setRequestProperty("Authorization", "Bearer $auth")
        }
        return try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            val result = JSONObject(text)
            if (conn.responseCode !in 200..299) throw IllegalStateException(result.optString("error", "HTTP ${conn.responseCode}"))
            result
        } finally { conn.disconnect() }
    }
}
