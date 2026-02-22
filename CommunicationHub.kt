package com.example.quantummessenger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket

object CommunicationHub {

    // СЕРВЕР (СЛУШАЕМ 8080)
    suspend fun startServer(onMessageReceived: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val serverSocket = ServerSocket(8080)
                // Бесконечный цикл приема
                while (true) {
                    val clientSocket = serverSocket.accept()
                    // Даем клиенту 2 минуты на передачу данных (Tor медленный)
                    clientSocket.soTimeout = 120000

                    try {
                        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                        val message = reader.readLine()

                        if (message != null && message.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                onMessageReceived(message)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        clientSocket.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // КЛИЕНТ (ОТПРАВЛЯЕМ ЧЕРЕЗ SOCKS 9050)
    suspend fun sendMessage(onionAddress: String, text: String): String {
        return withContext(Dispatchers.IO) {
            var attempt = 1
            // Пробуем 3 раза, если сеть лагает
            val maxAttempts = 3
            var resultLog = ""

            while (attempt <= maxAttempts) {
                try {
                    // 1. Прокси SOCKS5 (Tor)
                    val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))
                    val socket = Socket(proxy)

                    // 2. Чистим адрес
                    val cleanHost = onionAddress
                        .replace("http://", "")
                        .replace("/", "")
                        .trim()

                    // 3. !!! КРИТИЧЕСКИ ВАЖНО !!!
                    // Иначе будет ошибка "Host not found" или "Time out"
                    // Мы говорим: "Не ищи IP этого адреса, отдай его Тору как есть"
                    val address = InetSocketAddress.createUnresolved(cleanHost, 80)

                    // 4. Соединение (Таймаут 2 минуты)
                    val timeoutMs = 120000
                    socket.connect(address, timeoutMs)

                    // 5. Отправка
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(text)

                    Thread.sleep(500) // Даем время байтам уйти
                    socket.close()

                    return@withContext "✅ Ушло (попытка $attempt)"

                } catch (e: Exception) {
                    resultLog = e.message ?: "Unknown"
                    attempt++
                    if (attempt <= maxAttempts) {
                        // Ждем перед новой попыткой
                        Thread.sleep(3000)
                    }
                }
            }
            return@withContext "❌ Ошибка после $maxAttempts попыток: $resultLog"
        }
    }
}