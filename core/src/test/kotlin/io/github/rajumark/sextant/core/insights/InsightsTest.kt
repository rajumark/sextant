package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.CorpusApks
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.model.LibraryRecord
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

class ElfAndUrlTest {
    @Test
    fun `JNI names decode back to Java, including escapes`() {
        Elf.jni("Java_com_example_Foo_bar") shouldBe ("com.example.Foo" to "bar")
        Elf.jni("Java_com_example_My_1Class_do_1it") shouldBe ("com.example.My_Class" to "do_it")
        Elf.jni("Java_com_badlogic_gdx_graphics_g2d_freetype_FreeType_00024Bitmap_getBuffer") shouldBe
            ("com.badlogic.gdx.graphics.g2d.freetype.FreeType\$Bitmap" to "getBuffer")
        Elf.jni("Java_com_example_Foo_bar__ILjava_lang_String_2") shouldBe ("com.example.Foo" to "bar")
        Elf.jni("JNI_OnLoad") shouldBe null
    }

    @Test
    fun `garbage is not an ELF file`() {
        Elf.read(ByteArray(100) { it.toByte() }) shouldBe null
        Elf.read(ByteArray(0)) shouldBe null
    }

    @Test
    fun `URLs are found in text, with plausible hosts only`() {
        Urls.find("see https://api.example.com/v1/users?id=1, or http://localhost:8080.") shouldBe
            listOf("https://api.example.com/v1/users?id=1", "http://localhost:8080")
        Urls.find("format: https://%s/path") shouldBe emptyList()
        Urls.host("https://user@Api.Example.com:8443/x") shouldBe "api.example.com"
    }

    @Test
    fun `URLs in binary data are reduced to scheme and dotted host`() {
        val packed = "https://bsky.app/start/:name/:rkeyboardPaddingetExperiment\u0000http://b/24559754\u0000wss://chat.example.org/socket"
        Urls.findInBinary(packed) shouldBe listOf("https://bsky.app", "wss://chat.example.org")
    }
}

/** Facts about real corpus APKs, checked by hand. */
class InsightsTest {
    private fun insights(id: String) = Extractor.extract(CorpusApks.require(id)).insights!!

    @Test
    fun `frameworks across the corpus`() {
        insights("bluesky").stats.frameworks shouldContainAll listOf("Expo", "Hermes (JS engine)", "React Native")
        insights("bitwarden").stats.frameworks shouldContain ".NET (Xamarin / MAUI)"
        insights("fluffychat").stats.frameworks shouldBe listOf("Flutter")
        insights("shatteredpd").stats.frameworks shouldBe listOf("libGDX")
        insights("godot").stats.frameworks shouldContain "Godot"
        insights("tuxpaint").stats.frameworks shouldContain "SDL"
        insights("logseq").stats.frameworks shouldContain "Capacitor"
        insights("signal").stats.frameworks shouldNotContain "React Native"
    }

    @Test
    fun `exact library versions and build tool from META-INF`() {
        val signal = insights("signal")
        signal.libraries shouldContain LibraryRecord("androidx.core", "core", "1.18.0", "META-INF/androidx.core_core.version")
        signal.stats.androidGradlePlugin shouldBe "9.4.0"
        signal.stats.locales.size shouldBe 92
    }

    @Test
    fun `SDKs with class and manifest evidence`() {
        val sentry = insights("bluesky").sdks.single { it.name == "Sentry" }
        sentry.category shouldBe "crash-reporting"
        (sentry.classes > 1000) shouldBe true
        sentry.manifest shouldContain "io.sentry.android.core.SentryInitProvider"
    }

    @Test
    fun `native libraries, JNI mapping and endpoints from binaries`() {
        val gdx = insights("shatteredpd")
        gdx.nativeLibraries.map { it.name }.toSet() shouldContain "libgdx.so"
        gdx.nativeSymbols.first { it.jniClass != null }.library shouldStartWith "lib/"
        val bluesky = insights("bluesky")
        bluesky.endpoints.map { it.url } shouldContain "https://bsky.social"
        bluesky.endpoints
            .single { it.url == "https://updates.bsky.app/manifest" }
            .sources
            .single()
            .kind shouldBe "manifest"
    }
}
