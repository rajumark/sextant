package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.CorpusApks
import io.github.rajumark.sextant.core.SmaliPrinter
import io.github.rajumark.sextant.core.output.OutputWriter
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.sql.DriverManager

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReachTest {
    // Entry LMain;->start calls LBase;->work (abstract); LImpl extends LBase overrides work, which calls the target.
    private val conn =
        DriverManager.getConnection("jdbc:sqlite::memory:").apply {
            createStatement().use { st ->
                IndexSchema.statements(IndexSchema.DDL).forEach(st::execute)

                fun cls(
                    id: Int,
                    d: String,
                    sup: String,
                ) = st.execute(
                    "INSERT INTO classes VALUES ($id, '$d', '${d.drop(
                        1,
                    ).dropLast(1)}', '', '$sup', '[]', 'public', null, 'classes.dex', 0, '[]', 1, 0)",
                )
                cls(1, "LMain;", "Ljava/lang/Object;")
                cls(2, "LBase;", "Ljava/lang/Object;")
                cls(3, "LImpl;", "LBase;")
                val methods = listOf("LMain;->start()V", "LBase;->work()V", "LImpl;->work()V", "LNet;->send()V")
                methods.forEachIndexed { i, m ->
                    val defined = if (m.startsWith("LNet;")) 0 else 1
                    st.execute(
                        "INSERT INTO methods (id, descriptor, class, name, defined) VALUES (${i + 1}, '$m', '${m.substringBefore(
                            "->",
                        )}', 'x', $defined)",
                    )
                }
                st.execute("INSERT INTO calls VALUES (1, 2, 'virtual', 1, '[0]')") // Main.start → Base.work
                st.execute("INSERT INTO calls VALUES (3, 4, 'static', 1, '[0]')") // Impl.work → Net.send
            }
        }

    @Test
    fun `virtual calls reach overrides in subclasses`() {
        val r = ReachGraph(conn).search(listOf(1), reachTarget("LNet;->send"), maxDepth = 5, maxPaths = 5)
        r.paths
            .single()
            .steps
            .map { it.method to it.via } shouldBe
            listOf("LMain;->start()V" to null, "LImpl;->work()V" to "override", "LNet;->send()V" to "call")
    }

    @Test
    fun `depth limit stops the search`() {
        ReachGraph(conn).search(listOf(1), reachTarget("LNet;->send"), maxDepth = 1, maxPaths = 5).paths.size shouldBe 0
    }

    @Test
    fun `target specs`() {
        reachTarget("Cipher.getInstance")("Ljavax/crypto/Cipher;->getInstance(Ljava/lang/String;)Ljavax/crypto/Cipher;") shouldBe true
        reachTarget("okhttp3/")("Lokhttp3/OkHttpClient;->newCall(Lokhttp3/Request;)Lokhttp3/Call;") shouldBe true
        reachTarget("execute")("La;->execute()V") shouldBe true
        reachTarget("execute")("La;->executeLater()V") shouldBe false
    }

    private val dir = Files.createTempDirectory("sextant-reach")
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

    @Test
    fun `real APK - reach, field xref and smali agree`() {
        val camera = "de.markusfisch.android.binaryeye.activity.CameraActivity"
        val r = index.reach(camera, "Landroid/content/SharedPreferences\$Editor;->putBoolean", maxDepth = 5, maxPaths = 3)
        r.paths
            .first()
            .steps
            .first()
            .method shouldBe "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V"
        val xref = index.xref("Lde/markusfisch/android/binaryeye/activity/CameraActivity;->c1", 50)
        xref.map { it.relation to it.method } shouldContain
            (
                "write Lde/markusfisch/android/binaryeye/activity/CameraActivity;->c1:Z" to
                    "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V"
            )
        // calls.jsonl says getIntent is at code-unit offset 84 = 0x54.
        SmaliPrinter.print(
            CorpusApks.require("binaryeye"),
            "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V",
        ) shouldContain
            "#@54"
    }
}
