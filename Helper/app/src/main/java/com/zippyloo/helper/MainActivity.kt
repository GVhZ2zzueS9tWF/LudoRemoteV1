package com.zippyloo.helper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Collections
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val port = 18765
    private val pin = (100000..999999).random().toString()
    @Volatile private var running = true
    @Volatile private var pairedToken: String? = null
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(24, 32, 24, 32) }
        box.addView(TextView(this).apply { text = "LUDO HELPER"; textSize = 26f; gravity = Gravity.CENTER })
        status = TextView(this).apply { textSize = 17f; gravity = Gravity.CENTER; text = "Starting local connection…" }
        box.addView(status)
        box.addView(Button(this).apply { text = "Enable Accessibility"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        box.addView(TextView(this).apply { text = "\nKeep this app open while using the controller. Both phones must be on the same Wi-Fi/Hotspot. Only pair with a device you trust."; textSize = 14f; gravity = Gravity.CENTER })
        setContentView(box)
        val ip = localIp()
        status.text = "Helper IP: ${ip ?: "Not found"}\nPort: $port\nPair PIN: $pin\n\nEnable Accessibility, then connect from Controller."
        startServer()
    }

    private fun startServer() = thread(name = "ludo-helper-server") {
        try {
            ServerSocket(port).use { server ->
                server.soTimeout = 1000
                while (running) {
                    try { val client = server.accept(); thread { handle(client) } }
                    catch (_: java.net.SocketTimeoutException) { }
                }
            }
        } catch (e: Exception) { runOnUiThread { status.text = "Server error: ${e.message}" } }
    }

    private fun handle(socket: java.net.Socket) {
        socket.use { s ->
            s.soTimeout = 4000
            val input = s.getInputStream().bufferedReader()
            val output = s.getOutputStream().bufferedWriter()
            val request = input.readLine() ?: return
            val parts = request.split(" ")
            var length = 0
            var auth = ""
            while (true) {
                val line = input.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toIntOrNull() ?: 0
                if (line.startsWith("Authorization:", true)) auth = line.substringAfter(':').trim().removePrefix("Bearer ")
            }
            val body = CharArray(length.coerceIn(0, 4096))
            var read = 0
            while (read < body.size) { val n = input.read(body, read, body.size - read); if (n <= 0) break; read += n }
            val json = try { JSONObject(String(body, 0, read)) } catch (_: Exception) { JSONObject() }
            var code = 200
            val response = when (parts.getOrNull(1)) {
                "/pair" -> {
                    if (json.optString("pin") == pin) {
                        pairedToken = UUID.randomUUID().toString().replace("-", "")
                        JSONObject().put("ok", true).put("token", pairedToken)
                    } else { code = 403; JSONObject().put("ok", false).put("error", "Wrong PIN") }
                }
                "/tap" -> {
                    if (pairedToken == null || auth != pairedToken) { code = 401; JSONObject().put("ok", false).put("error", "Pair first") }
                    else {
                        val x = json.optDouble("x", -1.0).toFloat(); val y = json.optDouble("y", -1.0).toFloat()
                        val sent = TapAccessibilityService.instance?.tapNormalized(x, y) ?: false
                        if (sent) JSONObject().put("ok", true) else { code = 400; JSONObject().put("ok", false).put("error", "Enable Accessibility or check coordinates") }
                    }
                }
                else -> { code = 404; JSONObject().put("ok", false).put("error", "Not found") }
            }
            val data = response.toString()
            output.write("HTTP/1.1 $code ${if (code == 200) "OK" else "Error"}\r\nContent-Type: application/json\r\nContent-Length: ${data.toByteArray().size}\r\nConnection: close\r\n\r\n$data")
            output.flush()
        }
    }

    private fun localIp(): String? = try {
        Collections.list(NetworkInterface.getNetworkInterfaces()).asSequence()
            .flatMap { Collections.list(it.inetAddresses).asSequence() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }?.hostAddress
    } catch (_: Exception) { null }

    override fun onDestroy() { running = false; super.onDestroy() }
}
