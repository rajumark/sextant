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
                "unguarded",
                "sensitive-role",
                "system-role",
                "boot",
                "foreground",
                "task-hijack",
                "lock-screen",
                "uri-grants",
                "file-provider",
                "isolated",
                "direct-boot",
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
        val open = c.exported && c.enabled && !guarded
        if (activity && open && !c.launcher && links.isEmpty()) {
            reasons["hidden-entry"] =
                "Exported with no launcher icon, no links and no permission: only other apps start it, so check the intents it accepts."
        }
        if (!activity && open) {
            reasons["unguarded"] =
                when (c.type) {
                    "provider" ->
                        "Exported with no read or write permission: any app can query it" +
                            (if (c.pathPermissions.isEmpty()) "." else " except paths with a <path-permission>.")
                    "service" -> "Exported with no permission: any app can start or bind it."
                    else -> "Exported with no permission: any app can send it broadcasts."
                }
        }
        role(c)?.let { (role, sensitive) -> reasons[if (sensitive) "sensitive-role" else "system-role"] = role }
        val bootActions =
            c.intentFilters
                .flatMap { it.actions }
                .filter { it in BOOT_ACTIONS }
                .distinct()
        if (c.type == "receiver" && bootActions.isNotEmpty()) {
            reasons["boot"] =
                "Runs after boot (${bootActions.joinToString { it.substringAfterLast('.') }}): the app starts without being opened."
        }
        c.foregroundServiceType?.let { reasons["foreground"] = "Foreground service ($it): runs with a visible notification." }
        taskHijackReason(c, packageName)?.let { reasons["task-hijack"] = it }
        if (activity && (c.showWhenLocked == true || c.showOnLockScreen == true)) {
            reasons["lock-screen"] =
                (if (c.showWhenLocked == true) "showWhenLocked" else "showOnLockScreen") + "=true: shown above the lock screen."
        }
        if (c.type == "provider" && c.grantUriPermissions == true) {
            reasons["uri-grants"] =
                if (c.grantUriPaths.isEmpty()) {
                    "grantUriPermissions=true for any path: the app can hand out temporary access to all its URIs."
                } else {
                    "Grants temporary URI access for ${c.grantUriPaths.joinToString()}."
                }
        }
        if (c.type == "provider" && c.name.endsWith("FileProvider")) {
            reasons["file-provider"] = "Shares files as content:// URIs; the shared folders are listed in its FILE_PROVIDER_PATHS XML."
        }
        if (c.isolatedProcess == true) reasons["isolated"] = "isolatedProcess=true: runs in a sandboxed process with no app permissions."
        if (c.directBootAware == true) {
            reasons["direct-boot"] = "directBootAware=true: runs before the user unlocks the device (device-protected storage only)."
        }
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

    private val BOOT_ACTIONS =
        setOf(
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )

    /** Bind permissions only the system holds → (role, sensitive). Sensitive roles see user data or act for the user. */
    private val BIND_ROLES =
        mapOf(
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to ("Accessibility service: can read the screen and act for the user." to true),
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to ("Notification listener: reads every notification." to true),
            "android.permission.BIND_DEVICE_ADMIN" to ("Device admin: can lock, wipe or set password rules." to true),
            "android.permission.BIND_VPN_SERVICE" to ("VPN service: sees the device's network traffic." to true),
            "android.permission.BIND_INPUT_METHOD" to ("Keyboard (input method): sees what the user types." to true),
            "android.permission.BIND_AUTOFILL_SERVICE" to ("Autofill service: sees form fields and fills credentials." to true),
            "android.permission.BIND_CREDENTIAL_PROVIDER_SERVICE" to ("Credential provider: supplies passwords and passkeys." to true),
            "android.permission.BIND_INCALL_SERVICE" to ("In-call service: sees and controls phone calls." to true),
            "android.permission.BIND_SCREENING_SERVICE" to ("Call screening: sees incoming calls and can block them." to true),
            "android.permission.BIND_VOICE_INTERACTION" to ("Voice assistant: can read the screen when invoked." to true),
            "android.permission.BIND_DEVICE_POLICY_SERVICE" to ("Device policy service." to true),
            "android.permission.BIND_JOB_SERVICE" to ("JobScheduler job: the system runs it in the background." to false),
            "android.permission.BIND_QUICK_SETTINGS_TILE" to ("Quick Settings tile." to false),
            "android.permission.BIND_WALLPAPER" to ("Live wallpaper." to false),
            "android.permission.BIND_DREAM_SERVICE" to ("Screen saver (dream)." to false),
            "android.permission.BIND_REMOTEVIEWS" to ("Home-screen widget list adapter." to false),
            "android.permission.BIND_CHOOSER_TARGET_SERVICE" to ("Direct share targets." to false),
            "android.permission.BIND_TEXT_SERVICE" to ("Spell checker." to false),
            "android.permission.BIND_PRINT_SERVICE" to ("Print service." to false),
            "android.permission.BIND_NFC_SERVICE" to ("NFC host card emulation." to false),
            "android.permission.BIND_TELECOM_CONNECTION_SERVICE" to
                ("Calling (ConnectionService): places calls through the system dialer." to false),
            "android.permission.BIND_CONTROLS" to ("Device controls (smart-home tiles)." to false),
            "android.permission.BIND_COMPANION_DEVICE_SERVICE" to ("Companion device service." to false),
            "android.permission.BIND_CONDITION_PROVIDER_SERVICE" to ("Do Not Disturb condition provider." to false),
            "android.permission.BIND_CARRIER_SERVICES" to ("Carrier services." to false),
            "android.permission.BIND_APPWIDGET" to ("App widget host." to false),
        )

    /** Intent actions that reveal a role when no bind permission does → (role, sensitive). */
    private val ACTION_ROLES =
        mapOf(
            "android.provider.Telephony.SMS_RECEIVED" to ("Receives incoming SMS." to true),
            "android.provider.Telephony.SMS_DELIVER" to ("Default SMS app: receives every SMS." to true),
            "android.provider.Telephony.WAP_PUSH_DELIVER" to ("Default SMS app: receives MMS." to true),
            "android.intent.action.NEW_OUTGOING_CALL" to ("Sees outgoing call numbers." to true),
            "android.intent.action.PHONE_STATE" to ("Watches phone call state." to true),
            "android.accounts.AccountAuthenticator" to ("Account authenticator: adds an account type in Settings > Accounts." to true),
            "android.content.SyncAdapter" to ("Sync adapter: the system runs background sync." to false),
            "com.google.firebase.MESSAGING_EVENT" to ("Firebase Cloud Messaging: receives push messages." to false),
            "android.appwidget.action.APPWIDGET_UPDATE" to ("Home-screen widget." to false),
            "android.media.browse.MediaBrowserService" to ("Media browser: plays media for Android Auto and other controllers." to false),
            "android.support.customtabs.action.CustomTabsService" to ("Custom Tabs provider: other apps open web pages in it." to false),
            "androidx.media3.session.MediaSessionService" to ("Media session service: plays media for other controllers." to false),
        )

    /** What the system uses this component for, from its bind permission or intent actions; null if nothing special. */
    fun role(c: Component): Pair<String, Boolean>? {
        listOfNotNull(c.permission).firstNotNullOfOrNull { BIND_ROLES[it] }?.let { return it }
        return c.intentFilters.flatMap { it.actions }.firstNotNullOfOrNull { ACTION_ROLES[it] }
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
        if (foreign) parts += "taskAffinity=${c.taskAffinity} (not the package)"
        if (c.allowTaskReparenting == true) parts += "allowTaskReparenting=true"
        if (c.launchMode == "singleTask" || c.launchMode == "singleInstance") parts += "launchMode=${c.launchMode}"
        return parts.joinToString(", ") + ": it can move between tasks; review for task hijacking."
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

    /** Type-specific attributes that are set, as `android:` names, most important first. */
    fun attributes(c: Component): List<Pair<String, String>> {
        val pairs =
            when (c.type) {
                "activity", "activity-alias" ->
                    listOf(
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
                        "stateNotNeeded" to c.stateNotNeeded,
                        "maxAspectRatio" to c.maxAspectRatio,
                        "persistableMode" to c.persistableMode,
                        "relinquishTaskIdentity" to c.relinquishTaskIdentity,
                        "showOnLockScreen" to c.showOnLockScreen,
                        "enableOnBackInvokedCallback" to c.enableOnBackInvokedCallback,
                        "layout" to
                            c.windowLayout.entries
                                .joinToString(" ") { "${it.key}=${it.value}" }
                                .ifEmpty { null },
                        "banner" to c.banner,
                    )
                "service" ->
                    listOf(
                        "foregroundServiceType" to c.foregroundServiceType,
                        "isolatedProcess" to c.isolatedProcess,
                        "externalService" to c.externalService,
                        "stopWithTask" to c.stopWithTask,
                        "useAppZygote" to c.useAppZygote,
                        "visibleToInstantApps" to c.visibleToInstantApps,
                        "description" to c.description,
                    )
                "provider" ->
                    listOf(
                        "grantUriPermissions" to c.grantUriPermissions,
                        "forceUriPermissions" to c.forceUriPermissions,
                        "multiprocess" to c.multiprocess,
                        "syncable" to c.syncable,
                        "initOrder" to c.initOrder,
                    )
                else -> emptyList()
            } + listOf("directBootAware" to c.directBootAware, "icon" to c.icon, "roundIcon" to c.roundIcon)
        return pairs.mapNotNull { (k, v) -> v?.let { k to it.toString() } }
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
