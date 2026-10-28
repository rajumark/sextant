package io.github.rajumark.sextant.core

import jadx.api.JavaClass
import jadx.core.dex.nodes.MethodNode
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** One matching line of decompiled code. */
@Serializable
data class GrepHit(
    /** The top-level class, as a descriptor (`Lcom/a/B;`), using the bytecode name like the index. */
    val classDescriptor: String,
    /** The enclosing method as a descriptor (`Lcom/a/B;->m(I)V`), when the line is inside one. */
    val method: String?,
    /** 1-based line in the decompiled class, as jadx writes the file. */
    val line: Int,
    val text: String,
)

class GrepResult(
    val hits: List<GrepHit>,
    val classesSearched: Int,
    /** Classes jadx could not decompile at all; their code was not searched (`sextant smali` still shows it). */
    val failed: List<String>,
    /** True when the search stopped at the limit before every class was decompiled. */
    val truncated: Boolean,
)

/** Decompiled code of one top-level class: its lines and, for each line, the enclosing method (null outside one). */
private class ClassCode(
    val rawName: String,
    val lines: List<String>,
    val methods: List<String?>,
)

/**
 * Text search inside decompiled Java, like jadx-gui's code search. The first search decompiles every class with jadx
 * (in parallel, in jadx's own dependency batches) and keeps the code in a local cache keyed by the APK's hash (and
 * the mapping's), so later searches only read text. [packagePrefix] narrows the classes on a first, uncached run.
 */
object CodeGrep {
    private const val CODE_MARK = "#CODE"

