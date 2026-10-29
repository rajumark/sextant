package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.ProguardMapping
import io.github.rajumark.sextant.core.SmaliPrinter
import io.github.rajumark.sextant.core.SourceDecompiler

/** `sextant source <app.apk> <class|method>`: decompiled Java via jadx, on demand. */
class SourceCommand : CoreCliktCommand(name = "source") {
    private val apk by argument(help = "An APK or a .sqlite file from extract (its stored dex files are used)").path()
    private val target by argument(help = "Class (com.a.B, Lcom/a/B;) or method (Lcom/a/B;->m(I)V, Lcom/a/B;->m, com.a.B.m)")
    private val mapping by option(
        "--mapping",
        help = "ProGuard/R8 mapping.txt: show original class, method and field names; TARGET may then use the original name",
    ).path()

    override fun help(context: Context) =
        "Print decompiled Java (jadx) for one class or method. Names are kept as in the bytecode, matching the index, " +
            "unless --mapping gives the originals."

    override fun run() =
        guarded {
            val source = SourceDecompiler.decompile(apkOf(apk), target, mapping?.let { ProguardMapping.read(it) })
            System.err.println("// ${source.target}  (decompiled by jadx from ${source.topClass})")
            println(source.code)
        }
}

/** `sextant smali <app.apk> <class|method>`: bytecode assembly via baksmali. */
class SmaliCommand : CoreCliktCommand(name = "smali") {
    private val apk by argument(help = "An APK or a .sqlite file from extract (its stored dex files are used)").path()
    private val target by argument(help = "Class (com.a.B, Lcom/a/B;) or method (Lcom/a/B;->m(I)V, Lcom/a/B;->m)")

    override fun help(context: Context) =
        "Print smali (bytecode assembly) for one class or method. Works where decompiling fails; # 0x… offsets match the code files."

    override fun run() = guarded { print(SmaliPrinter.print(apkOf(apk), target)) }
}
