package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.CodeSink
import io.github.rajumark.sextant.core.ResourceResolver
import io.github.rajumark.sextant.core.android
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.EndpointRecord
import io.github.rajumark.sextant.core.model.EndpointSource
import io.github.rajumark.sextant.core.model.FrameworkRecord
import io.github.rajumark.sextant.core.model.HostCount
import io.github.rajumark.sextant.core.model.InsightStats
import io.github.rajumark.sextant.core.model.LibraryRecord
import io.github.rajumark.sextant.core.model.NativeLibrary
import io.github.rajumark.sextant.core.model.NativeSymbol
import io.github.rajumark.sextant.core.model.ResourceValue
import io.github.rajumark.sextant.core.model.SdkRecord
import io.github.rajumark.sextant.core.model.StringRecord
import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.core.output.InfoReports
import io.github.rajumark.sextant.core.output.SextantJson
import io.github.rajumark.sextant.core.toHex
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Path
import java.security.MessageDigest
import java.util.TreeMap
import java.util.TreeSet
import java.util.zip.ZipFile

/** Everything Version 5 derives about an APK. Lists are sorted, so output is deterministic. */
data class Insights(
    val stats: InsightStats,
    val libraries: List<LibraryRecord>,
    val frameworks: List<FrameworkRecord>,
    val sdks: List<SdkRecord>,
    val endpoints: List<EndpointRecord>,
    val resources: List<ResourceValue>,
    val nativeLibraries: List<NativeLibrary>,
    val nativeSymbols: List<NativeSymbol>,
    val findings: List<Finding> = emptyList(),
)

/** Collects what insights need from the code stream: class names and URLs in string constants. */
class InsightsCollector : CodeSink {
    val classNames = ArrayList<String>()
    var kotlinClasses = 0
    internal val codeUrls = TreeMap<String, TreeSet<String>>()

    override fun classRecord(record: ClassRecord) {
        classNames += record.name
        if (record.kotlin) kotlinClasses++
    }

    override fun string(record: StringRecord) {
        for (url in Urls.find(record.value)) {
            val methods = codeUrls.getOrPut(url) { TreeSet() }
            if (methods.size < MAX_SOURCES) methods += record.method
        }
    }
}

/** Evidence entries kept per URL. */
private const val MAX_SOURCES = 20

/** URL extraction from free text. Only URLs with a plausible host are kept. */
object Urls {
    private val HOST = Regex("""^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$|^\[[0-9a-f:.]+]$""")

    fun find(text: String): List<String> =
        candidates(text).map { it.trimEnd('.', ',', ';', ':', ')', '\'', '"', ']') }.filter { host(it) != null }.toList()

    private const val URL_CHARS = "-._~:/?#[]@!$&'()*+,;=%"
    private val SCHEMES = listOf("https", "http", "wss", "ws", "ftp")

