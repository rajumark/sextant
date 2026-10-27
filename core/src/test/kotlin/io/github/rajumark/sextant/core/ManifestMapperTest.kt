package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.XmlNode
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/** Hand-written manifest trees: each test pins one mapping rule. */
class ManifestMapperTest {
    private fun node(
        tag: String,
        vararg attrs: Pair<String, String>,
        children: List<XmlNode> = emptyList(),
    ) = XmlNode(tag, attrs.toMap().toSortedMap(), children)

    private fun manifest(
        vararg appChildren: XmlNode,
        top: List<XmlNode> = emptyList(),
        sdk: Pair<String, String> = "21" to "34",
    ) = node(
        "manifest",
        "package" to "com.example",
        "android:versionCode" to "42",
        "android:versionName" to "@string/version",
        children =
            listOf(node("uses-sdk", "android:minSdkVersion" to sdk.first, "android:targetSdkVersion" to sdk.second)) + top +
                node("application", "android:label" to "@string/app_name", "android:name" to ".App", children = appChildren.toList()),
    )

    private val resources =
        ResourceResolver(
            mapOf(
                "@string/app_name" to "Example",
                "@string/version" to "1.2.3",
                "@bool/yes" to "true",
                "@string/auth" to "com.example.files",
            ),
        )

    private fun mapper(
        root: XmlNode,
        warnings: MutableList<String> = mutableListOf(),
    ) = ManifestMapper(root, resources, warnings)

    private fun launcherFilter() =
        node(
            "intent-filter",
            children =
                listOf(
                    node("action", "android:name" to "android.intent.action.MAIN"),
                    node("category", "android:name" to "android.intent.category.LAUNCHER"),
                ),
        )

    @Test
    fun `facts resolve resource references and expand the application class`() {
        val facts = mapper(manifest()).facts()
        facts.packageName shouldBe "com.example"
        facts.versionName shouldBe "1.2.3"
        facts.versionCode shouldBe 42
        facts.minSdk shouldBe 21
        facts.targetSdk shouldBe 34
        facts.label shouldBe "Example"
        facts.applicationClass shouldBe "com.example.App"
        facts.flags.debuggable.shouldBeNull()
    }

    @Test
    fun `non-numeric sdk is reported as null with a warning`() {
        val warnings = mutableListOf<String>()
        mapper(manifest(sdk = "Baklava" to "Baklava"), warnings).facts().minSdk.shouldBeNull()
        warnings.first() shouldContain "minSdkVersion"
    }

    @Test
    fun `relative and bare component names are expanded with the package`() {
        val names =
            mapper(
                manifest(
                    node("activity", "android:name" to ".Main"),
                    node("activity", "android:name" to "Settings"),
                    node("activity", "android:name" to "org.other.Lib"),
                ),
            ).components().map { it.name }
        names shouldContainExactly listOf("com.example.Main", "com.example.Settings", "org.other.Lib")
    }

    @Test
    fun `components are sorted by type then name`() {
        val result =
            mapper(
                manifest(
                    node("provider", "android:name" to ".P", "android:authorities" to "a;b"),
                    node("service", "android:name" to ".S"),
                    node("activity", "android:name" to ".Z"),
                    node("activity", "android:name" to ".A"),
                ),
            ).components()
        result.map { it.type to it.name.substringAfterLast('.') } shouldContainExactly
            listOf("activity" to "A", "activity" to "Z", "service" to "S", "provider" to "P")
        result.last().authorities shouldContainExactly listOf("a", "b")
    }

    @Test
    fun `launcher, explicit exported via resource, alias target`() {
        val result =
            mapper(
                manifest(
                    node("activity", "android:name" to ".Main", children = listOf(launcherFilter())),
                    node("activity", "android:name" to ".Hidden", "android:exported" to "@bool/yes"),
                    node("activity-alias", "android:name" to ".Alias", "android:targetActivity" to ".Main"),
                ),
            ).components().associateBy { it.name.substringAfterLast('.') }
        result.getValue("Main").launcher shouldBe true
        result.getValue("Main").exported shouldBe true
        result.getValue("Main").exportedSource shouldBe "default"
        result.getValue("Hidden").exported shouldBe true
        result.getValue("Hidden").exportedSource shouldBe "explicit"
        result.getValue("Alias").targetActivity shouldBe "com.example.Main"
        result.getValue("Alias").exported shouldBe false
    }

