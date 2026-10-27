package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.CodeSink
import io.github.rajumark.sextant.core.model.AppFlags
import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.ResourceValue
import io.github.rajumark.sextant.core.model.StringRecord
import kotlinx.serialization.Serializable

/**
 * A point worth reviewing, with the evidence that triggered it. Findings are static observations, not proven
 * vulnerabilities: a check may exist elsewhere, and some patterns are legitimate in context.
 */
@Serializable
data class Finding(
    val id: String,
    /** info | low | medium | high */
    val severity: String,
    val title: String,
    val detail: String,
    val evidence: List<FindingEvidence>,
    /** How many places matched; [evidence] is capped. */
    val count: Int,
)

@Serializable
data class FindingEvidence(
    /** manifest | method | class | string | resource */
    val kind: String,
    val where: String,
    val value: String? = null,
)

/** Collects what the findings rules need from the code stream. */
class FindingsCollector : CodeSink {
    internal val callers = HashMap<String, MutableSet<String>>() // watched callee prefix → caller methods
    internal val methodStrings = HashMap<String, MutableSet<String>>() // method → watched strings it loads
    internal val secrets = mutableListOf<Pair<String, FindingEvidence>>() // rule id → evidence
    internal val trustManagers = HashSet<String>()
    internal val hostnameVerifiers = HashSet<String>()
    internal val tinyMethods = HashMap<String, Int>() // checkServerTrusted / verify implementations → instruction count

    override fun classRecord(record: ClassRecord) {
        if ("Ljavax/net/ssl/X509TrustManager;" in record.interfaces) trustManagers += record.id
        if ("Ljavax/net/ssl/HostnameVerifier;" in record.interfaces) hostnameVerifiers += record.id
    }

    override fun method(record: MethodRecord) {
        val owner = record.`class`
        val isTrust = owner in trustManagers && record.name == "checkServerTrusted"
        val isVerify = owner in hostnameVerifiers && record.id.endsWith("->verify(Ljava/lang/String;Ljavax/net/ssl/SSLSession;)Z")
        if ((isTrust || isVerify) && record.hasCode) tinyMethods[record.id] = record.instructions
    }

    override fun call(record: CallRecord) {
        for (prefix in Findings.WATCHED_CALLS) {
            if (record.callee.startsWith(prefix)) callers.getOrPut(prefix) { sortedSetOf() } += record.caller
        }
    }

    override fun string(record: StringRecord) {
        val v = record.value
        if (v.length <= 32 && Findings.isWatchedString(v)) methodStrings.getOrPut(record.method) { HashSet() } += v
        Findings.secretRule(v)?.let { rule -> secrets += rule to FindingEvidence("string", record.method, Findings.mask(v)) }
    }
}

object Findings {
    private const val MAX_EVIDENCE = 25

    private const val WEBVIEW_JS = "Landroid/webkit/WebSettings;->setJavaScriptEnabled("
    private const val JS_BRIDGE = "Landroid/webkit/WebView;->addJavascriptInterface("
    private const val FILE_ACCESS = "Landroid/webkit/WebSettings;->setAllowFileAccessFromFileURLs("
    private const val DEX_LOADER = "Ldalvik/system/DexClassLoader;-><init>("
    private const val MEM_LOADER = "Ldalvik/system/InMemoryDexClassLoader;-><init>("
    private const val EXEC = "Ljava/lang/Runtime;->exec("
    private const val PROCESS = "Ljava/lang/ProcessBuilder;->start("
    private const val CIPHER = "Ljavax/crypto/Cipher;->getInstance("
    private const val DIGEST = "Ljava/security/MessageDigest;->getInstance("
    private const val RANDOM_SEED = "Ljava/security/SecureRandom;->setSeed("

    val WATCHED_CALLS = listOf(WEBVIEW_JS, JS_BRIDGE, FILE_ACCESS, DEX_LOADER, MEM_LOADER, EXEC, PROCESS, CIPHER, DIGEST, RANDOM_SEED)

    private val WEAK_DIGESTS = setOf("MD5", "MD2", "MD4", "SHA-1", "SHA1", "SHA")
    private val WEAK_CIPHERS = setOf("DES", "DESede", "RC2", "RC4", "ARCFOUR", "Blowfish")

