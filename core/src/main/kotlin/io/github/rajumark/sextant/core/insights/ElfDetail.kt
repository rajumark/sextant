package io.github.rajumark.sextant.core.insights

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One section header of an ELF file. */
class ElfSection(
    val name: String,
    val type: String,
    val offset: Long,
    val size: Long,
    /** Flags as letters: w (write), a (alloc), x (exec). */
    val flags: String,
)

/** One dynamic symbol: defined (exported) or undefined (imported from another library). */
class ElfSymbol(
    val name: String,
    /** func | object | other */
    val type: String,
    /** global | weak | local */
    val binding: String,
    val defined: Boolean,
)

/** One printable string in a data section, with its file offset. */
class ElfString(
    val offset: Long,
    val value: String,
)

/** Everything Sextant reads from one ELF file. */
class ElfInfo(
    val elfClass: String,
    /** arm64 | arm | x86 | x86_64 | riscv64 | … or `machine N` */
    val machine: String,
    val soname: String?,
    val buildId: String?,
    /** DT_NEEDED: the libraries this one loads, in order. */
    val needed: List<String>,
    val runpath: String?,
    val sections: List<ElfSection>,
    val symbols: List<ElfSymbol>,
    val strings: List<ElfString>,
    /** True when [strings] stopped at the cap. */
    val stringsCapped: Boolean,
)

/**
 * A fuller ELF reader than [Elf]: header, section headers with names, the dynamic section (needed libraries, soname,
 * runpath), the GNU build id, every dynamic symbol (exported and imported) and the printable strings of the read-only
 * data. Never throws on garbage input; returns null when [bytes] is not ELF.
 */
object ElfDetail {
    /** Strings shorter than this are mostly noise in machine code. */
    private const val MIN_STRING = 6

    /** Per library, so one huge engine library cannot dominate the index. */
    const val MAX_STRINGS = 20_000

    private val MACHINES = mapOf(3 to "x86", 40 to "arm", 62 to "x86_64", 183 to "arm64", 243 to "riscv64", 8 to "mips")
    private val SECTION_TYPES =
        mapOf(
            0 to "null",
            1 to "progbits",
            2 to "symtab",
            3 to "strtab",
            4 to "rela",
            5 to "hash",
            6 to "dynamic",
            7 to "note",
            8 to "nobits",
            9 to "rel",
            11 to "dynsym",
            14 to "init_array",
            15 to "fini_array",
        )

    fun read(bytes: ByteArray): ElfInfo? =
        try {
            parse(bytes)
        } catch (e: RuntimeException) {
            null
        }

    private class Raw(
        val nameOff: Long,
        val type: Int,
        val flags: Long,
        val offset: Long,
        val size: Long,
        val link: Int,
        val entSize: Long,
    )