    private fun isUrlChar(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in URL_CHARS

    /** Every `scheme://…` run in [text]. Jumps between "://" occurrences, so megabytes of binary data scan quickly. */
    private fun candidates(text: String): Sequence<String> =
        sequence {
            var at = text.indexOf("://")
            while (at >= 0) {
                val scheme =
                    SCHEMES.firstOrNull { s ->
                        at >= s.length &&
                            text.regionMatches(at - s.length, s, 0, s.length, ignoreCase = true)
                    }
                if (scheme != null) {
                    val start = at - scheme.length
                    val boundary = start == 0 || !text[start - 1].isLetterOrDigit()
                    var end = at + 3
                    while (end < text.length && isUrlChar(text[end])) end++
                    if (boundary && end > at + 3) yield(text.substring(start, end))
                    at = text.indexOf("://", end)
                } else {
                    at = text.indexOf("://", at + 3)
                }
            }
        }

    /**
     * Binary files (Hermes bytecode, Dart snapshots, ELF) pack strings back to back without separators, so the path
     * of a URL found there cannot be trusted: it may run into the next string. Only `scheme://host` is reported, and
     * only for hosts that look like real domains (a dot), IP addresses, or localhost.
     */
    fun findInBinary(text: String): List<String> {
        if ("://" !in text) return emptyList()
        return candidates(text)
            .mapNotNull { url ->
                val host = host(url) ?: return@mapNotNull null
                if ('.' !in host && host != "localhost" && !host.startsWith("[")) return@mapNotNull null
                url.substringBefore("://").lowercase() + "://" + host
            }.distinct()
            .toList()
    }

    /** `https://user@api.example.com:8443/x` → `api.example.com`; null if the host is not a plausible name or IP. */
    fun host(url: String): String? {
        val afterScheme = url.substringAfter("://")
        val authority = afterScheme.takeWhile { it != '/' && it != '?' && it != '#' }.substringAfterLast('@')
        val host = (if (authority.startsWith("[")) authority.substringBefore(']') + "]" else authority.substringBefore(':')).lowercase()
        return host.takeIf { it.isNotEmpty() && HOST.matches(it) }
    }
}

@Serializable
private data class SdkRule(
    val name: String,
    val category: String,
    val packages: List<String>,
    val manifest: List<String>,
)

object InsightsBuilder {
    private const val MAX_NATIVE_BYTES = 256L * 1024 * 1024
    private val VERSION_FILE = Regex("""META-INF/([^/]+?)_([^/]+)\.version""")
    private const val MAX_ASSET_BYTES = 64L * 1024 * 1024

    /** Text-like assets worth scanning for URLs: JS bundles (React Native/Hermes), configs, web assets. */
    private val SCANNED_ASSET = Regex("""assets/.*\.(bundle|hbc|js|json|html?|xml|txt|properties|cfg|conf|ini|yaml|yml)""")
    private val NATIVE_LIB = Regex("""lib/([^/]+)/([^/]+\.so)""")

    private val rules: List<SdkRule> by lazy {
        val text =
            InsightsBuilder::class.java
                .getResourceAsStream("/io/github/rajumark/sextant/sdks.json")!!
                .use { it.readBytes().decodeToString() }
        SextantJson.reader.decodeFromString(ListSerializer(SdkRule.serializer()), text)
    }