    @Test
    fun `intent filter data, priority and autoVerify`() {
        val filter =
            node(
                "intent-filter",
                "android:autoVerify" to "true",
                "android:priority" to "5",
                children =
                    listOf(
                        node("action", "android:name" to "android.intent.action.VIEW"),
                        node("category", "android:name" to "android.intent.category.DEFAULT"),
                        node("category", "android:name" to "android.intent.category.BROWSABLE"),
                        node("data", "android:scheme" to "https", "android:host" to "example.com", "android:pathPrefix" to "/join"),
                    ),
            )
        val f =
            mapper(
                manifest(node("activity", "android:name" to ".Link", children = listOf(filter))),
            ).components().single().intentFilters.single()
        f.autoVerify shouldBe true
        f.priority shouldBe 5
        f.categories shouldContainExactly listOf("android.intent.category.BROWSABLE", "android.intent.category.DEFAULT")
        f.data.single().pathPrefix shouldBe "/join"
    }

    @Test
    fun `permissions - kinds, dangerous flag, custom, maxSdk, dedupe and order`() {
        val top =
            listOf(
                node("uses-permission", "android:name" to "android.permission.INTERNET"),
                node("uses-permission", "android:name" to "android.permission.INTERNET"),
                node("uses-permission", "android:name" to "android.permission.CAMERA"),
                node("uses-permission-sdk-23", "android:name" to "android.permission.READ_CONTACTS"),
                node("uses-permission", "android:name" to "com.vendor.permission.C2D", "android:maxSdkVersion" to "30"),
                node("permission", "android:name" to "com.example.SECRET", "android:protectionLevel" to "signature"),
                node("permission", "android:name" to "com.example.RISKY", "android:protectionLevel" to "dangerous"),
            )
        val perms = mapper(manifest(top = top)).permissions()
        perms.map { it.kind to it.name } shouldContainExactly
            listOf(
                "uses-permission" to "android.permission.CAMERA",
                "uses-permission" to "android.permission.INTERNET",
                "uses-permission" to "com.vendor.permission.C2D",
                "uses-permission-sdk-23" to "android.permission.READ_CONTACTS",
                "declared" to "com.example.RISKY",
                "declared" to "com.example.SECRET",
            )
        val byName = perms.associateBy { it.name }
        byName.getValue("android.permission.CAMERA").dangerous shouldBe true
        byName.getValue("android.permission.INTERNET").dangerous shouldBe false
        byName.getValue("com.vendor.permission.C2D").custom shouldBe true
        byName.getValue("com.vendor.permission.C2D").maxSdk shouldBe 30
        byName.getValue("com.example.RISKY").dangerous shouldBe true
        byName.getValue("com.example.SECRET").protectionLevel shouldBe "signature"
    }

    @Test
    fun `WRITE_EXTERNAL_STORAGE implies READ_EXTERNAL_STORAGE with the same maxSdk`() {
        val top =
            listOf(
                node(
                    "uses-permission",
                    "android:name" to "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android:maxSdkVersion" to "28",
                ),
            )
        val implied = mapper(manifest(top = top)).permissions().filter { it.kind == "implied" }
        implied shouldHaveSize 1
        implied.single().name shouldBe "android.permission.READ_EXTERNAL_STORAGE"
        implied.single().maxSdk shouldBe 28
        implied.single().impliedBy shouldBe "android.permission.WRITE_EXTERNAL_STORAGE"
    }

    @Test
    fun `nothing is implied when READ is already requested`() {
        val top =
            listOf(
                node("uses-permission", "android:name" to "android.permission.WRITE_EXTERNAL_STORAGE"),
                node("uses-permission", "android:name" to "android.permission.READ_EXTERNAL_STORAGE"),
            )
        mapper(manifest(top = top)).permissions().filter { it.kind == "implied" }.shouldBeEmpty()
    }

    @Test
    fun `unexpected boolean values are treated as absent with a warning`() {
        val warnings = mutableListOf<String>()
        val c = mapper(manifest(node("service", "android:name" to ".S", "android:exported" to "maybe")), warnings).components().single()
        c.exportedSource shouldBe "default"
        warnings.single() shouldContain "maybe"
    }

    @Test
    fun `provider authorities given as a resource reference are resolved`() {
        mapper(
            manifest(node("provider", "android:name" to ".P", "android:authorities" to "@string/auth")),
        ).components().single().authorities shouldContainExactly
            listOf("com.example.files")
    }