    private fun parse(bytes: ByteArray): ElfInfo? {
        if (bytes.size < 52 ||
            bytes[0] != 0x7f.toByte() ||
            bytes[1] != 'E'.code.toByte() ||
            bytes[2] != 'L'.code.toByte() ||
            bytes[3] != 'F'.code.toByte()
        ) {
            return null
        }
        val is64 = bytes[4].toInt() == 2
        val b = ByteBuffer.wrap(bytes).order(if (bytes[5].toInt() == 2) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
        val machineId = b.getShort(0x12).toInt() and 0xffff
        val machine = MACHINES[machineId] ?: "machine $machineId"
        val shoff = if (is64) b.getLong(0x28) else u32(b, 0x20)
        val shentsize = b.getShort(if (is64) 0x3A else 0x2E).toInt() and 0xffff
        val shnum = b.getShort(if (is64) 0x3C else 0x30).toInt() and 0xffff
        val shstrndx = b.getShort(if (is64) 0x3E else 0x32).toInt() and 0xffff
        val elfClass = if (is64) "ELF64" else "ELF32"
        if (shoff <= 0 || shnum == 0 || shoff + shnum.toLong() * shentsize > bytes.size) {
            return ElfInfo(elfClass, machine, null, null, emptyList(), null, emptyList(), emptyList(), emptyList(), false)
        }
        val raw =
            (0 until shnum).map { i ->
                val o = (shoff + i.toLong() * shentsize).toInt()
                if (is64) {
                    Raw(
                        u32(b, o),
                        b.getInt(o + 4),
                        b.getLong(o + 8),
                        b.getLong(o + 0x18),
                        b.getLong(o + 0x20),
                        b.getInt(o + 0x28),
                        b.getLong(
                            o + 0x38,
                        ),
                    )
                } else {
                    Raw(u32(b, o), b.getInt(o + 4), u32(b, o + 8), u32(b, o + 0x10), u32(b, o + 0x14), b.getInt(o + 0x18), u32(b, o + 0x24))
                }
            }

        fun inFile(s: Raw) = s.type != 8 && s.offset >= 0 && s.size >= 0 && s.offset + s.size <= bytes.size
        val names = raw.getOrNull(shstrndx)?.takeIf(::inFile)
        val sections =
            raw.map { s ->
                ElfSection(
                    name = names?.let { cString(bytes, it.offset + s.nameOff) }.orEmpty(),
                    type = SECTION_TYPES[s.type] ?: "0x%x".format(s.type),
                    offset = s.offset,
                    size = s.size,
                    flags =
                        buildString {
                            if (s.flags and 1L != 0L) append('w')
                            if (s.flags and 2L != 0L) append('a')
                            if (s.flags and 4L != 0L) append('x')
                        },
                )
            }

        // The dynamic section: DT_NEEDED (1), DT_SONAME (14), DT_RUNPATH (29) / DT_RPATH (15), as offsets into its string table.
        val needed = mutableListOf<String>()
        var soname: String? = null
        var runpath: String? = null
        raw.withIndex().firstOrNull { it.value.type == 6 && inFile(it.value) }?.let { (_, dyn) ->
            val strtab = raw.getOrNull(dyn.link)?.takeIf(::inFile) ?: return@let
            val ent = if (is64) 16 else 8
            var o = dyn.offset
            while (o + ent <= dyn.offset + dyn.size) {
                val tag = if (is64) b.getLong(o.toInt()) else u32(b, o.toInt())
                val value = if (is64) b.getLong(o.toInt() + 8) else u32(b, o.toInt() + 4)
                if (tag == 0L) break
                val str = { cString(bytes, strtab.offset + value) }
                when (tag) {
                    1L -> str()?.let { needed += it }
                    14L -> soname = str()
                    15L, 29L -> runpath = str()
                }
                o += ent
            }
        }

        // GNU build id: a note named "GNU" of type 3, usually in .note.gnu.build-id.
        var buildId: String? = null
        for (s in raw) {
            if (s.type != 7 || !inFile(s) || buildId != null) continue
            var o = s.offset
            while (o + 12 <= s.offset + s.size) {
                val nameSize = u32(b, o.toInt())
                val descSize = u32(b, o.toInt() + 4)
                val type = u32(b, o.toInt() + 8)
                val nameStart = o + 12
                val descStart = nameStart + ((nameSize + 3) and 3L.inv())
                if (descStart + descSize > s.offset + s.size) break
                if (type == 3L && nameSize == 4L && cString(bytes, nameStart) == "GNU") {
                    buildId = bytes.copyOfRange(descStart.toInt(), (descStart + descSize).toInt()).joinToString("") { "%02x".format(it) }
                    break
                }
                o = descStart + ((descSize + 3) and 3L.inv())
            }
        }

        val symbols = mutableListOf<ElfSymbol>()
        for (sym in raw) {
            if (sym.type != 11 || !inFile(sym) || sym.link !in raw.indices) continue
            val str = raw[sym.link].takeIf(::inFile) ?: continue
            val entSize =
                if (sym.entSize > 0) {
                    sym.entSize
                } else if (is64) {
                    24L
                } else {
                    16L
                }
            for (k in 1 until (sym.size / entSize).toInt()) {
                val o = (sym.offset + k * entSize).toInt()
                val nameOff = u32(b, o)
                val info = (if (is64) b.get(o + 4) else b.get(o + 12)).toInt() and 0xff
                val shndx = (if (is64) b.getShort(o + 6) else b.getShort(o + 14)).toInt() and 0xffff
                val name = cString(bytes, str.offset + nameOff)?.takeIf { it.isNotEmpty() } ?: continue
                val type =
                    when (info and 0xf) {
                        1 -> "object"
                        2 -> "func"
                        else -> "other"
                    }
                val binding =
                    when (info shr 4) {
                        0 -> "local"
                        1 -> "global"
                        2 -> "weak"
                        else -> "other"
                    }
                symbols += ElfSymbol(name, type, binding, defined = shndx != 0)
            }
        }

        // Printable strings of the read-only data (.rodata and similar alloc, non-exec, non-write progbits).
        val strings = mutableListOf<ElfString>()
        var capped = false
        for ((i, s) in raw.withIndex()) {
            val name = sections[i].name
            if (s.type != 1 || !inFile(s) || s.flags and 4L != 0L || s.flags and 1L != 0L || s.flags and 2L == 0L) continue
            if (!(name.startsWith(".rodata") || name.isEmpty())) continue
            var start = -1L
            var o = s.offset
            val end = s.offset + s.size
            while (o <= end) {
                val c = if (o < end) bytes[o.toInt()].toInt() and 0xff else 0
                val printable = c in 0x20..0x7e || c == 0x09
                if (printable) {
                    if (start < 0) start = o
                } else if (start >= 0) {
                    if (o - start >= MIN_STRING) {
                        if (strings.size >= MAX_STRINGS) {
                            capped = true
                            break
                        }
                        strings += ElfString(start, String(bytes, start.toInt(), (o - start).toInt(), Charsets.ISO_8859_1))
                    }
                    start = -1
                }
                o++
            }
            if (capped) break
        }
        return ElfInfo(
            elfClass,
            machine,
            soname,
            buildId,
            needed,
            runpath,
            sections,
            symbols.distinctBy { it.name to it.defined },
            strings,
            capped,
        )
    }

    private fun u32(
        b: ByteBuffer,
        o: Int,
    ) = b.getInt(o).toLong() and 0xffffffffL

    private fun cString(
        bytes: ByteArray,
        start: Long,
    ): String? {
        if (start !in bytes.indices.first.toLong()..bytes.indices.last.toLong()) return null
        var end = start.toInt()
        while (end < bytes.size && bytes[end] != 0.toByte()) end++
        return String(bytes, start.toInt(), end - start.toInt(), Charsets.UTF_8)
    }
}