    fun build(
        apk: Path,
        manifest: XmlNode,
        components: List<Component>,
        resources: ResourceResolver,
        code: InsightsCollector?,
        warnings: MutableList<String>,
    ): Insights {
        val libraries = mutableListOf<LibraryRecord>()
        val natives = mutableListOf<NativeLibrary>()
        val symbols = mutableListOf<NativeSymbol>()
        val entryNames = mutableListOf<String>()
        val binaryUrls = TreeMap<String, TreeSet<EndpointSource>>()

        fun scan(
            bytes: ByteArray,
            source: EndpointSource,
        ) {
            // ISO-8859-1 maps every byte to one char, so URLs inside binary files (Hermes bytecode, Dart snapshots, ELF) survive.
            for (url in Urls.findInBinary(String(bytes, Charsets.ISO_8859_1))) {
                val set = binaryUrls.getOrPut(url) { TreeSet(compareBy({ it.kind }, { it.where })) }
                if (set.size < MAX_SOURCES) set += source
            }
        }
        var agp: String? = null
        ZipFile(apk.toFile()).use { zip ->
            for (entry in zip.entries().asSequence().sortedBy { it.name }) {
                val name = entry.name
                entryNames += name
                VERSION_FILE.matchEntire(name)?.let { m ->
                    val version = zip.getInputStream(entry).use { it.readNBytes(256).decodeToString().trim() }
                    if (version.isNotEmpty() && version.length <= 64 && '\n' !in version) {
                        libraries += LibraryRecord(m.groupValues[1], m.groupValues[2], version, name)
                    }
                }
                if (SCANNED_ASSET.matches(name) && entry.size in 1..MAX_ASSET_BYTES) {
                    scan(zip.getInputStream(entry).use { it.readNBytes(MAX_ASSET_BYTES.toInt()) }, EndpointSource("asset", name))
                }
                if (name == "META-INF/com/android/build/gradle/app-metadata.properties") {
                    val props = zip.getInputStream(entry).use { it.readNBytes(4096).decodeToString() }
                    agp =
                        props
                            .lines()
                            .firstOrNull { it.startsWith("androidGradlePluginVersion=") }
                            ?.substringAfter('=')
                            ?.trim()
                }
                NATIVE_LIB.matchEntire(name)?.let { m ->
                    val bytes = zip.getInputStream(entry).use { it.readNBytes((MAX_NATIVE_BYTES + 1).toInt()) }
                    if (bytes.size > MAX_NATIVE_BYTES) {
                        warnings += "$name is larger than ${MAX_NATIVE_BYTES / 1024 / 1024} MB; symbols not read"
                        return@let
                    }
                    scan(bytes, EndpointSource("native", name))
                    val elf = Elf.read(bytes)
                    val exported = elf?.exportedFunctions.orEmpty()
                    var jni = 0
                    for (sym in exported) {
                        val mapped = Elf.jni(sym)
                        if (mapped != null) jni++
                        symbols += NativeSymbol(name, sym, mapped?.first, mapped?.second)
                    }
                    natives +=
                        NativeLibrary(
                            path = name,
                            abi = m.groupValues[1],
                            name = m.groupValues[2],
                            sizeBytes = bytes.size.toLong(),
                            sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).toHex(),
                            elfClass = elf?.elfClass,
                            exportedSymbols = exported.size,
                            jniMethods = jni,
                        )
                }
            }
        }

        val manifestNames = manifestNames(manifest, components)
        val classNames = code?.classNames.orEmpty()
        val frameworks = Frameworks.detect(entryNames, classNames, manifestNames, code?.kotlinClasses ?: 0)
        val sdks = sdks(classNames, manifestNames)
        val endpoints = endpoints(code, resources, manifest, components, binaryUrls)
        val hosts = endpoints.groupingBy { it.host }.eachCount()

        val stats =
            InsightStats(
                libraries = libraries.size,
                frameworks = frameworks.map { it.name },
                sdks = sdks.size,
                sdkCategories = sdks.groupingBy { it.category }.eachCount().toSortedMap(),
                endpoints = endpoints.size,
                hosts = hosts.size,
                topHosts =
                    hosts.entries
                        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                        .take(TOP_HOSTS)
                        .map { HostCount(it.key, it.value) },
                resourceValues = resources.entries.size,
                locales = resources.locales,
                nativeLibraries = natives.size,
                jniMethods = natives.sumOf { it.jniMethods },
                androidGradlePlugin = agp,
            )
        return Insights(
            stats = stats,
            libraries = libraries.sortedWith(compareBy({ it.group }, { it.artifact })),
            frameworks = frameworks,
            sdks = sdks,
            endpoints = endpoints,
            resources = resources.entries,
            nativeLibraries = natives.sortedBy { it.path },
            nativeSymbols = symbols.sortedWith(compareBy({ it.library }, { it.name })),
        )
    }

    /** Component names, meta-data names and values, and provider authorities: SDKs often show up here even when obfuscated. */
    private fun manifestNames(
        manifest: XmlNode,
        components: List<Component>,
    ): Set<String> {
        val names = sortedSetOf<String>()
        components.forEach { c ->
            names += c.name
            names += c.authorities
        }

        fun walk(node: XmlNode) {
            if (node.tag == "meta-data") {
                node.android("name")?.let(names::add)
                node.android("value")?.let(names::add)
            }
            node.children.forEach(::walk)
        }
        walk(manifest)
        return names
    }

