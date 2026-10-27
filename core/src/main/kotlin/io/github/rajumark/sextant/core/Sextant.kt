package io.github.rajumark.sextant.core

import java.util.Properties

/** Tool identity, shared by the CLI and every output file. */
object Sextant {
    const val NAME = "sextant"

    /** Output format version. Bumped only when the shape of the output files changes. */
    const val SCHEMA_VERSION = 7

    val version: String by lazy {
        val props = Properties()
        Sextant::class.java.getResourceAsStream("/io/github/rajumark/sextant/sextant.properties")?.use(props::load)
        props.getProperty("version") ?: "unknown"
    }
}
