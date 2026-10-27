package io.github.rajumark.sextant.core

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition
import com.android.tools.smali.baksmali.Adaptors.MethodDefinition
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.formatter.DexFormatter
import java.io.StringWriter
import java.nio.file.Path

/**
 * Smali (bytecode assembly) for one class or method, via baksmali. Works where decompilation fails, and the
 * `# 0x…` offsets are the same code-unit offsets used in calls.jsonl, strings.jsonl and field_refs.jsonl.
 */
object SmaliPrinter {
    fun print(
        apk: Path,
        target: String,
    ): String {
        val (classDescriptor, methodPart) = SourceDecompiler.splitTarget(target)
        val warnings = mutableListOf<String>()
        val def =
            CodeExtractor
                .loadDexFiles(apk, warnings)
                .firstNotNullOfOrNull { (_, dex) -> dex.classes.firstOrNull { it.type == classDescriptor } }
                ?: throw SextantException.input(
                    "class $classDescriptor not found in $apk",
                    "check the name with `sextant find <dir> <name> --kind class`",
                )
        val options =
            BaksmaliOptions().apply {
                codeOffsets = true
                debugInfo = true
            }
        val out = StringWriter()
        val writer = BaksmaliWriter(out)
        val classDefinition = ClassDefinition(options, def)
        if (methodPart == null) {
            classDefinition.writeTo(writer)
        } else {
            val methods =
                def.methods.filter { m ->
                    val id = DexFormatter.INSTANCE.getMethodDescriptor(m).substringAfter("->")
                    if ('(' in methodPart) id == methodPart else m.name == methodPart
                }
            if (methods.isEmpty()) throw SextantException.input("method $methodPart not found in $classDescriptor")
            methods.forEachIndexed { i, m ->
                if (i > 0) writer.write("\n")
                val impl = m.implementation
                if (impl ==
                    null
                ) {
                    MethodDefinition.writeEmptyMethodTo(writer, m, classDefinition)
                } else {
                    MethodDefinition(classDefinition, m, impl).writeTo(writer)
                }
            }
        }
        writer.flush()
        return out.toString()
    }
}
