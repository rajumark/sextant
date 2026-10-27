package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.CorpusApks
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.model.AppFlags
import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.ResourceValue
import io.github.rajumark.sextant.core.model.StringRecord
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test

class FindingsTest {
    private val noFlags = AppFlags(null, null, null, null, null)

    private fun component(
        type: String,
        name: String,
        exported: Boolean = true,
        permission: String? = null,
        launcher: Boolean = false,
    ) = Component(type, name, exported, "explicit", true, permission, null, launcher, null, emptyList(), emptyList())

    @Test
    fun `manifest rules`() {
        val flags =
            AppFlags(
                debuggable = true,
                allowBackup = true,
                usesCleartextTraffic = true,
                networkSecurityConfig = null,
                extractNativeLibs = null,
            )
        val ids =
            Findings
                .build(
                    flags,
                    34,
                    listOf(
                        component("provider", "a.P"),
                        component("activity", "a.Main", launcher = true),
                        component("service", "a.S", permission = "a.PERM"),
                        component("receiver", "a.R"),
                    ),
                    emptyList(),
                    null,
                ).associateBy { it.id }
        ids.getValue("debuggable").severity shouldBe "high"
        ids.getValue("cleartext").severity shouldBe "medium"
        ids
            .getValue("exported-provider")
            .evidence
            .single()
            .where shouldBe "a.P"
        // Launcher activities and permission-protected components are not reported.
        ids.getValue("exported-component").evidence.map { it.where } shouldBe listOf("a.R")
    }

    @Test
    fun `cleartext is the default below targetSdk 28 without a network security config`() {
        Findings.build(noFlags, 27, emptyList(), emptyList(), null).map { it.id } shouldContain "cleartext-default"
        Findings.build(noFlags, 28, emptyList(), emptyList(), null).map { it.id } shouldNotContain "cleartext-default"
    }

    @Test
    fun `code rules correlate calls with the strings loaded in the same method`() {
        val c = FindingsCollector()
        c.call(
            CallRecord(
                "La;->hash()V",
                "Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;",
                "static",
                1,
                listOf(2),
            ),
        )
        c.string(StringRecord("MD5", "La;->hash()V", 1, listOf(0)))
        c.call(
            CallRecord(
                "Lb;->safe()V",
                "Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;",
                "static",
                1,
                listOf(2),
            ),
        )
        c.string(StringRecord("SHA-256", "Lb;->safe()V", 1, listOf(0)))
        c.classRecord(
            ClassRecord(
                "Lt;",
                "t",
                "",
                "Ljava/lang/Object;",
                listOf("Ljavax/net/ssl/X509TrustManager;"),
                listOf("public"),
                null,
                "classes.dex",
                false,
                emptyList(),
                3,
                0,
            ),
        )
        c.method(
            MethodRecord(
                "Lt;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V",
                "Lt;",
                "checkServerTrusted",
                emptyList(),
                "V",
                listOf("public"),
                true,
                1,
                3,
                0,
                0,
            ),
        )
        val ids = Findings.build(noFlags, 34, emptyList(), emptyList(), c).associateBy { it.id }
        ids
            .getValue("weak-hash")
            .evidence
            .single()
            .where shouldBe "La;->hash()V"
        ids.getValue("trust-all-certificates").severity shouldBe "high"
    }

    @Test
    fun `secrets are detected and masked, a bare PEM header is not a key`() {
        val c = FindingsCollector()
        c.string(StringRecord("-----BEGIN PRIVATE KEY-----", "Lp;->parse()V", 1, listOf(0)))
        c.string(StringRecord("AKIAIOSFODNN7EXAMPLE", "Lk;->aws()V", 1, listOf(0)))
        val findings =
            Findings.build(
                noFlags,
                34,
                emptyList(),
                listOf(ResourceValue("string", "google_api_key", "AIzaSyA-1234567890abcdefghijklmnopqrstu")),
                c,
            )
        val ids = findings.map { it.id }
        ids shouldNotContain "secret-private-key"
        val aws = findings.single { it.id == "secret-aws-key" }
        aws.evidence.single().value!! shouldNotContain "IOSFODNN7"
        findings
            .single { it.id == "google-api-key" }
            .evidence
            .single()
            .where shouldBe "@string/google_api_key"
    }

    @Test
    fun `facts from real APKs`() {
        val appManager =
            Extractor
                .extract(CorpusApks.require("appmanager"))
                .insights!!
                .findings
                .associateBy { it.id }
        appManager.getValue("trust-all-certificates").evidence.map { it.where } shouldContain
            "Lio/github/muntashirakon/adb/SslUtils\$2;->checkServerTrusted([Ljava/security/cert/X509Certificate;Ljava/lang/String;)V"
        appManager.keys shouldNotContain "secret-private-key"
        val signal =
            Extractor
                .extract(CorpusApks.require("signal"))
                .insights!!
                .findings
                .map { it.id }
        signal shouldContain "weak-cipher"
        signal shouldNotContain "debuggable"
    }

    @Test
    fun `providers protected by read and write permissions are not flagged, half-protected ones are`() {
        val both = component("provider", "a.Both").copy(readPermission = "a.R", writePermission = "a.W")
        val readOnly = component("provider", "a.ReadOnly").copy(readPermission = "a.R")
        val open = component("provider", "a.Open")
        val ids = Findings.build(noFlags, 34, listOf(both, readOnly, open), emptyList(), null).associateBy { it.id }
        ids.getValue("exported-provider").evidence.map { it.where } shouldBe listOf("a.Open")
        ids.getValue("exported-provider-partial").evidence.single().let {
            it.where shouldBe "a.ReadOnly"
            it.value!! shouldContain "write open"
        }
    }

    @Test
    fun `identity and debug-style flags`() {
        val flags = noFlags.copy(testOnly = true, sharedUserId = "a.shared", profileableShell = true)
        val ids = Findings.build(flags, 34, emptyList(), emptyList(), null).map { it.id }
        ids shouldContain "test-only"
        ids shouldContain "shared-user-id"
        ids shouldContain "profileable-shell"
    }
}