    fun isWatchedString(v: String) = v in WEAK_DIGESTS || v.substringBefore('/') in WEAK_CIPHERS || "/ECB/" in v || v == "AES"

    private val SECRET_RULES =
        listOf(
            "secret-aws-key" to Regex("""\bAKIA[0-9A-Z]{16}\b"""),
            // A header alone is just a parser constant; require base64 key material after it.
            "secret-private-key" to Regex("""-----BEGIN (?:RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----\s*[A-Za-z0-9+/=\s]{64,}"""),
            "secret-stripe-live" to Regex("""\bsk_live_[0-9A-Za-z]{20,}\b"""),
            "secret-slack-token" to Regex("""\bxox[baprs]-[0-9A-Za-z-]{10,}\b"""),
            "secret-github-token" to Regex("""\bgh[pousr]_[0-9A-Za-z]{36}\b"""),
            "google-api-key" to Regex("""\bAIza[0-9A-Za-z_-]{35}\b"""),
            "jwt" to Regex("""\beyJ[0-9A-Za-z_-]{10,}\.eyJ[0-9A-Za-z_-]{10,}\.[0-9A-Za-z_-]{10,}\b"""),
        )

    fun secretRule(v: String): String? =
        if (v.length < 16 || v.length > 4096) null else SECRET_RULES.firstOrNull { (_, re) -> re.containsMatchIn(v) }?.first

    /** Shows enough of a secret to recognize it without reproducing it: first 6 and last 4 characters. */
    fun mask(v: String): String {
        val token = SECRET_RULES.firstNotNullOfOrNull { (_, re) -> re.find(v)?.value } ?: v
        return if (token.length <= 12) token.take(3) + "…" else token.take(6) + "…" + token.takeLast(4)
    }

