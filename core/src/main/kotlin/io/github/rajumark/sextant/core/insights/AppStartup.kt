package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.index.CallNode
import io.github.rajumark.sextant.core.index.Direction
import io.github.rajumark.sextant.core.index.IndexReader
import kotlinx.serialization.Serializable

/**
 * What runs when the app process starts, in order: the zygote preload, the AppComponentFactory, the Application class's
 * attachBaseContext, every content provider of the main process (androidx.startup initializers among them), then
 * Application.onCreate. With a code index it also follows those methods' calls and names the SDKs they start.
 */
@Serializable
data class AppStartup(
    val applicationClass: String?,
    /** Application class first, then its superclasses up to the first framework class. Empty without a code index. */
    val superclasses: List<String>,
    val appComponentFactory: String?,
    val zygotePreload: String?,
    val backupAgent: String?,
    val manageSpaceActivity: String?,
    /** android:process of <application>, when the main process is renamed. */
    val process: String?,
    val steps: List<StartupStep>,
    val providers: List<StartupProvider>,
    val methods: List<StartupMethod>,
    val sdks: List<StartupSdk>,
    /** False when no index.sqlite was available: superclasses, methods and sdks are then empty. */
    val codeIndexed: Boolean,
)

@Serializable
data class StartupStep(
    val order: Int,
    val what: String,
    val target: String,
)

@Serializable
data class StartupProvider(
    val name: String,
    val authorities: List<String>,
    val initOrder: Int?,
    val exported: Boolean,
    /** androidx.startup Initializer classes listed in its meta-data. */
    val initializers: List<String>,
    /** The SDK the provider belongs to (many SDKs start themselves this way), or null. */
    val sdk: String?,
)

@Serializable
data class StartupMethod(
    /** Smali descriptor, e.g. `Lcom/app/App;->onCreate()V`. */
    val method: String,
    /** Direct calls, in first-call order (deduplicated). */
    val calls: List<String>,
    /** Calls beyond the fan-out limit. */
    val more: Int,
)

@Serializable
data class StartupSdk(
    val sdk: String,
    val category: String,
    /** The first startup method that reaches it, and the call that does. */
    val from: String,
    val via: String,
)

object AppStartups {
    private const val DEPTH = 3
    private const val FAN_OUT = 60
    private val FRAMEWORK = listOf("android.", "androidx.", "java.", "javax.", "kotlin.", "dalvik.")

    fun build(
        extraction: Extraction,
        index: IndexReader?,
    ): AppStartup {
        val flags = extraction.meta.flags
        val app = extraction.meta.applicationClass
        val mainProcess = flags.process
        val providers =
            extraction.components
                .filter { it.type == "provider" && it.enabled && (it.process == null || it.process == mainProcess) }
                .sortedByDescending { it.initOrder ?: 0 }
                .map { p ->
                    StartupProvider(
                        name = p.name,
                        authorities = p.authorities,
                        initOrder = p.initOrder,
                        exported = p.exported,
                        initializers = extraction.metadata.filter { it.scope == p.name && it.value == "androidx.startup" }.map { it.name },
                        sdk = InsightsBuilder.sdkOf(p.name)?.first,
                    )
                }

        val steps = mutableListOf<Pair<String, String>>()
        flags.zygotePreloadName?.let { steps += "Zygote preload (app zygote)" to it }
        flags.appComponentFactory?.let { steps += "AppComponentFactory creates the Application and components" to it }
        steps += "Application.attachBaseContext" to (app ?: "android.app.Application (no custom class)")
        providers.forEach { p ->
            steps += "ContentProvider.onCreate" + (p.initOrder?.let { " (initOrder $it)" } ?: "") to p.name
            p.initializers.forEach { steps += "  androidx.startup Initializer.create" to it }
        }
        steps += "Application.onCreate" to (app ?: "android.app.Application (no custom class)")

        var superclasses = emptyList<String>()
        val methods = mutableListOf<StartupMethod>()
        val sdks = linkedMapOf<String, StartupSdk>()
        if (index != null) {
            superclasses = app?.let { chain(index, it) }.orEmpty()
            val startClasses = superclasses.filter { c -> FRAMEWORK.none { c.startsWith(it) } }
            val targets =
                startClasses.flatMap { c ->
                    listOf("${desc(c)}->attachBaseContext(Landroid/content/Context;)V", "${desc(c)}->onCreate()V")
                } +
                    providers.flatMap {
                        listOf("${desc(it.name)}->onCreate()Z") +
                            it.initializers.map { i -> "${desc(i)}->create(Landroid/content/Context;)Ljava/lang/Object;" }
                    }
            for (t in targets) {
                val id = index.resolveMethods(t).firstOrNull()?.first ?: continue
                val tree = index.callTree(id, Direction.CALLEES, DEPTH, FAN_OUT)
                methods += StartupMethod(t, tree.children.map { it.method }.distinct(), tree.more)
                collectSdks(tree, t, sdks)
            }
        }
        return AppStartup(
            applicationClass = app,
            superclasses = superclasses,
            appComponentFactory = flags.appComponentFactory,
            zygotePreload = flags.zygotePreloadName,
            backupAgent = flags.backupAgent,
            manageSpaceActivity = flags.manageSpaceActivity,
            process = mainProcess,
            steps = steps.mapIndexed { i, (what, target) -> StartupStep(i + 1, what, target) },
            providers = providers,
            methods = methods,
            sdks = sdks.values.sortedWith(compareBy({ it.category }, { it.sdk })),
            codeIndexed = index != null,
        )
    }

    private fun desc(className: String) = "L" + className.replace('.', '/') + ";"

    private fun className(descriptor: String) =
        descriptor
            .substringBefore("->")
            .removePrefix("L")
            .removeSuffix(";")
            .replace('/', '.')

    /** The class and its superclasses, following the index until a class it does not define (a framework class). */
    private fun chain(
        index: IndexReader,
        start: String,
    ): List<String> {
        val out = mutableListOf(start)
        var current = start
        repeat(12) {
            val r = index.query("SELECT superclass FROM classes WHERE name = '${current.replace("'", "''")}'", 1)
            val sup =
                r.rows
                    .firstOrNull()
                    ?.firstOrNull()
                    ?.toString() ?: return out
            val name = sup.removePrefix("L").removeSuffix(";").replace('/', '.')
            out += name
            current = name
        }
        return out
    }

    private fun collectSdks(
        node: CallNode,
        from: String,
        out: MutableMap<String, StartupSdk>,
    ) {
        for (child in node.children) {
            InsightsBuilder.sdkOf(className(child.method))?.let { (name, category) ->
                if (name !in out) out[name] = StartupSdk(name, category, from, child.method)
            }
            collectSdks(child, from, out)
        }
    }
}
