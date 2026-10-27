package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.IntentData
import io.github.rajumark.sextant.core.model.IntentFilter
import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.core.output.ManifestXml
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ComponentReviewTest {
    private fun data(
        scheme: String? = null,
        host: String? = null,
        pathPrefix: String? = null,
    ) = IntentData(scheme, host, null, null, pathPrefix, null, null, null, null)

    private fun filter(
        actions: List<String>,
        categories: List<String> = emptyList(),
        data: List<IntentData> = emptyList(),
        autoVerify: Boolean = false,
    ) = IntentFilter(actions, categories, data, 0, autoVerify)

    private fun component(
        type: String = "activity",
        name: String = "com.app.Main",
        exported: Boolean = true,
        permission: String? = null,
        launcher: Boolean = false,
        filters: List<IntentFilter> = emptyList(),
    ) = Component(type, name, exported, "explicit", true, permission, null, launcher, null, emptyList(), filters)

    private val appLink =
        filter(
            listOf("android.intent.action.VIEW"),
            listOf("android.intent.category.BROWSABLE", "android.intent.category.DEFAULT"),
            listOf(data(scheme = "https"), data(host = "example.org"), data(pathPrefix = "/join")),
            autoVerify = true,
        )

    @Test
    fun `verified https link is an app link and a deep link, combined across data elements`() {
        val r = ComponentReviews.review(component(filters = listOf(appLink)), "com.app")
        r.tags shouldContainExactly listOf("app-link", "deep-link")
        r.deepLinks shouldBe listOf("https://example.org/join*")
        r.appLinkHosts shouldBe listOf("example.org")
        r.entryPoints.single() shouldContain "verified App Link"
    }

    @Test
    fun `exported activity with no launcher, link or permission is a hidden entry`() {
        ComponentReviews.review(component(), "com.app").tags shouldContain "hidden-entry"
        ComponentReviews.review(component(permission = "com.app.P"), "com.app").tags shouldNotContain "hidden-entry"
        ComponentReviews.review(component(launcher = true), "com.app").tags shouldNotContain "hidden-entry"
    }

    @Test
    fun `task hijack matches the task-affinity finding rule`() {
        val foreign = component().copy(taskAffinity = "other.task", launchMode = "singleTask")
        ComponentReviews.taskHijackReason(foreign, "com.app")!! shouldContain "launchMode=singleTask"
        ComponentReviews.taskHijackReason(component().copy(taskAffinity = ""), "com.app") shouldBe null
        ComponentReviews.taskHijackReason(component().copy(taskAffinity = "com.app"), "com.app") shouldBe null
        ComponentReviews.taskHijackReason(component().copy(allowTaskReparenting = true), "com.app")!! shouldContain "allowTaskReparenting"
    }

    @Test
    fun `system bind permissions name the role, sensitive ones apart`() {
        val a11y = component("service", "com.app.A11y", permission = "android.permission.BIND_ACCESSIBILITY_SERVICE")
        ComponentReviews.review(a11y, "com.app").tags shouldContain "sensitive-role"
        val job = component("service", "com.app.Job", permission = "android.permission.BIND_JOB_SERVICE")
        ComponentReviews.review(job, "com.app").tags shouldContainExactly listOf("system-role", "guarded")
    }

    @Test
    fun `receivers, providers and services get their own verdicts`() {
        val boot = component("receiver", "com.app.Boot", filters = listOf(filter(listOf("android.intent.action.BOOT_COMPLETED"))))
        ComponentReviews.review(boot, "com.app").tags shouldContainExactly listOf("unguarded", "boot")
        val files =
            component("provider", "androidx.core.content.FileProvider", exported = false)
                .copy(grantUriPermissions = true, authorities = listOf("com.app.files"))
        ComponentReviews.review(files, "com.app").tags shouldContainExactly listOf("uri-grants", "file-provider")
        val fg = component("service", "com.app.Sync", exported = false).copy(foregroundServiceType = "dataSync")
        ComponentReviews.review(fg, "com.app").tags shouldContainExactly listOf("foreground")
    }

    @Test
    fun `aliases are listed on their target and adb commands are ready to run`() {
        val main = component(launcher = true)
        val alias = component("activity-alias", "com.app.Alias").copy(targetActivity = "com.app.Main")
        val reviews = ComponentReviews.of(listOf(main, alias), "com.app")
        reviews[0].aliases shouldBe listOf("com.app.Alias")
        reviews[0].adb.first().command shouldBe "adb shell am start -n com.app/com.app.Main"
    }

    @Test
    fun `manifest entry is the declared element with its children`() {
        val manifest =
            XmlNode(
                "manifest",
                mapOf("package" to "com.app"),
                listOf(
                    XmlNode(
                        "application",
                        emptyMap(),
                        listOf(XmlNode("activity", mapOf("android:name" to ".Main"), listOf(XmlNode("intent-filter", emptyMap(), emptyList())))),
                    ),
                ),
            )
        val node = ManifestXml.component(manifest, "com.app.Main")!!
        ManifestXml.render(node) shouldBe "<activity android:name=\".Main\">\n    <intent-filter />\n</activity>\n"
    }
}
