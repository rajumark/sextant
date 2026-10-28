package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.types.restrictTo
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.index.CallNode
import io.github.rajumark.sextant.core.index.CallTreeText
import io.github.rajumark.sextant.core.index.Direction
import io.github.rajumark.sextant.core.index.ReachResult
import io.github.rajumark.sextant.core.index.XrefRow
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.builtins.ListSerializer

/** Shared implementation of `callers` and `callees`. */
abstract class CallTreeCommand(
    name: String,
    private val direction: Direction,
) : CoreCliktCommand(name = name) {
    private val dir by argument(help = INDEX_INPUT).path()
    private val method by argument(help = "Method: Lcom/a/B;->m(I)V, Lcom/a/B;->m, com.a.B.m, B.m or just m")
    private val depth by option("--depth", "-d", help = "How many levels to follow (default 3)").int().restrictTo(1..20).default(3)
    private val fanOut by option("--fan-out", help = "Maximum children per method (default 15)").int().restrictTo(1..1000).default(15)
    private val json by option("--json", help = "The tree as JSON (one array of root trees)").flag()

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                val matches = index.resolveMethods(method)
                if (matches.isEmpty()) {
                    throw SextantException.input("no method matches '$method'", "try `sextant find $dir $method --kind method`")
                }
                // Overloads of one method (same class and name) are shown together; anything else must be narrowed down.
                val groups = matches.groupBy { it.second.substringBefore('(') }
                if (groups.size > 1) {
                    System.err.println("${groups.size} methods match '$method'; pass one of these:")
                    groups.keys.forEach { println(it) }
                    return@use
                }
                val trees = matches.map { (id, _) -> index.callTree(id, direction, depth, fanOut) }
                if (json) {
                    println(SextantJson.compact.encodeToString(ListSerializer(CallNode.serializer()), trees))
                } else {
                    print(trees.joinToString("\n") { CallTreeText.render(it) })
                }
            }
        }
}

class CallersCommand : CallTreeCommand("callers", Direction.CALLERS) {
    override fun help(context: Context) =
        "Show who calls a method, as a tree. Calls through an interface or superclass are recorded against that base method."
}

class CalleesCommand : CallTreeCommand("callees", Direction.CALLEES) {
    override fun help(context: Context) = "Show what a method calls, as a tree. External (framework/library) methods are leaves."
}

/** `sextant xref <dir> <class|field>`: where a class or field is used. */
class XrefCommand : CoreCliktCommand(name = "xref") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val target by argument(help = "Class (com.a.B, Lcom/a/B;, B) or field (Lcom/a/B;->f:I, Lcom/a/B;->f, com.a.B.f)")
    private val limit by option("--limit", help = "Maximum rows (default 200)").int().default(200)
    private val json by option("--json", help = "One JSON object per row").flag()

    override fun help(context: Context) =
        "Where is a class or field used: instantiations, casts, instanceof, const-class, calls into its methods, field reads and writes."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                val rows = index.xref(target, limit)
                if (json) {
                    rows.forEach { println(SextantJson.compact.encodeToString(XrefRow.serializer(), it)) }
                } else {
                    rows.groupBy { it.relation }.forEach { (relation, group) ->
                        println(relation)
                        group.forEach { println("    ${it.method}  ×${it.count}") }
                    }
                }
                System.err.println(
                    "${rows.size} reference${if (rows.size == 1) "" else "s"}${if (rows.size == limit) " (stopped at --limit $limit)" else ""}",
                )
            }
        }
}

/** `sextant reach <dir> <from> <to>`: call paths from an entry point to target methods. */
class ReachCommand : CoreCliktCommand(name = "reach") {
    private val dir by argument(help = INDEX_INPUT).path()
    private val from by argument(help = "Entry point: a component/class (com.a.LoginActivity) or a method")
    private val to by argument(help = "Target: Ljavax/crypto/Cipher;->getInstance, Cipher.getInstance, okhttp3/ or a method name")
    private val depth by option("--depth", help = "Maximum call depth (default 10)").int().restrictTo(1..50).default(10)
    private val paths by option("--paths", help = "Maximum paths to report (default 5)").int().restrictTo(1..100).default(5)
    private val json by option("--json", help = "The result as JSON").flag()

    override fun help(context: Context) =
        "Find call paths from an entry point (e.g. an exported activity) to target methods, following virtual and interface " +
            "dispatch into subclasses. Static approximation: reflection, framework callbacks and native code are not followed."

    override fun run() =
        guarded {
            openIndex(dir).use { index ->
                val result = index.reach(from, to, depth, paths)
                if (json) {
                    println(SextantJson.compact.encodeToString(ReachResult.serializer(), result))
                    return@use
                }
                if (result.paths.isEmpty()) {
                    println(
                        "No path found from ${result.entries.size} entry method(s) within depth $depth (${result.visited} methods visited).",
                    )
                }
                result.paths.forEachIndexed { i, path ->
                    println("Path ${i + 1} (${path.steps.size - 1} calls)")
                    path.steps.forEachIndexed { k, step ->
                        val via = if (step.via == "override") "  (via override)" else ""
                        println("  ".repeat(k + 1) + (if (k == 0) "" else "→ ") + step.method + via)
                    }
                }
                System.err.println(
                    "${result.paths.size} path(s) · ${result.entries.size} entry methods · ${result.visited} methods visited",
                )
            }
        }
}
