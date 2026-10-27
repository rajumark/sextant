package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.IntentFilter
import io.github.rajumark.sextant.core.output.InfoReports
import kotlinx.serialization.Serializable

/**
 * Verdicts for one component, with the reason for each, its entry points in plain words, and adb commands to reach it.
 * The CLI (`sextant components`, `sextant component`) and the desktop app both read these, so they always agree.
 */
@Serializable
data class ComponentReview(
    val type: String,
    val name: String,
    /** Stable verdict ids, in [ComponentReview.TAGS] order. */
    val tags: List<String>,
    /** Why each tag applies: tag → one sentence. */
    val reasons: Map<String, String>,
    /** What can start the component, one plain sentence per intent filter. */
    val entryPoints: List<String>,
    val deepLinks: List<String>,
    /** Hosts of http(s) links with autoVerify=true; each needs /.well-known/assetlinks.json to verify. */
    val appLinkHosts: List<String>,
    /** Activity aliases whose targetActivity is this activity. */
    val aliases: List<String>,
    val adb: List<AdbCommand>,
) {
    companion object {
        /** Every tag, in display order. */
        val TAGS =
            listOf(
                "launcher",
                "app-link",
                "deep-link",
                "hidden-entry",
                "task-hijack",
                "lock-screen",
                "exported-by-default",
                "guarded",
                "disabled",
            )
    }
}

@Serializable
data class AdbCommand(
    val label: String,
    val command: String,
)

object ComponentReviews {
    private const val BROWSABLE = "android.intent.category.BROWSABLE"
    private const val VIEW = "android.intent.action.VIEW"
    private val ACTIVITIES = setOf("activity", "activity-alias")

    fun of(
        components: List<Component>,
        packageName: String?,
    ): List<ComponentReview> {
        val aliasesByTarget = components.filter { it.type == "activity-alias" && it.targetActivity != null }.groupBy { it.targetActivity }
        return components.map { review(it, packageName, aliasesByTarget[it.name].orEmpty().map { a -> a.name }) }
    }

    fun review(
        c: Component,
        packageName: String?,
        aliases: List<String> = emptyList(),
    ): ComponentReview {
        val reasons = linkedMapOf<String, String>()
        val links = InfoReports.deepLinks(c)
        val appHosts = appLinkHosts(c)
        val activity = c.type in ACTIVITIES
        val guarded = c.permission != null || c.readPermission != null || c.writePermission != null

        if (activity && c.launcher) reasons["launcher"] = "Has a MAIN/LAUNCHER filter: it is an icon in the app drawer."
        if (appHosts.isNotEmpty()) {
            reasons["app-link"] =
                "Verified App Link for ${appHosts.joinToString()}: opens without a chooser if the site's assetlinks.json lists this app."
        }
        if (links.isNotEmpty()) {
            reasons["deep-link"] =
                if (c.intentFilters.any { BROWSABLE in it.categories }) {
                    "Opens from links in browsers and other apps (${links.size} pattern${if (links.size == 1) "" else "s"})."
                } else {
                    "Handles VIEW intents for ${links.size} URI pattern${if (links.size == 1) "" else "s"} (not BROWSABLE: other apps only)."
                }
        }
        if (activity && c.exported && c.enabled && !c.launcher && links.isEmpty() && !guarded) {
            reasons["hidden-entry"] =
                "Exported with no launcher icon, no links and no permission: only other apps start it, so check the intents it accepts."
        }
        taskHijackReason(c, packageName)?.let { reasons["task-hijack"] = it }
        if (activity && c.showWhenLocked == true) reasons["lock-screen"] = "showWhenLocked=true: shown above the lock screen."
        if (c.exported && c.exportedSource == "default") {
            reasons["exported-by-default"] = "No android:exported; exported because it has intent filters (pre-Android 12 default)."
        }
        if (guarded) {
            reasons["guarded"] =
                "Callers need " + listOfNotNull(c.permission, c.readPermission, c.writePermission).distinct().joinToString(" / ") + "."
        }
        if (!c.enabled) reasons["disabled"] = "android:enabled=false: off until the app enables it at runtime."

        val tags = ComponentReview.TAGS.filter { it in reasons }
        return ComponentReview(
            type = c.type,
            name = c.name,
            tags = tags,
            reasons = tags.associateWith { reasons.getValue(it) },
            entryPoints = c.intentFilters.map(::describe),
            deepLinks = links,
            appLinkHosts = appHosts,
            aliases = aliases.sorted(),
            adb = adb(c, packageName),
        )
    }

    /**
     * Why an activity can move between tasks (StrandHogg-style task hijacking), or null. Same rule as the `task-affinity`
     * finding: a taskAffinity other than the package, or allowTaskReparenting=true.
     */
    fun taskHijackReason(
        c: Component,
        packageName: String?,
    ): String? {
        if (c.type != "activity") return null
        val foreign = c.taskAffinity != null && c.taskAffinity.isNotEmpty() && c.taskAffinity != packageName
        if (!foreign && c.allowTaskReparenting != true) return null
        val parts = mutableListOf<String>()
        if (foreign) parts += "taskAffinity=${c.taskAffinity} differs from the package"
        if (c.allowTaskReparenting == true) parts += "allowTaskReparenting=true"
        if (c.launchMode == "singleTask" || c.launchMode == "singleInstance") parts += "launchMode=${c.launchMode}"
        return parts.joinToString(", ").replaceFirstChar { it.uppercase() } + ": it can move between tasks; review for task hijacking."
    }

