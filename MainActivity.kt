package com.example.quantummessenger

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Base64
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val tvStatus = TextView(this).apply {
            text = "Нажмите ЗАПУСК"
            setTextColor(Color.GREEN)
            textSize = 16f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        val tvMyLink = TextView(this).apply {
            text = ""
            setTextColor(Color.YELLOW)
            setTextIsSelectable(true)
            textSize = 14f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 20, 0, 20)
        }

        val inputAddress = EditText(this).apply {
            hint = "Адрес друга (.onion)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        val inputMessage = EditText(this).apply {
            hint = "Сообщение..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        val btnSend = Button(this).apply {
            text = "ОТПРАВИТЬ"
            isEnabled = false
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        val tvLog = TextView(this).apply {
            text = "Лог чата:\n"
            setTextColor(Color.LTGRAY)
            textSize = 14f
        }
        scroll.addView(tvLog)

        val btnStart = Button(this).apply {
            text = "ЗАПУСТИТЬ СЕРВЕР"
        }

        rootLayout.addView(tvStatus)
        rootLayout.addView(tvMyLink)
        rootLayout.addView(btnStart)
        rootLayout.addView(inputAddress)
        rootLayout.addView(inputMessage)
        rootLayout.addView(btnSend)
        rootLayout.addView(scroll)

        setContentView(rootLayout)

        // --- ЗАПУСК ---
        btnStart.setOnClickListener {
            btnStart.isEnabled = false
            tvStatus.text = "Принудительная очистка..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // АВТО-ОЧИСТКА ПЕРЕД ЗАПУСКОМ
                    val configDir = getDir("tor_config", MODE_PRIVATE)
                    val dataDir = getDir("tor_data", MODE_PRIVATE)

                    // Удаляем старый cookie и lock, чтобы Tor не ругался
                    File(dataDir, "control_auth_cookie").delete()
                    File(dataDir, "lock").delete()

                    withContext(Dispatchers.Main) { tvStatus.text = "Старт ядра..." }

                    val torPath = TorLoader.getTorExecutablePath(applicationContext)
                    val cookieFile = File(dataDir, "control_auth_cookie")
                    val torrc = File(configDir, "torrc")
                    val torFile = File(torPath)

                    torrc.writeText(
                        "SocksPort 9050\n" +
                                "ControlPort 9051\n" +
                                "CookieAuthentication 1\n" +
                                "CookieAuthFile ${cookieFile.absolutePath}\n" +
                                "DataDirectory ${dataDir.absolutePath}\n" +
                                "GeoIPFile ${torFile.parent}/geoip\n" +
                                "GeoIPv6File ${torFile.parent}/geoip6\n"
                    )

                    val process = ProcessBuilder(torPath, "-f", torrc.absolutePath).start()

                    withContext(Dispatchers.Main) { tvStatus.text = "Инициализация (ждите)..." }

                    // Ждем пока Tor "прогреется"
                    Thread.sleep(3000)

                    if (process.isAlive) {
                        try {
                            val myAddress = TorController.createHiddenService(dataDir)

                            withContext(Dispatchers.Main) {
                                tvStatus.text = "✅ ACTIVE (v3)"
                                tvMyLink.text = myAddress
                                btnSend.isEnabled = true
                                btnStart.visibility = android.view.View.GONE
                            }

                            CommunicationHub.startServer { incomingText ->
                                tvLog.append("\n[ОНИ]: $incomingText")
                            }
                        } catch (e: Exception) {
                            process.destroy() // Если не удалось получить адрес, убиваем процесс
                            throw e
                        }
                    } else {
                        val errorMsg = process.errorStream.bufferedReader().use { it.readText() }
                        throw Exception("Tor упал: $errorMsg")
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "ОШИБКА: ${e.message}"
                        btnStart.isEnabled = true
                    }
                }
            }
        }

        // --- ОТПРАВКА ---
        btnSend.setOnClickListener {
            val target = inputAddress.text.toString()
            val msg = inputMessage.text.toString()

            if (target.isEmpty() || msg.isEmpty()) return@setOnClickListener

            tvLog.append("\n[Я]: $msg")
            tvLog.append("\n(Отправка... ждите 15-30 сек)")
            inputMessage.setText("")

            CoroutineScope(Dispatchers.IO).launch {
                val result = CommunicationHub.sendMessage(target, msg)
                withContext(Dispatchers.Main) {
                    tvLog.append("\n -> $result")
                }
            }
        }
    }
}