    fun search(
        apk: Path,
        pattern: Regex,
        packagePrefix: String? = null,
        limit: Int = 200,
        mapping: ProguardMapping? = null,
        threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 8),
        cache: Boolean = true,
    ): GrepResult {
        val dir = if (cache) cacheDir(apk, mapping) else null
        val list = dir?.resolve("classes.txt")
        if (list != null && Files.isRegularFile(list)) return searchCache(dir, list, pattern, packagePrefix, limit)
        return SourceDecompiler.open(apk, mapping, threads).use { jadx ->
            val classes = jadx.classes.filter { c -> packagePrefix == null || matchesPrefix(c.rawName, c.fullName, packagePrefix) }
            val found = ConcurrentHashMap<String, List<GrepHit>>()
            val count = AtomicInteger()
            val stop = AtomicBoolean(false)
            val retry = ConcurrentHashMap.newKeySet<JavaClass>()

            fun visit(cls: JavaClass) {
                val code = dir?.let { read(it, cls.rawName) } ?: decompile(cls).also { c -> dir?.let { write(it, c) } }
                val hits = match(code, pattern)
                if (hits.isEmpty()) return
                found[cls.rawName] = hits
                if (count.addAndGet(hits.size) >= limit) stop.set(true)
            }
            val pool = Executors.newFixedThreadPool(threads)
            try {
                for (batch in jadx.decompileScheduler.buildBatches(classes)) {
                    pool.execute {
                        // Classes stay loaded: unloading lets another batch reload them concurrently, which jadx can't.
                        for (cls in batch) {
                            if (stop.get()) return@execute
                            runCatching { visit(cls) }.onFailure { retry += cls }
                        }
                    }
                }
            } finally {
                pool.shutdown()
                pool.awaitTermination(1, TimeUnit.DAYS)
            }
            // A class that failed in parallel gets one more try on its own; what still fails is reported, not hidden.
            val failed =
                retry.sortedBy { it.rawName }.filter { cls ->
                    if (stop.get()) return@filter false
                    runCatching { visit(cls) }.isFailure
                }
            // The cache is complete only after a full, unlimited pass over every class.
            if (dir != null && packagePrefix == null && !stop.get()) {
                Files.write(dir.resolve("failed.txt"), failed.map { it.rawName })
                val tmp = dir.resolve("classes.txt.tmp")
                Files.write(tmp, classes.map { "${it.rawName}\t${it.fullName}" })
                Files.move(tmp, dir.resolve("classes.txt"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }
            val sorted = found.keys.sorted().flatMap { found.getValue(it) }
            GrepResult(sorted.take(limit), classes.size, failed.map { it.rawName }, stop.get())
        }
    }

    private fun searchCache(
        dir: Path,
        list: Path,
        pattern: Regex,
        packagePrefix: String?,
        limit: Int,
    ): GrepResult {
        val names =
            Files
                .readAllLines(list)
                .map { it.substringBefore('\t') to it.substringAfter('\t') }
                .filter { (raw, full) -> packagePrefix == null || matchesPrefix(raw, full, packagePrefix) }
                .map { it.first }
                .sorted()
        val failedAll = dir.resolve("failed.txt").let { if (Files.isRegularFile(it)) Files.readAllLines(it).toSet() else emptySet() }
        val hits =
            names
                .parallelStream()
                .map { raw -> read(dir, raw)?.let { match(it, pattern) }.orEmpty() }
                .toList()
                .flatten()
        return GrepResult(hits.take(limit), names.size, names.filter { it in failedAll }, hits.size > limit)
    }

    private fun matchesPrefix(
        raw: String,
        full: String,
        prefix: String,
    ) = raw.startsWith(prefix) || full.startsWith(prefix)

    private fun cacheDir(
        apk: Path,
        mapping: ProguardMapping?,
    ): Path {
        val key = ApkReader.sha256(apk).take(24) + (mapping?.let { "-" + it.fingerprint } ?: "")
        return Bundles
            .cacheRoot()
            .resolve("code")
            .resolve(key)
            .also { Files.createDirectories(it.resolve("c")) }
    }

    // Hashed file names: obfuscated classes `a` and `A` would collide on a case-insensitive disk.
    private fun fileFor(
        dir: Path,
        raw: String,
    ): Path =
        dir.resolve("c").resolve(
            MessageDigest
                .getInstance("SHA-1")
                .digest(raw.toByteArray())
                .toHex()
                .take(24) + ".txt",
        )

    private fun decompile(cls: JavaClass): ClassCode {
        val info = cls.codeInfo
        val lines = info.codeStr.lines()
        val methods = ArrayList<String?>(lines.size)
        var offset = 0
        for (line in lines) {
            // The first non-blank character of a line: at the indentation the enclosing node is still the class.
            val at = offset + (line.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: 0)
            methods +=
                (info.codeMetadata.getNodeAt(at) as? MethodNode)?.let { m ->
                    "L" + m.parentClass.rawName.replace('.', '/') + ";->" + m.methodInfo.shortId
                }
            offset += line.length + 1
        }
        return ClassCode(cls.rawName, lines, methods)
    }

    /** Header lines `#M first last descriptor` give each method's line range; the code follows `#CODE`. */
    private fun write(
        dir: Path,
        c: ClassCode,
    ) {
        val out = StringBuilder()
        var i = 0
        while (i < c.methods.size) {
            val m = c.methods[i]
            var j = i
            while (j + 1 < c.methods.size && c.methods[j + 1] == m) j++
            if (m != null) {
                out
                    .append("#M ")
                    .append(i)
                    .append(' ')
                    .append(j)
                    .append(' ')
                    .append(m)
                    .append('\n')
            }
            i = j + 1
        }
        out.append(CODE_MARK).append('\n')
        c.lines.forEach { out.append(it).append('\n') }
        val file = fileFor(dir, c.rawName)
        val tmp = file.resolveSibling(file.fileName.toString() + ".tmp" + Thread.currentThread().id)
        Files.writeString(tmp, out)
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun read(
        dir: Path,
        raw: String,
    ): ClassCode? {
        val file = fileFor(dir, raw)
        if (!Files.isRegularFile(file)) return null
        val all = Files.readAllLines(file)
        val mark = all.indexOf(CODE_MARK)
        if (mark < 0) return null
        val lines = all.subList(mark + 1, all.size)
        val methods = arrayOfNulls<String>(lines.size)
        for (h in all.subList(0, mark)) {
            val parts = h.split(' ', limit = 4)
            for (k in parts[1].toInt()..parts[2].toInt()) if (k < methods.size) methods[k] = parts[3]
        }
        return ClassCode(raw, lines, methods.toList())
    }

    private fun match(
        c: ClassCode,
        pattern: Regex,
    ): List<GrepHit> {
        val descriptor = "L" + c.rawName.replace('.', '/') + ";"
        return c.lines.mapIndexedNotNull { i, line ->
            if (pattern.containsMatchIn(line)) GrepHit(descriptor, c.methods.getOrNull(i), i + 1, line.trim()) else null
        }
    }
}
