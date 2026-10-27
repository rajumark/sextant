package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.CorpusApks
import io.github.rajumark.sextant.core.SourceDecompiler
import io.github.rajumark.sextant.core.output.OutputWriter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallTreeTest {
    // Synthetic graph: a → b → c → a (cycle), a → d1..d5 (fan-out), b → ext (external).
    private val conn =
        DriverManager.getConnection("jdbc:sqlite::memory:").apply {
            createStatement().use { st ->
                IndexSchema.statements(IndexSchema.DDL).forEach(st::execute)
                val methods = listOf("a", "b", "c", "d1", "d2", "d3", "d4", "d5", "ext")
                methods.forEachIndexed { i, m ->
                    val defined = if (m == "ext") 0 else 1
                    st.execute(
                        "INSERT INTO methods (id, descriptor, class, name, defined) VALUES (${i + 1}, 'LX;->$m()V', 'LX;', '$m', $defined)",
                    )
                }

                fun id(m: String) = methods.indexOf(m) + 1
                val edges = listOf("a" to "b", "b" to "c", "c" to "a", "b" to "ext") + (1..5).map { "a" to "d$it" }
                edges.forEach { (from, to) -> st.execute("INSERT INTO calls VALUES (${id(from)}, ${id(to)}, 'virtual', 1, '[0]')") }
            }
        }

    private val dir = Files.createTempDirectory("sextant-calltree")
    private val index by lazy {
        OutputWriter.extractTo(CorpusApks.require("binaryeye"), dir, force = false)
        IndexReader.open(dir)
    }

    @AfterAll
    fun cleanup() {
        conn.close()
        runCatching { index.close() }
        dir.toFile().deleteRecursively()
    }

    private fun tree(
        root: Long,
        direction: Direction,
        depth: Int = 5,
        fanOut: Int = 10,
    ): CallNode {
        val builder = CallTreeBuilder(conn, direction, depth, fanOut)
        return builder.build(root).also { builder.close() }
    }

    @Test
    fun `cycles are marked and not expanded again`() {
        val c =
            tree(1, Direction.CALLEES)
                .children
                .first { it.method == "LX;->b()V" }
                .children
                .first { it.method == "LX;->c()V" }
        c.children.single().method shouldBe "LX;->a()V"
        c.children.single().cycle shouldBe true
        c.children.single().children shouldHaveSize 0
    }

    @Test
    fun `fan-out limit reports how many were left out`() {
        val root = tree(1, Direction.CALLEES, fanOut = 3)
        root.children shouldHaveSize 3
        root.more shouldBe 3
    }

    @Test
    fun `external methods are leaves in callee trees`() {
        val ext = tree(2, Direction.CALLEES).children.first { it.method == "LX;->ext()V" }
        ext.defined shouldBe false
        ext.children shouldHaveSize 0
    }

    @Test
    fun `depth limit stops the walk`() {
        tree(1, Direction.CALLERS, depth = 1).children.map { it.method } shouldContainExactly listOf("LX;->c()V")
        tree(1, Direction.CALLERS, depth = 1).children.single().children shouldHaveSize 0
    }

    @Test
    fun `text rendering uses tree branches and markers`() {
        val text = CallTreeText.render(tree(2, Direction.CALLEES))
        text shouldContain "├── "
        text shouldContain "(external)"
        text shouldContain "(cycle)"
    }

    @Test
    fun `method queries resolve on a real APK`() {
        index.resolveMethods("CameraActivity.onResume").map { it.second } shouldContainExactly
            listOf("Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V")
        index.resolveMethods("Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume").size shouldBe 1
        (index.resolveMethods("onResume").size > 1) shouldBe true
    }

    @Test
    fun `callers of a framework method on a real APK`() {
        val getIntent = index.resolveMethods("Landroid/app/Activity;->getIntent()Landroid/content/Intent;").single().first
        val tree = index.callTree(getIntent, Direction.CALLERS, depth = 1, fanOut = 50)
        tree.defined shouldBe false
        tree.children.map { it.method to it.count } shouldContain
            ("Lde/markusfisch/android/binaryeye/activity/PickActivity;->onCreate(Landroid/os/Bundle;)V" to 6)
    }

    @Test
    fun `source decompiles one method of a real APK`() {
        val src =
            SourceDecompiler.decompile(
                CorpusApks.require("binaryeye"),
                "de.markusfisch.android.binaryeye.activity.CameraActivity.onResume",
            )
        src.code shouldContain "welcome_shown"
        src.target shouldBe "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume"
    }
}