    fun build(
        flags: AppFlags,
        targetSdk: Int?,
        components: List<Component>,
        resources: List<ResourceValue>,
        code: FindingsCollector?,
        packageName: String? = null,
    ): List<Finding> {
        val out = mutableListOf<Finding>()

        fun add(
            id: String,
            severity: String,
            title: String,
            detail: String,
            evidence: List<FindingEvidence>,
        ) {
            if (evidence.isNotEmpty()) out += Finding(id, severity, title, detail, evidence.take(MAX_EVIDENCE), evidence.size)
        }

        // Manifest.
        if (flags.debuggable == true) {
            add(
                "debuggable",
                "high",
                "App is debuggable",
                "android:debuggable=true lets anyone attach a debugger to the app.",
                listOf(manifest("application debuggable=true")),
            )
        }
        when {
            flags.usesCleartextTraffic == true ->
                add(
                    "cleartext",
                    "medium",
                    "Cleartext HTTP allowed",
                    "android:usesCleartextTraffic=true allows unencrypted HTTP.",
                    listOf(manifest("application usesCleartextTraffic=true")),
                )
            flags.usesCleartextTraffic == null && flags.networkSecurityConfig == null && (targetSdk ?: 0) < 28 ->
                add(
                    "cleartext-default",
                    "low",
                    "Cleartext HTTP allowed by default",
                    "targetSdk < 28 with no network security config: cleartext HTTP is permitted.",
                    listOf(manifest("targetSdk=$targetSdk, no networkSecurityConfig")),
                )
        }
        if (flags.allowBackup == true) {
            add(
                "allow-backup",
                "info",
                "App data can be backed up",
                "android:allowBackup=true: data may be copied off the device via backup.",
                listOf(manifest("application allowBackup=true")),
            )
        }
        if (flags.testOnly == true) {
            add(
                "test-only",
                "medium",
                "Test-only build",
                "android:testOnly=true: a debug/test build that the Play Store rejects; it can only be installed with adb install -t.",
                listOf(manifest("application testOnly=true")),
            )
        }
        flags.sharedUserId?.let {
            add(
                "shared-user-id",
                "low",
                "Shares a user ID with other apps",
                "android:sharedUserId lets apps signed with the same key share one process and all data. Deprecated since Android 10.",
                listOf(manifest("manifest sharedUserId=$it")),
            )
        }
        if (flags.profileableShell == true) {
            add(
                "profileable-shell",
                "low",
                "Profileable from the shell",
                "<profileable android:shell=\"true\">: adb shell can attach profilers to this release build.",
                listOf(manifest("application/profileable shell=true")),
            )
        }
        if (flags.requestLegacyExternalStorage == true) {
            add(
                "legacy-storage",
                "info",
                "Legacy external storage requested",
                "requestLegacyExternalStorage opts out of scoped storage on Android 10 (ignored from Android 11).",
                listOf(manifest("application requestLegacyExternalStorage=true")),
            )
        }

        val exposed = components.filter { it.exported && it.enabled && !it.launcher }
        val providers = exposed.filter { it.type == "provider" && it.permission == null }
        // A provider is protected by android:permission, or by readPermission + writePermission together.
        add(
            "exported-provider",
            "medium",
            "Content provider exported without permission",
            "Other apps can query and change these providers; check what data and paths they expose.",
            providers
                .filter { it.readPermission == null && it.writePermission == null }
                .map { FindingEvidence("manifest", it.name, it.authorities.joinToString()) },
        )
        add(
            "exported-provider-partial",
            "low",
            "Content provider readable or writable without permission",
            "Only one of readPermission / writePermission is set, so the other direction is open to every app.",
            providers
                .filter { (it.readPermission == null) != (it.writePermission == null) }
                .map { p ->
                    val open = if (p.readPermission == null) "read" else "write"
                    FindingEvidence("manifest", p.name, "$open open; ${p.authorities.joinToString()}")
                },
        )
        add(
            "uri-grants-any-path",
            "info",
            "Exported provider grants URI permissions for any path",
            "An exported provider with grantUriPermissions=true and no <grant-uri-permission> elements can hand out temporary access to any of its URIs.",
            components
                // Non-exported providers with URI grants (the usual FileProvider setup) are the safe pattern; only exported ones matter.
                .filter { it.type == "provider" && it.exported && it.grantUriPermissions == true && it.grantUriPaths.isEmpty() }
                .map { FindingEvidence("manifest", it.name, it.authorities.joinToString()) },
        )
        add(
            "exported-component",
            "low",
            "Components exported without permission",
            "Other apps can start these activities, services or receivers; check how they handle incoming intents.",
            exposed.filter { it.type != "provider" && it.permission == null }.map { FindingEvidence("manifest", it.name, it.type) },
        )
        add(
            "task-affinity",
            "info",
            "Custom task affinity or task reparenting",
            "Activities with a taskAffinity other than the app's, or allowTaskReparenting=true, can move between tasks; review them for task hijacking.",
            components
                .filter { c -> ComponentReviews.taskHijackReason(c, packageName) != null }
                .map { c ->
                    FindingEvidence("manifest", c.name, c.taskAffinity?.let { "taskAffinity=$it" } ?: "allowTaskReparenting=true")
                },
        )

        // Code.
        if (code != null) {
            fun callersOf(prefix: String) = code.callers[prefix].orEmpty().map { FindingEvidence("method", it) }
            add(
                "webview-javascript",
                "info",
                "WebView JavaScript enabled",
                "Review what content these WebViews load.",
                callersOf(WEBVIEW_JS),
            )
            add(
                "webview-js-bridge",
                "medium",
                "JavaScript bridge into a WebView",
                "addJavascriptInterface exposes Java methods to page scripts; content must be trusted.",
                callersOf(JS_BRIDGE),
            )
            add(
                "webview-file-access",
                "medium",
                "WebView file URL access",
                "setAllowFileAccessFromFileURLs is called; check it is set to false.",
                callersOf(FILE_ACCESS),
            )
            add(
                "dynamic-code",
                "medium",
                "Dynamic code loading",
                "Code is loaded at runtime (DexClassLoader / InMemoryDexClassLoader) and is not visible to static analysis.",
                callersOf(DEX_LOADER) + callersOf(MEM_LOADER),
            )
            add(
                "exec",
                "low",
                "Runs external commands",
                "Runtime.exec / ProcessBuilder.start is used; check the inputs.",
                callersOf(EXEC) + callersOf(PROCESS),
            )
            add(
                "seeded-random",
                "low",
                "SecureRandom with explicit seed",
                "setSeed on SecureRandom can make output predictable on some platforms.",
                callersOf(RANDOM_SEED),
            )

            fun correlated(
                prefix: String,
                match: (String) -> Boolean,
            ) = code.callers[prefix].orEmpty().flatMap { m ->
                code.methodStrings[m]
                    .orEmpty()
                    .filter(match)
                    .map { FindingEvidence("method", m, it) }
            }
            add(
                "weak-hash",
                "low",
                "Weak hash algorithm",
                "MD5/SHA-1 are not collision resistant; fine for checksums, not for security decisions.",
                correlated(DIGEST) { it in WEAK_DIGESTS },
            )
            add(
                "weak-cipher",
                "low",
                "Weak cipher or ECB mode",
                "DES/RC4/… or ECB mode (\"AES\" alone defaults to ECB); ECB leaks patterns unless used for single-block key wrapping.",
                correlated(CIPHER) { it.substringBefore('/') in WEAK_CIPHERS || "/ECB/" in it || it == "AES" },
            )
            add(
                "trust-all-certificates",
                "high",
                "TrustManager may accept every certificate",
                "checkServerTrusted has at most one instruction (an empty body), so certificates are not validated.",
                code.tinyMethods
                    .filter {
                        it.key.contains(
                            "->checkServerTrusted",
                        ) &&
                            it.value <= 1
                    }.keys
                    .sorted()
                    .map { FindingEvidence("method", it) },
            )
            add(
                "accept-all-hostnames",
                "medium",
                "HostnameVerifier may accept every host",
                "verify() has at most two instructions (likely `return true`).",
                code.tinyMethods
                    .filter {
                        it.key.contains(
                            "->verify(",
                        ) &&
                            it.value <= 2
                    }.keys
                    .sorted()
                    .map { FindingEvidence("method", it) },
            )
            for ((rule, evidence) in code.secrets.groupBy({ it.first }, { it.second })) secretFinding(rule, evidence)?.let(out::add)
        }

        // Secrets in resources.
        val resourceSecrets =
            resources.filter { it.type == "string" }.mapNotNull { r ->
                secretRule(r.value)?.let {
                    it to
                        FindingEvidence("resource", "@string/${r.name}", mask(r.value))
                }
            }
        for ((rule, evidence) in resourceSecrets.groupBy({ it.first }, { it.second })) {
            val finding = secretFinding(rule, evidence) ?: continue
            val existing = out.indexOfFirst { it.id == finding.id }
            if (existing < 0) {
                out += finding
            } else {
                val merged = out[existing].evidence + finding.evidence
                out[existing] = out[existing].copy(evidence = merged.take(MAX_EVIDENCE), count = out[existing].count + finding.count)
            }
        }
        return out.sortedWith(compareBy({ SEVERITY.indexOf(it.severity) }, { it.id }))
    }

