package io.github.rajumark.sextant.desktop

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

val isMac: Boolean = System.getProperty("os.name").startsWith("Mac")

/** "⌘" on macOS, "Ctrl+" elsewhere; used in shortcut hints. */
val ShortcutPrefix: String = if (isMac) "⌘" else "Ctrl+"

/** Pointer shown over the draggable pane divider. */
val ResizeHorizontalPointer: PointerIcon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))

/** Puts plain text on the system clipboard. */
fun copyToClipboard(text: String) {
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
}

/** Native "open" dialog filtered to APKs and split bundles (.apks, .xapk, .apkm). Returns null when the user cancels. */
fun chooseApkFile(owner: java.awt.Frame?): java.nio.file.Path? {
    val dialog = java.awt.FileDialog(owner, "Open APK", java.awt.FileDialog.LOAD)
    // Start in Downloads, where most APKs land; fall back to the home folder if it does not exist.
    dialog.directory = downloadsDir().toString()
    dialog.setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in setOf("apk", "apks", "xapk", "apkm") }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return java.nio.file.Path
        .of(dialog.directory, file)
}

/** Native folder picker for an existing `.sextant` output folder. */
fun chooseOutputFolder(owner: java.awt.Frame?): java.nio.file.Path? {
    if (isMac) System.setProperty("apple.awt.fileDialogForDirectories", "true")
    try {
        if (isMac) {
            val dialog = java.awt.FileDialog(owner, "Open output folder", java.awt.FileDialog.LOAD)
            dialog.isVisible = true
            val file = dialog.file ?: return null
            return java.nio.file.Path
                .of(dialog.directory, file)
        }
        val chooser =
            javax.swing.JFileChooser(downloadsDir().toFile()).apply {
                fileSelectionMode =
                    javax.swing.JFileChooser.DIRECTORIES_ONLY
            }
        return if (chooser.showOpenDialog(owner) == javax.swing.JFileChooser.APPROVE_OPTION) chooser.selectedFile.toPath() else null
    } finally {
        if (isMac) System.clearProperty("apple.awt.fileDialogForDirectories")
    }
}

/** Opens [dir] in Finder / Explorer / the file manager. */
fun revealInFileManager(dir: java.nio.file.Path) {
    runCatching {
        java.awt.Desktop
            .getDesktop()
            .open(dir.toFile())
    }
}

/** The user's Downloads folder, or the home folder when Downloads does not exist. */
fun downloadsDir(): java.nio.file.Path {
    val home =
        java.nio.file.Path
            .of(System.getProperty("user.home"))
    return home.resolve("Downloads").takeIf {
        java.nio.file.Files
            .isDirectory(it)
    } ?: home
}

/**
 * Opens a web address in the user's default browser. The app makes no request itself; the browser does, and only
 * after the user clicks. Returns false when the system cannot open links.
 */
fun openInBrowser(url: String): Boolean =
    runCatching {
        if (!java.awt.Desktop.isDesktopSupported() ||
            !java.awt.Desktop
                .getDesktop()
                .isSupported(java.awt.Desktop.Action.BROWSE)
        ) {
            return@runCatching false
        }
        java.awt.Desktop
            .getDesktop()
            .browse(java.net.URI(url))
        true
    }.getOrDefault(false)

/** A query string that is safe to put in a URL (spaces become %20). */
fun urlQuery(text: String): String =
    java.net.URLEncoder
        .encode(text, Charsets.UTF_8)
        .replace("+", "%20")

/** Asks where to save a file, then writes [bytes] there. Returns the path written, or null when the user cancels. */
fun saveWithDialog(
    suggestedName: String,
    bytes: ByteArray,
): java.nio.file.Path? {
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Save icon", java.awt.FileDialog.SAVE)
    dialog.directory = downloadsDir().toString()
    dialog.file = suggestedName
    dialog.isVisible = true
    val name = dialog.file ?: return null
    val target =
        java.nio.file.Path
            .of(dialog.directory, name)
    java.nio.file.Files
        .write(target, bytes)
    return target
}