    private fun matches(
        name: String,
        prefix: String,
    ) = name == prefix || name.startsWith("$prefix.") || name.startsWith("$prefix$")

    /** Index of rule prefixes: a class is matched by looking up each of its dotted prefixes, not by scanning every rule. */
    private val prefixIndex: Map<String, List<SdkRule>> by lazy {
        rules.flatMap { r -> r.packages.map { it to r } }.groupBy({ it.first }, { it.second })
    }

    private fun sdks(
        classNames: List<String>,
        manifestNames: Set<String>,
    ): List<SdkRecord> {
        val matched = HashMap<SdkRule, MutableList<String>>()
        for (cls in classNames) {
            val hit = HashSet<SdkRule>()
            var i = cls.indexOf('.')
            while (true) {
                val prefix = if (i < 0) cls else cls.substring(0, i)
                prefixIndex[prefix]?.let(hit::addAll)
                // Inner classes: "com.a.B$C" also matches the rule "com.a.B".
                val dollar = prefix.indexOf('$')
                if (dollar > 0) prefixIndex[prefix.substring(0, dollar)]?.let(hit::addAll)
                if (i < 0) break
                i = cls.indexOf('.', i + 1)
            }
            hit.forEach { matched.getOrPut(it) { mutableListOf() } += cls }
        }
        return rules
            .mapNotNull { rule ->
                val classes = matched[rule].orEmpty()
                val manifest = manifestNames.filter { n -> rule.manifest.any { matches(n, it) } }
                if (classes.isEmpty() && manifest.isEmpty()) return@mapNotNull null
                val packages =
                    classes
                        .groupingBy { it.substringBeforeLast('.', "") }
                        .eachCount()
                        .entries
                        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                        .take(MAX_PACKAGES)
                        .map { it.key }
                SdkRecord(rule.name, rule.category, classes.size, packages, manifest.take(MAX_PACKAGES))
            }.sortedWith(compareBy({ it.category }, { it.name }))
    }

    private fun endpoints(
        code: InsightsCollector?,
        resources: ResourceResolver,
        manifest: XmlNode,
        components: List<Component>,
        binaryUrls: Map<String, Set<EndpointSource>>,
    ): List<EndpointRecord> {
        val sources = TreeMap<String, MutableList<EndpointSource>>()

        fun add(
            url: String,
            source: EndpointSource,
        ) {
            val list = sources.getOrPut(url) { mutableListOf() }
            if (list.size < MAX_SOURCES && source !in list) list += source
        }
        code?.codeUrls?.forEach { (url, methods) -> methods.forEach { add(url, EndpointSource("code", it)) } }
        binaryUrls.forEach { (url, srcs) -> srcs.forEach { add(url, it) } }
        resources.entries.filter { it.type == "string" }.forEach { r ->
            Urls.find(r.value).forEach { add(it, EndpointSource("resource", "@string/${r.name}")) }
        }
        components.forEach { c -> InfoReports.deepLinks(c).filter { "://" in it }.forEach { add(it, EndpointSource("deep_link", c.name)) } }

        fun walk(node: XmlNode) {
            if (node.tag == "meta-data") {
                node.android("value")?.let { v ->
                    Urls.find(v).forEach {
                        add(
                            it,
                            EndpointSource(
                                "manifest",
                                node.android("name") ?: "meta-data",
                            ),
                        )
                    }
                }
            }
            node.children.forEach(::walk)
        }
        walk(manifest)
        return sources.map { (url, src) ->
            // Deep links may use wildcard hosts (`*.example.com`): keep them, with the host as written.
            val host = Urls.host(url) ?: url.substringAfter("://").takeWhile { it != '/' && it != '?' && it != '#' }.lowercase()
            EndpointRecord(url, url.substringBefore("://").lowercase(), host, src.sortedWith(compareBy({ it.kind }, { it.where })))
        }
    }

    private const val TOP_HOSTS = 15
    private const val MAX_PACKAGES = 10
}
