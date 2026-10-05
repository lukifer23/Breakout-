package com.breakoutplus.game

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Same-directory replace after fsync: readers see either the previous or next checkpoint. */
class SnapshotFileStore(private val directory: File) {
    @Synchronized fun write(name: String, value: String) {
        require(name.matches(Regex("[a-z_]+\\.json")))
        check(directory.isDirectory || directory.mkdirs())
        val target = File(directory, name)
        val temporary = File(directory, "$name.new")
        FileOutputStream(temporary).use { stream ->
            stream.write(value.toByteArray(Charsets.UTF_8)); stream.fd.sync()
        }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
    @Synchronized fun read(name: String): String? = File(directory, name).takeIf { it.isFile }?.readText()
    @Synchronized fun delete(name: String) { Files.deleteIfExists(File(directory, name).toPath()) }
    @Synchronized fun quarantine(name: String) {
        val file = File(directory, name)
        if (file.isFile) Files.move(file.toPath(), File(directory, "$name.corrupt").toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
