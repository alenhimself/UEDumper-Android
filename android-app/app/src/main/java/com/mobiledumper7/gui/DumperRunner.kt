package com.mobiledumper7.gui

import android.content.Context
import android.os.Build
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DumpRequest(
    val packageName: String,
    /** 1 = process_vm_readv, 2 = pread — mirrors the CLI --mem flag. */
    val memType: Int,
    val suspendGame: Boolean,
    val dumpSo: Boolean,
)

data class DumpResult(
    val success: Boolean,
    val zipPath: String?,
    val log: String,
)

/**
 * Runs the prebuilt MobileDumper-7 executable as root.
 *
 * Execution model (same as KernelSU Manager's use of libsu):
 * the matching binary from assets/dumper/<abi>/ is copied to the app's
 * private files dir, chmodded, then executed with a root shell:
 * `MobileDumper-7 -p <pkg> -o <out> [-s] [-d] -m <1|2>`.
 * Root is mandatory — reading another app's memory requires it.
 */
object DumperRunner {

    fun abiDir(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.startsWith("arm64") -> "arm64-v8a"
            abi.startsWith("x86_64") -> "x86_64"
            abi == "x86" -> "x86"
            else -> "armeabi-v7a"
        }
    }

    fun isRootAvailable(): Boolean =
        try {
            Shell.getShell().isRoot
        } catch (_: Exception) {
            false
        }

    suspend fun ensureBinary(context: Context, onLog: (String) -> Unit): File? =
        withContext(Dispatchers.IO) {
            val abi = abiDir()
            val out = File(context.filesDir, "dumper/$abi/MobileDumper-7")
            try {
                if (!out.exists()) {
                    out.parentFile?.mkdirs()
                    context.assets.open("dumper/$abi/MobileDumper-7").use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                    onLog("Installed dumper binary for $abi.")
                }
                out.setExecutable(true, true)
                Shell.cmd("chmod 755 '${out.absolutePath}'").exec()
                if (!out.canExecute()) onLog("Warning: binary is not executable (${out.absolutePath}).")
                out
            } catch (e: Exception) {
                onLog("Binary install failed: ${e.message}")
                null
            }
        }

    suspend fun runDump(
        context: Context,
        request: DumpRequest,
        onLog: (String) -> Unit,
    ): DumpResult = withContext(Dispatchers.IO) {
        val fullLog = StringBuilder()
        fun log(line: String) {
            fullLog.appendLine(line)
            onLog(line)
        }

        val pkg = request.packageName.trim()
        if (pkg.isEmpty()) {
            log("No target package selected.")
            return@withContext DumpResult(false, null, fullLog.toString())
        }

        val bin = ensureBinary(context, ::log)
            ?: return@withContext DumpResult(false, null, fullLog.toString())

        val dumpsRoot = File(
            context.getExternalFilesDir("dumps") ?: context.filesDir,
            "MobileDumper-7",
        ).apply { mkdirs() }

        val cmd = buildString {
            append("'${bin.absolutePath}'")
            append(" -p '").append(pkg).append('\'')
            append(" -o '").append(dumpsRoot.absolutePath).append('\'')
            if (request.dumpSo) append(" -d")
            if (request.suspendGame) append(" -s")
            append(" -m ").append(if (request.memType == 2) "2" else "1")
        }
        log("$ $cmd")
        log("Output dir: ${dumpsRoot.absolutePath}/MobileDumper-7")

        return@withContext try {
            val stdOut = mutableListOf<String>()
            val result = Shell.cmd(cmd).to(stdOut, stdOut).exec()
            stdOut.forEach(::log)
            val zip = findNewestZip(File(dumpsRoot, "MobileDumper-7"))
                ?: findNewestZip(dumpsRoot)
            val ok = result.isSuccess && zip != null
            if (ok) log("Dump finished: ${zip!!.absolutePath}")
            else log("Dump failed (exit=${result.code}). Check the log above.")
            DumpResult(ok, zip?.absolutePath, fullLog.toString())
        } catch (e: Exception) {
            log("Execution error: ${e.message}")
            DumpResult(false, null, fullLog.toString())
        }
    }

    private fun findNewestZip(dir: File): File? {
        if (!dir.isDirectory) return null
        return dir.walkTopDown()
            .filter { it.isFile && it.extension.equals("zip", ignoreCase = true) }
            .maxByOrNull { it.lastModified() }
    }
}
