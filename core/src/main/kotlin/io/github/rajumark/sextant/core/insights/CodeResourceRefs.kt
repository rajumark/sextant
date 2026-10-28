package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.CodeExtractor
import io.github.rajumark.sextant.core.CodeSink
import io.github.rajumark.sextant.core.RawResource
import io.github.rajumark.sextant.core.model.ResourceRef
import io.github.rajumark.sextant.core.model.ResourceUseRecord

/** Collects the resource ids the code loads, for [CodeResourceRefs]. */
class ResourceUseCollector : CodeSink {
    val uses = ArrayList<ResourceUseRecord>()

    override fun resourceUse(record: ResourceUseRecord) {
        uses += record
    }
}

/**
 * Code → resource references: every resource id the code loads (`const 0x7f…` in R8 builds, `R$type.name` reads
 * otherwise), resolved against resources.arsc, as [ResourceRef]s with `from_kind = code`. `from` is the method;
 * `attribute` is the first call within 8 instructions after the id is loaded, usually the one it is passed to
 * (`setContentView`, `getString`, …), or empty when none follows.
 * Ids that are not in the resource table (other int constants that happen to start with 0x7f) are dropped.
 */
object CodeResourceRefs {
    fun of(
        uses: List<ResourceUseRecord>,
        table: List<RawResource>,
    ): List<ResourceRef> {
        val byId = HashMap<Int, String>()
        // R field names use `_` where resource names may use `.` (styles), so match on the field-name form.
        val byField = HashMap<String, String>()
        for (r in table) {
            val ref = "@${r.type}/${r.name}"
            byId.putIfAbsent(r.id, ref)
            byField.putIfAbsent("${r.type}/${r.name.replace('.', '_')}", ref)
        }
        return uses
            .mapNotNull { u ->
                val ref =
                    if (u.key.startsWith("0x")) {
                        byId[
                            u.key
                                .substring(2)
                                .toLong(16)
                                .toInt(),
                        ]
                    } else {
                        CodeExtractor.R_FIELD.matchEntire(u.key)?.let { m -> byField["${m.groupValues[1]}/${m.groupValues[2]}"] }
                    }
                ref?.let { ResourceRef(it, "code", u.method, u.via.orEmpty()) }
            }.distinct()
            .sortedWith(compareBy({ it.ref }, { it.from }, { it.attribute }))
    }
}
