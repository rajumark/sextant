package io.github.rajumark.sextant.core.output

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions

/** Temp files are created owner-only (0600); output files should be readable like any normal file (0644). */
internal fun Path.makeReadable() {
    if (Files.getFileAttributeView(this, PosixFileAttributeView::class.java) != null) {
        Files.setPosixFilePermissions(this, PosixFilePermissions.fromString("rw-r--r--"))
    }
}
