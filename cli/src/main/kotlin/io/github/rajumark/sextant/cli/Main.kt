package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import io.github.rajumark.sextant.core.ExitCode
import io.github.rajumark.sextant.core.Sextant
import io.github.rajumark.sextant.core.SextantException
import java.io.BufferedOutputStream
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

class SextantCommand : CoreCliktCommand(name = Sextant.NAME) {
    init {
        versionOption(Sextant.version, names = setOf("--version", "-V"))
    }

    override fun help(context: Context) = "Turn an Android APK into structured data for people and AI agents."

    override fun run() = Unit
}

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
            InsightsCommand(),
            EndpointsCommand(),
            McpCommand(),
            DiffCommand(),
            FindingsCommand(),
            XrefCommand(),
            ReachCommand(),
            SmaliCommand(),
            IconCommand(),
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
