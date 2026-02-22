package com.example.quantummessenger

import android.content.Context
import java.io.File

data class TorRuntimeFiles(
    val executablePath: String,
    val geoIpFilePath: String,
    val geoIpV6FilePath: String
)

object TorLoader {
    fun prepareTorRuntime(context: Context): TorRuntimeFiles {
        val nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)
        val nativeFiles = nativeLibraryDir.listFiles()?.toList().orEmpty()

        val torSource = nativeFiles
            .firstOrNull { it.name.equals("libtor.so", ignoreCase = true) }
            ?: nativeFiles.firstOrNull {
                it.name.contains("tor", ignoreCase = true) &&
                    !it.name.contains("jtor", ignoreCase = true) &&
                    it.canRead()
            }
            ?: throw Exception("Tor binary not found in $nativeLibraryDir")

        val runtimeDir = File(context.filesDir, "tor_runtime").apply { mkdirs() }
        val executable = File(runtimeDir, "tor")
        torSource.copyTo(executable, overwrite = true)
        executable.setExecutable(true, true)

        if (!executable.canExecute()) {
            throw Exception("Tor binary is not executable: ${executable.absolutePath}")
        }

        val geoIp = copySupportFile(nativeFiles, runtimeDir, "geoip")
        val geoIpV6 = copySupportFile(nativeFiles, runtimeDir, "geoip6")

        return TorRuntimeFiles(
            executablePath = executable.absolutePath,
            geoIpFilePath = geoIp.absolutePath,
            geoIpV6FilePath = geoIpV6.absolutePath
        )
    }

    private fun copySupportFile(nativeFiles: List<File>, runtimeDir: File, name: String): File {
        val source = nativeFiles.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: throw Exception("$name not found next to Tor binary")

        return File(runtimeDir, name).also { source.copyTo(it, overwrite = true) }
    }
}
