package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.core.output.SextantJson
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class ManifestCoverageTest {
    @Test
    fun `every attribute used by the corpus manifests is structured or deliberately raw`() {
        val golden = CorpusApks.goldenDir
        if (!golden.isDirectory()) return
        val gaps =
            Files.list(golden).use { dirs ->
                dirs
                    .filter { Files.exists(it.resolve("manifest.json")) }
                    .toList()
                    .flatMap { dir ->
                        val root = SextantJson.pretty.decodeFromString(XmlNode.serializer(), Files.readString(dir.resolve("manifest.json")))
                        ManifestCoverage.unmapped(root).map { "${dir.name}: $it" }
                    }
            }
        gaps.shouldBeEmpty()
    }
}
