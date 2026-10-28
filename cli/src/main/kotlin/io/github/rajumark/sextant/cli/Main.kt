package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.Sextant
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.IndexCache
import io.github.rajumark.sextant.core.index.IndexReader
import java.io.BufferedOutputStream
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

class SextantCommand : CoreCliktCommand(name = Sextant.NAME) {
    init {
        versionOption(Sextant.version, names = setOf("--version", "-V"))
    }

    override fun help(context: Context) = "Turn an Android APK into structured data for people and AI agents."

    override fun run() = Unit
}

/** Opens a .sqlite file or an APK (indexed once, then read from the cache); a build is announced on stderr. */
internal fun openIndex(path: Path): IndexReader = IndexReader.open(path) { System.err.println(it) }

/** Help text for commands that take any of those. */
internal const val INDEX_INPUT = "An APK (indexed once, then cached) or a .sqlite file from sextant extract"

/** The whole extraction from an APK (through the cache) or a .sqlite file. */
internal fun loadExtraction(path: Path): Extraction = openIndex(path).use { it.extraction() }

/** A .sqlite file for [path]: itself, or the cached index of an APK (built once, announced on stderr). */
internal fun indexFile(path: Path): Path =
    if (Files.isRegularFile(path) && !path.toString().endsWith(".sqlite")) IndexCache.index(path) { System.err.println(it) } else path

/** Runs [block], turning expected failures into a clean message and the documented exit code. */
internal fun <T> guarded(block: () -> T): T =
    try {
        block()
    } catch (e: SextantException) {
        val message =
            buildString {
                append("Error: ").append(e.message)
                e.hint?.let { append("\nhint: ").append(it) }
            }
        throw CliktError(message, statusCode = e.exitCode.code)
    }

/**
 * stdout and stderr are always UTF-8. Java otherwise uses the console code page on Windows (often cp1252/cp437),
 * which turns tree lines, arrows and non-Latin strings into `?` and makes --json output invalid for consumers.
 */
private fun utf8Streams() {
    System.setOut(PrintStream(BufferedOutputStream(FileOutputStream(FileDescriptor.out), 1 shl 13), true, Charsets.UTF_8))
    System.setErr(PrintStream(BufferedOutputStream(FileOutputStream(FileDescriptor.err), 1 shl 10), true, Charsets.UTF_8))
}

fun main(args: Array<String>) {
    utf8Streams()
    val command =
        SextantCommand().subcommands(
            ExtractCommand(),
            InfoCommand(),
            SqlCommand(),
            FindCommand(),
            ClassCommand(),
            CallersCommand(),
            CalleesCommand(),
            SourceCommand(),
            GrepCommand(),
            InsightsCommand(),
            EndpointsCommand(),
            McpCommand(),
            DiffCommand(),
            FindingsCommand(),
            XrefCommand(),
            ReachCommand(),
            SmaliCommand(),
            IconCommand(),
            ComponentsCommand(),
            ComponentCommand(),
            AppCommand(),
            ResourceCommand(),
            SdkCommand(),
            DeobfCommand(),
            FilesCommand(),
            CoverageCommand(),
            CacheCommand(),
            XmlCommand(),
        )
    try {
        command.parse(args)
    } catch (e: CliktError) {
        // clikt-core does not exit on its own: print the message and exit with the documented code.
        command.getFormattedHelp(e)?.let { if (e.printError) System.err.println(it) else println(it) }
        exitProcess(e.statusCode)
    } catch (e: Throwable) {
        System.err.println("internal error: ${e.javaClass.name}: ${e.message}")
        System.err.println("This is a bug in sextant ${Sextant.version}. Please report it with the APK's sha256.")
        if (System.getenv("SEXTANT_DEBUG") != null) e.printStackTrace() else System.err.println("(set SEXTANT_DEBUG=1 for a stack trace)")
        exitProcess(ExitCode.INTERNAL.code)
    }
}