    private fun appLinkHosts(c: Component): List<String> =
        c.intentFilters
            .filter { it.autoVerify && VIEW in it.actions && BROWSABLE in it.categories }
            .filter { f -> f.data.any { it.scheme == "http" || it.scheme == "https" } }
            .flatMap { f -> f.data.mapNotNull { it.host } }
            .distinct()
            .sorted()

    /** One intent filter in plain words, e.g. "Opens https://example.org/join* from browsers (verified)". */
    fun describe(f: IntentFilter): String {
        val main = "android.intent.action.MAIN" in f.actions
        if (main && "android.intent.category.LAUNCHER" in f.categories) return "App launcher icon"
        if (main && "android.intent.category.LEANBACK_LAUNCHER" in f.categories) return "TV launcher icon"
        val probe = InfoReports.deepLinks(f).sorted()
        val mimes = f.data.mapNotNull { it.mimeType }.distinct()
        val actions = f.actions.map { it.removePrefix("android.intent.action.") }
        if (VIEW in f.actions && probe.isNotEmpty()) {
            val where = if (BROWSABLE in f.categories) "from browsers and apps" else "from other apps"
            val shown = probe.take(3).joinToString() + if (probe.size > 3) " (+${probe.size - 3})" else ""
            return "Opens $shown $where" + if (f.autoVerify) " (verified App Link)" else ""
        }
        val what = if (actions.isEmpty()) "no action" else actions.joinToString(" / ")
        val types = if (mimes.isEmpty()) "" else " for " + mimes.joinToString()
        val verb =
            when {
                actions.any { it == "SEND" || it == "SEND_MULTIPLE" } -> "Receives shared content"
                else -> "Handles"
            }
        return "$verb ($what)$types" + if (f.priority != 0) " · priority ${f.priority}" else ""
    }

    /** `<activity>` attributes that are set, as `android:` names in manifest order of importance; empty for other types. */
    fun activityAttributes(c: Component): List<Pair<String, String>> {
        if (c.type !in ACTIVITIES) return emptyList()
        return listOf(
            "launchMode" to c.launchMode,
            "taskAffinity" to c.taskAffinity,
            "allowTaskReparenting" to c.allowTaskReparenting,
            "documentLaunchMode" to c.documentLaunchMode,
            "lockTaskMode" to c.lockTaskMode,
            "excludeFromRecents" to c.excludeFromRecents,
            "autoRemoveFromRecents" to c.autoRemoveFromRecents,
            "maxRecents" to c.maxRecents,
            "noHistory" to c.noHistory,
            "clearTaskOnLaunch" to c.clearTaskOnLaunch,
            "finishOnTaskLaunch" to c.finishOnTaskLaunch,
            "alwaysRetainTaskState" to c.alwaysRetainTaskState,
            "showWhenLocked" to c.showWhenLocked,
            "turnScreenOn" to c.turnScreenOn,
            "allowEmbedded" to c.allowEmbedded,
            "parentActivityName" to c.parentActivityName,
            "theme" to c.theme,
            "screenOrientation" to c.screenOrientation,
            "configChanges" to c.configChanges,
            "windowSoftInputMode" to c.windowSoftInputMode,
            "resizeableActivity" to c.resizeableActivity,
            "supportsPictureInPicture" to c.supportsPictureInPicture,
            "hardwareAccelerated" to c.hardwareAccelerated,
        ).mapNotNull { (k, v) -> v?.let { k to it.toString() } }
    }

    /** adb commands to reach the component from a connected device. */
    fun adb(
        c: Component,
        packageName: String?,
    ): List<AdbCommand> {
        val pkg = packageName ?: c.name.substringBeforeLast('.')
        val target = "$pkg/${c.name}"
        val firstAction = c.intentFilters.flatMap { it.actions }.firstOrNull()
        val out = mutableListOf<AdbCommand>()
        when (c.type) {
            "activity", "activity-alias" -> {
                out +=
                    AdbCommand(
                        if (c.exported) "Start" else "Start (not exported: needs a debuggable build or root)",
                        "adb shell am start -n $target",
                    )
                InfoReports.deepLinks(c).filter { '*' !in it && "://" in it }.take(3).forEach { uri ->
                    out += AdbCommand("Open $uri", "adb shell am start -a $VIEW -d \"$uri\" $pkg")
                }
                if (firstAction != null && firstAction != "android.intent.action.MAIN" && firstAction != VIEW) {
                    out += AdbCommand("Start with $firstAction", "adb shell am start -a $firstAction -n $target")
                }
            }
            "service" -> {
                val verb = if (c.foregroundServiceType != null) "start-foreground-service" else "startservice"
                out += AdbCommand("Start service", "adb shell am $verb -n $target")
            }
            "receiver" ->
                out += AdbCommand("Send broadcast", "adb shell am broadcast" + (firstAction?.let { " -a $it" } ?: "") + " -n $target")
            "provider" -> c.authorities.forEach { a -> out += AdbCommand("Query $a", "adb shell content query --uri content://$a/") }
        }
        return out
    }
}
