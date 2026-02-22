package com.example.quantummessenger

import android.content.Context
import java.io.File

object TorLoader {
    fun getTorExecutablePath(context: Context): String {
        val nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)

        // Ищем файл, содержащий "tor" и заканчивающийся на ".so"
        val sourceFile = nativeLibraryDir.listFiles()?.find { file ->
            file.name.contains("tor", ignoreCase = true) && file.name.endsWith(".so")
        }

        if (sourceFile != null && sourceFile.exists()) {
            return sourceFile.absolutePath
        } else {
            val filesInDir = nativeLibraryDir.listFiles()?.joinToString { it.name } ?: "Empty"
            throw Exception("Tor binary not found in $nativeLibraryDir.\nFiles: $filesInDir")
        }
    }
}