    private fun secretFinding(
        rule: String,
        evidence: List<FindingEvidence>,
    ): Finding? {
        val (severity, title, detail) =
            when (rule) {
                "secret-aws-key" ->
                    Triple(
                        "high",
                        "AWS access key",
                        "An AWS access key ID is embedded; check whether a secret key ships too.",
                    )
                "secret-private-key" -> Triple("high", "Private key embedded", "A PEM private key block is embedded in the app.")
                "secret-stripe-live" -> Triple("high", "Stripe live secret key", "A Stripe live secret key (sk_live_) is embedded.")
                "secret-slack-token" -> Triple("high", "Slack token", "A Slack token is embedded.")
                "secret-github-token" -> Triple("high", "GitHub token", "A GitHub token is embedded.")
                "google-api-key" ->
                    Triple(
                        "info",
                        "Google API key",
                        "Google API keys in apps are usually public by design; check they are restricted to this app.",
                    )
                "jwt" -> Triple("medium", "JSON Web Token", "A JWT is embedded; check it is not a long-lived credential.")
                else -> return null
            }
        val distinct = evidence.distinct()
        return Finding(rule, severity, title, detail, distinct.take(MAX_EVIDENCE), distinct.size)
    }

    private fun manifest(where: String) = FindingEvidence("manifest", where)

    private val SEVERITY = listOf("high", "medium", "low", "info")
}
