package io.github.rajumark.sextant.core

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.zip.ZipFile

class NestedArchivesTest {
    @Test
    fun `dex code inside jars in assets is listed class by class`() {
        val apk = CorpusApks.require("appmanager")
        val warnings = mutableListOf<String>()
        val files = ApkFiles.list(apk, emptyList(), warnings)
        val nested = ZipFile(apk.toFile()).use { NestedArchives.read(it, files, warnings) }
        val am = nested.classes.filter { it.container == "assets/am.jar" }
        am.size shouldBeGreaterThan 10
        am.map { it.descriptor } shouldContain "Lio/github/muntashirakon/AppManager/server/BroadcastSender;"
        am.all { it.entry == "classes.dex" } shouldBe true
        nested.entries.filter { it.archive == "assets/am.jar" }.map { it.magic } shouldContain "dex"
        warnings.filter { "nested archive" in it } shouldBe emptyList()
    }
}
