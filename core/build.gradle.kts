plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.jadx.core)
    implementation(libs.jadx.dex.input)
    implementation(libs.apksig)
    implementation(libs.dexlib2)
    // Same version as dexlib2 (baksmali is its disassembler).
    implementation("com.android.tools.smali:smali-baksmali:${libs.versions.dexlib2.get()}")
    implementation(libs.sqlite.jdbc)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Regenerate fixtures/golden from the real-APK corpus; review the diff before committing.
tasks.register<Test>("updateGolden") {
    description = "Regenerates golden output files from the real-APK corpus."
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter.includeTestsMatching("*CorpusGoldenTest*")
    systemProperty("sextant.updateGolden", "true")
    outputs.upToDateWhen { false }
}

tasks.test {
    // Corpus tests read the big APKs; give them room.
    maxHeapSize = "2g"
}

// Bake the project version into the jar so `sextant --version` can report it.
tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("**/sextant.properties") {
        expand("version" to version)
    }
}