    @Test
    fun `features, libraries and OpenGL ES version`() {
        val m =
            manifest(
                node("uses-library", "android:name" to "org.apache.http.legacy", "android:required" to "false"),
                node("uses-native-library", "android:name" to "libOpenCL.so"),
                top =
                    listOf(
                        node("uses-feature", "android:name" to "android.hardware.camera"),
                        node("uses-feature", "android:name" to "android.hardware.nfc", "android:required" to "false"),
                        node("uses-feature", "android:glEsVersion" to "0x00030001", "android:required" to "true"),
                    ),
            )
        mapper(m).features().map { Triple(it.kind, it.name, it.required) } shouldContainExactly
            listOf(
                Triple("feature", "android.hardware.camera", null),
                Triple("feature", "android.hardware.nfc", false),
                Triple("feature", "glEsVersion", true),
                Triple("library", "org.apache.http.legacy", false),
                Triple("native-library", "libOpenCL.so", null),
            )
        mapper(m).features().single { it.name == "glEsVersion" }.version shouldBe "3.1"
    }

    @Test
    fun `metadata on application and components, values resolved`() {
        val m =
            manifest(
                node("meta-data", "android:name" to "app.flag", "android:value" to "@bool/yes"),
                node("property", "android:name" to "android.window.PROPERTY", "android:value" to "true"),
                node(
                    "service",
                    "android:name" to ".S",
                    children =
                        listOf(
                            node(
                                "meta-data",
                                "android:name" to "svc.key",
                                "android:resource" to "@xml/svc",
                            ),
                        ),
                ),
            )
        mapper(m).metadata().map { listOf(it.scope, it.kind, it.name, it.value, it.resource) } shouldContainExactly
            listOf(
                listOf("application", "property", "android.window.PROPERTY", "true", null),
                listOf("application", "meta-data", "app.flag", "true", null),
                listOf("com.example.S", "meta-data", "svc.key", null, "@xml/svc"),
            )
    }

    @Test
    fun `queries - packages, intents and providers`() {
        val q =
            node(
                "queries",
                children =
                    listOf(
                        node("package", "android:name" to "com.google.android.apps.maps"),
                        node("provider", "android:authorities" to "com.example.data"),
                        node(
                            "intent",
                            children =
                                listOf(
                                    node("action", "android:name" to "android.intent.action.VIEW"),
                                    node(
                                        "data",
                                        "android:scheme" to "https",
                                    ),
                                ),
                        ),
                    ),
            )
        val result = mapper(manifest(top = listOf(q))).queries()
        result.map { it.kind to it.name } shouldContainExactly
            listOf("package" to "com.google.android.apps.maps", "intent" to "VIEW https:", "provider" to "com.example.data")
        result[1].data.single().scheme shouldBe "https"
    }

    @Test
    fun `provider, activity and service attributes`() {
        val m =
            manifest(
                node(
                    "provider",
                    "android:name" to ".P",
                    "android:authorities" to "a",
                    "android:readPermission" to "a.READ",
                    "android:grantUriPermissions" to "true",
                    children = listOf(node("path-permission", "android:pathPrefix" to "/x", "android:writePermission" to "a.WRITE_X")),
                ),
                node("activity", "android:name" to ".A", "android:launchMode" to "singleTask", "android:taskAffinity" to ""),
                node("service", "android:name" to ".S", "android:foregroundServiceType" to "dataSync|location"),
            )
        val c = mapper(m).components().associateBy { it.name.substringAfterLast('.') }
        c.getValue("P").readPermission shouldBe "a.READ"
        c.getValue("P").grantUriPermissions shouldBe true
        c
            .getValue("P")
            .pathPermissions
            .single()
            .writePermission shouldBe "a.WRITE_X"
        c.getValue("A").launchMode shouldBe "singleTask"
        c.getValue("A").taskAffinity shouldBe ""
        c.getValue("S").foregroundServiceType shouldBe "dataSync|location"
    }

    @Test
    fun `identity and application flags`() {
        val root = manifest(node("profileable", "android:shell" to "true"))
        val withIdentity =
            root.copy(
                attributes =
                    (
                        root.attributes + ("android:sharedUserId" to "a.shared") +
                            ("android:installLocation" to "auto")
                    ).toSortedMap(),
            )
        val flags = mapper(withIdentity).facts().flags
        flags.sharedUserId shouldBe "a.shared"
        flags.installLocation shouldBe "auto"
        flags.profileableShell shouldBe true
        flags.testOnly.shouldBeNull()
    }
}
