package io.github.cluno1.sonorus.features.scores.data

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class ScoreRevisionStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, "scores/gmusic_321")

    fun saveWorkingCopy(variant: BundledScoreVariant, bytes: ByteArray) {
        require(bytes.isNotEmpty())
        val target = workingFile(variant)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.outputStream().use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        runCatching {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrElse {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun workingFile(variant: BundledScoreVariant): File = File(
        root,
        "${variant.name.lowercase()}/working.musicxml"
    )
}
