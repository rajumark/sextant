package io.github.rajumark.sextant.core.insights

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Minimal ELF reader: the exported, defined function symbols of a shared library (.dynsym). */
object Elf {
    class Result(
        val elfClass: String,
        val exportedFunctions: List<String>,
    )

    private const val SHT_DYNSYM = 11
    private const val STT_FUNC = 2
    private const val STB_GLOBAL = 1
    private const val STB_WEAK = 2
    private const val SHN_UNDEF = 0

    /** Returns null when [bytes] is not a well-formed ELF file. Never throws on garbage input. */
    fun read(bytes: ByteArray): Result? =
        try {
            parse(bytes)
        } catch (e: RuntimeException) {
            null
        }

    private fun parse(bytes: ByteArray): Result? {
        if (bytes.size < 52 ||
            bytes[0] != 0x7f.toByte() ||
            bytes[1] != 'E'.code.toByte() ||
            bytes[2] != 'L'.code.toByte() ||
            bytes[3] != 'F'.code.toByte()
        ) {
            return null
        }
        val is64 = bytes[4].toInt() == 2
        val order = if (bytes[5].toInt() == 2) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        val b = ByteBuffer.wrap(bytes).order(order)
        val shoff = if (is64) b.getLong(0x28) else b.getInt(0x20).toLong() and 0xffffffffL
        val shentsize = b.getShort(if (is64) 0x3A else 0x2E).toInt() and 0xffff
        val shnum = b.getShort(if (is64) 0x3C else 0x30).toInt() and 0xffff
        if (shoff <= 0 ||
            shnum == 0 ||
            shoff + shnum.toLong() * shentsize > bytes.size
        ) {
            return Result(if (is64) "ELF64" else "ELF32", emptyList())
        }

        fun section(i: Int): Section {
            val o = (shoff + i.toLong() * shentsize).toInt()
            return if (is64) {
                Section(b.getInt(o + 4), b.getLong(o + 0x18), b.getLong(o + 0x20), b.getInt(o + 0x28), b.getLong(o + 0x38))
            } else {
                Section(b.getInt(o + 4), u32(b, o + 0x10), u32(b, o + 0x14), b.getInt(o + 0x18), u32(b, o + 0x24))
            }
        }

        val names = mutableListOf<String>()
        for (i in 0 until shnum) {
            val sym = section(i)
            if (sym.type != SHT_DYNSYM || sym.link !in 0 until shnum) continue
            val str = section(sym.link)
            val entSize =
                if (sym.entSize > 0) {
                    sym.entSize
                } else if (is64) {
                    24L
                } else {
                    16L
                }
            val count = (sym.size / entSize).toInt()
            for (k in 1 until count) {
                val o = (sym.offset + k * entSize).toInt()
                val nameOff: Long
                val info: Int
                val shndx: Int
                if (is64) {
                    nameOff = u32(b, o)
                    info = b.get(o + 4).toInt() and 0xff
                    shndx = b.getShort(o + 6).toInt() and 0xffff
                } else {
                    nameOff = u32(b, o)
                    info = b.get(o + 12).toInt() and 0xff
                    shndx = b.getShort(o + 14).toInt() and 0xffff
                }
                val bind = info shr 4
                if ((info and 0xf) != STT_FUNC || (bind != STB_GLOBAL && bind != STB_WEAK) || shndx == SHN_UNDEF) continue
                cString(bytes, (str.offset + nameOff).toInt())?.let { if (it.isNotEmpty()) names += it }
            }
        }
        return Result(if (is64) "ELF64" else "ELF32", names.distinct().sorted())
    }

    private class Section(
        val type: Int,
        val offset: Long,
        val size: Long,
        val link: Int,
        val entSize: Long,
    )

    private fun u32(
        b: ByteBuffer,
        o: Int,
    ) = b.getInt(o).toLong() and 0xffffffffL

    private fun cString(
        bytes: ByteArray,
        start: Int,
    ): String? {
        if (start !in bytes.indices) return null
        var end = start
        while (end < bytes.size && bytes[end] != 0.toByte()) end++
        return String(bytes, start, end - start, Charsets.UTF_8)
    }

    /**
     * Decodes a JNI function name back to (class, method): `Java_com_example_Foo_bar` → (`com.example.Foo`, `bar`).
     * Escapes: `_1` = `_`, `_2` = `;`, `_3` = `[`, `_0xxxx` = Unicode. Overloaded names (`__` signature suffix) keep the method only.
     */
    fun jni(symbol: String): Pair<String, String>? {
        if (!symbol.startsWith("Java_")) return null
        val body = symbol.removePrefix("Java_").substringBefore("__")
        val parts = mutableListOf(StringBuilder())
        var i = 0
        while (i < body.length) {
            val c = body[i]
            if (c == '_' && i + 1 < body.length && body[i + 1] in "0123") {
                when (body[i + 1]) {
                    '1' -> parts.last().append('_')
                    '2' -> parts.last().append(';')
                    '3' -> parts.last().append('[')
                    '0' -> {
                        if (i + 6 > body.length) return null
                        parts.last().append(body.substring(i + 2, i + 6).toIntOrNull(16)?.toChar() ?: return null)
                        i += 4
                    }
                }
                i += 2
            } else if (c == '_') {
                parts += StringBuilder()
                i++
            } else {
                parts.last().append(c)
                i++
            }
        }
        if (parts.size < 2) return null
        return parts.dropLast(1).joinToString(".") to parts.last().toString()
    }
}
