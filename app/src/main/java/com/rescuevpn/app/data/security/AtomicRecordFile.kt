package com.rescuevpn.app.data.security

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** Minimal atomic encrypted-record file abstraction; implementations store ciphertext only. */
internal interface AtomicRecordFile {
    fun read(): ByteArray?
    fun write(contents: ByteArray)
    fun delete()
}

internal fun interface AtomicRecordFileFactory {
    fun forProfile(profileId: String): AtomicRecordFile
}

/**
 * Android AtomicFile adapter. AtomicFile's backup/restore protocol protects
 * the previous complete ciphertext if a write is interrupted.
 */
internal class AndroidAtomicRecordFileFactory(
    private val directory: File
) : AtomicRecordFileFactory {

    override fun forProfile(profileId: String): AtomicRecordFile {
        require(profileId.isNotBlank() && profileId.length <= MAX_PROFILE_ID_CHARS &&
            profileId.none { it.isISOControl() }) { "Invalid profile ID." }
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create the private metadata directory.")
        }
        if (!directory.isDirectory) throw IOException("Metadata path is not a directory.")

        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(profileId.toByteArray(Charsets.UTF_8))
        val filename = digest.joinToString("") { "%02x".format(it) } + ".rma"
        return AndroidAtomicRecordFile(AtomicFile(File(directory, filename)))
    }

    private companion object {
        const val MAX_PROFILE_ID_CHARS = 128
    }
}

private class AndroidAtomicRecordFile(
    private val atomicFile: AtomicFile
) : AtomicRecordFile {

    override fun read(): ByteArray? {
        if (!atomicFile.exists()) return null
        return try {
            atomicFile.readFully()
        } catch (error: FileNotFoundException) {
            null
        }
    }

    override fun write(contents: ByteArray) {
        var stream: java.io.FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(contents)
            atomicFile.finishWrite(stream)
            stream = null
        } catch (error: Exception) {
            if (stream != null) atomicFile.failWrite(stream)
            throw IOException("Atomic encrypted metadata write failed.", error)
        }
    }

    override fun delete() {
        atomicFile.delete()
    }
}
