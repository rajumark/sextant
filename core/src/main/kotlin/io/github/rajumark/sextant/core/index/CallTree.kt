package io.github.rajumark.sextant.core.index

import kotlinx.serialization.Serializable
import java.sql.Connection

/** One node of a caller or callee tree. */
@Serializable
data class CallNode(
    val method: String,
    /** Call sites between this node and its parent (0 for the root). */
    val count: Int,
    /** False for external methods (framework, other libraries): they have no body to follow. */
    val defined: Boolean,
    /** True when this method already appears higher up the same path; its children are not repeated. */
    val cycle: Boolean,
    val children: List<CallNode>,
    /** Children left out because of the fan-out limit. */
    val more: Int,
)

enum class Direction { CALLERS, CALLEES }

/**
 * Walks the call graph in index.sqlite. Edges are static: calls through an interface or superclass point at
 * that interface/superclass method, so callers of an override are found by asking about the base method too.
 */
internal class CallTreeBuilder(
    private val conn: Connection,
    private val direction: Direction,
    private val maxDepth: Int,
    private val fanOut: Int,
) {
    private val edgeSql =
        when (direction) {
            Direction.CALLERS ->
                "SELECT m.id, m.descriptor, m.defined, c.count FROM calls c JOIN methods m ON m.id = c.caller " +
                    "WHERE c.callee = ? ORDER BY c.count DESC, m.descriptor"
            Direction.CALLEES ->
                "SELECT m.id, m.descriptor, m.defined, c.count FROM calls c JOIN methods m ON m.id = c.callee " +
                    "WHERE c.caller = ? ORDER BY c.count DESC, m.descriptor"
        }
    private val edges = conn.prepareStatement(edgeSql)

    private data class Edge(
        val id: Long,
        val descriptor: String,
        val defined: Boolean,
        val count: Int,
    )

    fun build(root: Long): CallNode {
        val (descriptor, defined) =
            conn.prepareStatement("SELECT descriptor, defined FROM methods WHERE id = ?").use { st ->
                st.setLong(1, root)
                st.executeQuery().use { rs ->
                    rs.next()
                    rs.getString(1) to (rs.getInt(2) == 1)
                }
            }
        return node(root, descriptor, defined, 0, depth = 0, path = setOf(root))
    }

    private fun node(
        id: Long,
        descriptor: String,
        defined: Boolean,
        count: Int,
        depth: Int,
        path: Set<Long>,
    ): CallNode {
        // Callees of an external method are unknown; callers of anything can always be looked up.
        val expandable = depth < maxDepth && (direction == Direction.CALLERS || defined)
        if (!expandable) return CallNode(descriptor, count, defined, cycle = false, children = emptyList(), more = 0)
        val all = edgesOf(id)
        val shown = all.take(fanOut)
        val children =
            shown.map { e ->
                if (e.id in path) {
                    CallNode(e.descriptor, e.count, e.defined, cycle = true, children = emptyList(), more = 0)
                } else {
                    node(e.id, e.descriptor, e.defined, e.count, depth + 1, path + e.id)
                }
            }
        return CallNode(descriptor, count, defined, cycle = false, children = children, more = all.size - shown.size)
    }

    private fun edgesOf(id: Long): List<Edge> {
        edges.setLong(1, id)
        return edges.executeQuery().use { rs ->
            val out = mutableListOf<Edge>()
            while (rs.next()) out += Edge(rs.getLong(1), rs.getString(2), rs.getInt(3) == 1, rs.getInt(4))
            out
        }
    }

    fun close() = edges.close()
}

/** Renders a tree with box-drawing characters, one method per line. */
object CallTreeText {
    fun render(root: CallNode): String =
        buildString {
            appendLine(root.method + if (!root.defined) "  (external)" else "")
            children(root, "")
        }

    private fun StringBuilder.children(
        node: CallNode,
        prefix: String,
    ) {
        val total = node.children.size + if (node.more > 0) 1 else 0
        node.children.forEachIndexed { i, child ->
            val last = i == total - 1
            append(prefix)
                .append(if (last) "└── " else "├── ")
                .append(child.method)
                .append("  ×")
                .append(child.count)
            if (!child.defined) append("  (external)")
            if (child.cycle) append("  (cycle)")
            appendLine()
            children(child, prefix + if (last) "    " else "│   ")
        }
        if (node.more > 0) append(prefix).append("└── … ").append(node.more).appendLine(" more (raise the fan-out limit)")
    }
}
