package com.example.quantummessenger // <--- ВАЖНО: Пакет исправлен

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security

object QuantumCore {
    init {
        // Чтобы не было ошибок при повторном запуске
        Security.removeProvider("BC")
        Security.removeProvider("BCPQC")
        Security.addProvider(BouncyCastleProvider())
        Security.addProvider(BouncyCastlePQCProvider())
    }

    fun generateKeys(): KeyPair {
        val generator = KeyPairGenerator.getInstance("KYBER1024", "BCPQC")
        return generator.generateKeyPair()
    }
}