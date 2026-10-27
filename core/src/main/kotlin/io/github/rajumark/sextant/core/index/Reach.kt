package io.github.rajumark.sextant.core.index

import kotlinx.serialization.Serializable
import java.sql.Connection

/** One call path from an entry point to a target. */
@Serializable
data class ReachPath(
    val steps: List<ReachStep>,
)

@Serializable
data class ReachStep(
    val method: String,
    /** How this step was reached from the previous one: `call`, or `override` (class-hierarchy dispatch); null for the entry. */
    val via: String?,
)

@Serializable
data class ReachResult(
    val entries: List<String>,
    val paths: List<ReachPath>,
    val visited: Int,
    val truncated: Boolean,
)

/**
 * Breadth-first search over the static call graph with class-hierarchy analysis (CHA): a call to a method of class C
 * also reaches the same-signature methods defined in subclasses/implementors of C, which is how virtual and interface
 * calls dispatch at runtime. Signatures with more than 25 implementations (Runnable.run, Function1.invoke, …) are not
 * expanded, because that connects unrelated code. Still an approximation: reflection, callbacks registered with the
 * framework, and native code are not followed.
 */
internal class ReachGraph(
    conn: Connection,
) {
    private val descriptor: Array<String>
    private val ownerOf: IntArray
    private val defined: BooleanArray
    private val classIndex = HashMap<String, Int>()
    private val classNames = ArrayList<String>()
    private val out: Array<IntArray>
    private val implementations = HashMap<String, MutableList<Int>>() // "name(sig)ret" → defined methods with it
    private val supertypes = HashMap<Int, Set<Int>>()
    private val directSupers = HashMap<Int, List<Int>>()

    init {
        val count =
            conn.createStatement().use { st ->
                st.executeQuery("SELECT max(id) FROM methods").use {
                    it.next()
                    it.getInt(1)
                }
            }
        descriptor = Array(count + 1) { "" }
        ownerOf = IntArray(count + 1)
        defined = BooleanArray(count + 1)
        conn.createStatement().use { st ->
            st.executeQuery("SELECT id, descriptor, class, defined FROM methods").use { rs ->
                while (rs.next()) {
                    val id = rs.getInt(1)
                    descriptor[id] = rs.getString(2)
                    ownerOf[id] = classId(rs.getString(3))
                    defined[id] = rs.getInt(4) == 1
                    if (defined[id]) implementations.getOrPut(descriptor[id].substringAfter("->")) { mutableListOf() } += id
                }
            }
        }
        val edges = Array(count + 1) { IntArrayBuilder() }
        conn.createStatement().use { st ->
            st.executeQuery("SELECT caller, callee FROM calls").use { rs -> while (rs.next()) edges[rs.getInt(1)].add(rs.getInt(2)) }
        }
        out = Array(count + 1) { edges[it].toArray() }
        conn.createStatement().use { st ->
            st.executeQuery("SELECT descriptor, superclass, interfaces FROM classes").use { rs ->
                while (rs.next()) {
                    val supers = mutableListOf<Int>()
                    rs.getString(2)?.let { supers += classId(it) }
                    INTERFACE.findAll(rs.getString(3)).forEach { supers += classId(it.groupValues[1]) }
                    directSupers[classId(rs.getString(1))] = supers
                }
            }
        }
    }

    private fun classId(name: String) = classIndex.getOrPut(name) { classNames.add(name).let { classNames.size - 1 } }

    private fun supertypesOf(cls: Int): Set<Int> =
        supertypes.getOrPut(cls) {
            val result = HashSet<Int>()
            val stack = ArrayDeque(directSupers[cls].orEmpty())
            while (stack.isNotEmpty()) {
                val s = stack.removeLast()
                if (result.add(s)) stack.addAll(directSupers[s].orEmpty())
            }
            result
        }

    /** Targets of a call to [callee]: the callee itself plus overriding implementations in subtypes. */
    private fun dispatch(callee: Int): List<Pair<Int, String>> {
        val result = mutableListOf(callee to "call")
        val key = descriptor[callee].substringAfter("->")
        if (key.startsWith("<init>") || key.startsWith("<clinit>")) return result
        val owner = ownerOf[callee]
        val candidates = implementations[key].orEmpty()
        // Very generic signatures (Runnable.run, Function1.invoke, …) have thousands of implementations: expanding them
        // connects unrelated code, so they are not followed.
        if (candidates.size > MAX_IMPLEMENTATIONS) return result
        for (impl in candidates) {
            if (impl != callee && owner in supertypesOf(ownerOf[impl])) result += impl to "override"
        }
        return result
    }

    fun entriesOfClass(classDescriptor: String): List<Int> {
        val cls = classIndex[classDescriptor] ?: return emptyList()
        return descriptor.indices.filter { defined[it] && ownerOf[it] == cls }
    }

    fun search(
        entries: List<Int>,
        isTarget: (String) -> Boolean,
        maxDepth: Int,
        maxPaths: Int,
    ): ReachResult {
        val parent = HashMap<Int, Pair<Int, String>>()
        val depth = HashMap<Int, Int>()
        val queue = ArrayDeque<Int>()
        for (e in entries) {
            depth[e] = 0
            queue.add(e)
        }
        val hits = mutableListOf<Int>()
        var truncated = false
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val d = depth.getValue(node)
            if (d > 0 && isTarget(descriptor[node])) {
                hits += node
                if (hits.size >= maxPaths) {
                    truncated = queue.isNotEmpty()
                    break
                }
                continue // a path ends at its first target
            }
            if (d >= maxDepth || !defined[node]) continue
            for (callee in out[node]) {
                for ((next, via) in dispatch(callee)) {
                    if (next in depth) continue
                    depth[next] = d + 1
                    parent[next] = node to via
                    queue.add(next)
                }
            }
        }
        val paths =
            hits.map { hit ->
                val steps = ArrayDeque<ReachStep>()
                var cur = hit
                while (true) {
                    val p = parent[cur]
                    steps.addFirst(ReachStep(descriptor[cur], p?.second))
                    cur = p?.first ?: break
                }
                ReachPath(steps.toList())
            }
        return ReachResult(entries.map { descriptor[it] }, paths, depth.size, truncated)
    }

    /** Ids of methods matching a descriptor list (from [IndexReader.resolveMethods]). */
    fun idsOf(descriptors: List<String>): List<Int> {
        val wanted = descriptors.toHashSet()
        return descriptor.indices.filter { descriptor[it] in wanted }
    }

    private class IntArrayBuilder {
        private var data = IntArray(0)
        private var size = 0

        fun add(v: Int) {
            if (size == data.size) data = data.copyOf(maxOf(4, size * 2))
            data[size++] = v
        }

        fun toArray(): IntArray = data.copyOf(size)
    }

    private companion object {
        val INTERFACE = Regex("\"(L[^\"]+;)\"")
        const val MAX_IMPLEMENTATIONS = 25
    }
}

/** Builds a target predicate: `Ljavax/crypto/Cipher;->getInstance` (prefix), `Cipher.getInstance`, `okhttp3/` or a bare method name. */
fun reachTarget(spec: String): (String) -> Boolean =
    when {
        spec.startsWith("L") -> { d -> d.startsWith(spec) }
        '.' in spec -> {
            val owner = spec.substringBeforeLast('.').replace('.', '/')
            val name = spec.substringAfterLast('.')
            val needle = "$owner;->$name("
            ({ d -> d.contains("/$needle") || d.startsWith("L$needle") })
        }
        '/' in spec -> { d -> d.contains(spec) }
        else -> { d -> d.contains("->$spec(") }
    }
