package com.example.quantummessenger

import net.freehaven.tor.control.TorControlConnection
import java.io.File
import java.net.Socket

object TorController {

    fun createHiddenService(dataDir: File): String {
        // 1. Ждем файл Cookie (подтверждение, что Tor жив)
        val cookieFile = File(dataDir, "control_auth_cookie")
        var attempts = 0

        // Ждем до 30 секунд появления файла
        while (!cookieFile.exists() && attempts < 60) {
            Thread.sleep(500)
            attempts++
        }

        if (!cookieFile.exists()) {
            throw Exception("Tor process started, but Cookie file not found. Check logs.")
        }

        // 2. ПОДКЛЮЧЕНИЕ С ПОВТОРАМИ (Решает ECONNREFUSED)
        var socket: Socket? = null
        var connectAttempts = 0

        // Пытаемся подключиться 20 раз с паузой в 1 секунду
        while (socket == null && connectAttempts < 20) {
            try {
                socket = Socket("127.0.0.1", 9051)
            } catch (e: Exception) {
                Thread.sleep(1000)
                connectAttempts++
            }
        }

        if (socket == null) throw Exception("Tor Control Port (9051) is not responding.")

        // 3. Работаем с библиотекой
        val conn = TorControlConnection(socket)
        conn.launchThread(true)

        val cookieBytes = cookieFile.readBytes()
        conn.authenticate(cookieBytes)

        // 4. Создаем сервис
        val responseMap = conn.addOnion(
            "NEW:BEST",
            mapOf(80 to "127.0.0.1:8080")
        )

        // Ищем адрес в ответе (ключи могут отличаться в разных версиях)
        val address = responseMap["onionAddress"] ?: responseMap["ServiceID"]

        if (address != null) {
            return "$address.onion"
        } else {
            throw Exception("Hidden Service created, but ID is null. Resp: $responseMap")
        }
    }
}